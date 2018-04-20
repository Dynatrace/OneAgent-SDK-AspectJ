package de.dynatrace.aspectj;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;

import com.dynatrace.oneagent.sdk.api.IncomingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.Tracer;
import com.dynatrace.oneagent.sdk.api.enums.SDKState;

import de.dynatrace.aspectj.util.SDKUtil;

@Aspect
public class AspectTagging {

	private static final String INCOMING = "incoming";
	private static final String OUTGOING = "outgoing";

	private static Logger logger = SDKUtil.getLogger(AspectTagging.class.getName());
	private static OneAgentSDK oneAgentSdk = SDKUtil.getInstance().oneAgentSdk;

	/**
	 * Checks if the Agent State of the OneAgent running in the JVM is active.
	 * 
	 * @return
	 */
	private static boolean isOneAgentActive() {
		SDKState agentState = oneAgentSdk.getCurrentState();
		return agentState != null && agentState.equals(SDKState.ACTIVE);
	}

	/**
	 * Around aspect Pointcut declared in aop.xml Method for sending the Dynatrace
	 * Tag.
	 * 
	 * @param joinPoint
	 * @param map
	 * @throws Throwable
	 */
	protected static void sendTag(ProceedingJoinPoint joinPoint) throws Throwable {
		handleTraceAndTagging(joinPoint, true);
	}

	/**
	 * Around aspect pointcut declared in AOP.XML. Method for receiving the
	 * Dynatrace tag.
	 * 
	 * @param joinPoint
	 * @param map
	 * @throws Throwable
	 */
	protected static void receiveTag(ProceedingJoinPoint joinPoint) throws Throwable {
		handleTraceAndTagging(joinPoint, false);
	}

	/**
	 * Method that is executed with an Around Advice of the declared method in
	 * Aspectj definition (conf/META-INF/aop.xml) that sends/receives the
	 * communication between Endpoints. A TransferObject will be searched for the
	 * transfer of the x-dynatrace tagging.
	 * 
	 * @param joinPoint
	 * @param outgoing
	 * @throws Throwable
	 */
	private static void handleTraceAndTagging(ProceedingJoinPoint joinPoint, boolean outgoing) throws Throwable {

		String tag = null;
		Tracer tracer = null;
		String sdkSide = null;
		Map transferObject = null;
		String parameterDeepAccess = null;
		int parameterPosition = 0;
		boolean isAgentActive = isOneAgentActive();

		if (outgoing) {
			sdkSide = OUTGOING;
			parameterPosition = SDKUtil.send_parameter_position;
			parameterDeepAccess = SDKUtil.send_parameter_deepaccess;

		} else {
			sdkSide = INCOMING;
			parameterPosition = SDKUtil.receive_parameter_position;
			parameterDeepAccess = SDKUtil.receive_parameter_deepaccess;
		}

		if (isAgentActive) {
			logger.info("SDK " + sdkSide + " instrumented method : " + joinPoint.getSignature().getName());
			logger.info("SDK " + sdkSide + " captured arguments : " + Arrays.toString(joinPoint.getArgs()));

			// Find Transfer Object
			transferObject = findTransferObject(joinPoint.getArgs(), parameterPosition, parameterDeepAccess);

			if (transferObject != null) {

				/*
				 * Handle Tag
				 */
				if (outgoing) {
					// create Tag and mark Object outgoing side
					tracer = oneAgentSdk.traceOutgoingRemoteCall(SDKUtil.methodName, SDKUtil.serviceName,
							SDKUtil.serviceEndpoint, SDKUtil.channelType, SDKUtil.channelEndpoint);
					((OutgoingRemoteCallTracer) tracer).setProtocolName(SDKUtil.serviceProtocol);
				}
				// Receive (incoming) side
				else {
					tracer = oneAgentSdk.traceIncomingRemoteCall(SDKUtil.methodName, SDKUtil.serviceName,
							SDKUtil.serviceEndpoint);
					tag = getTagAndClean(transferObject);
					((IncomingRemoteCallTracer) tracer).setDynatraceStringTag(tag);
				}

				/*
				 * Start tracing
				 */
				tracer.start();
				try {

					if (outgoing) {
						// Get Tag and set in Object
						tag = ((OutgoingRemoteCallTracer) tracer).getDynatraceStringTag();
						logger.fine("SDK tag generated: " + tag);
						transferObject.put(SDKUtil.TAG_ID, tag);

					} else {
						// Set Protocol for incoming
						((IncomingRemoteCallTracer) tracer).setProtocolName(SDKUtil.serviceProtocol);
					}
					joinPoint.proceed(); // continue on the intercepted method

				} catch (Throwable e) {
					// mark if an error happened
					logger.severe("Exception catched in instrumented method:" + e.getMessage());
					tracer.error(e);
					// not swallowing the exception, throwing it back to the application
					throw e;

				} finally {
					// close the remote call
					logger.fine("SDK ending call");
					// end the call
					tracer.end();
					// Remove the tag (Client and Server side), try to influence the logic of the
					// application the least possible.
					transferObject.remove(SDKUtil.TAG_ID);
				}

			} else {
				logger.warning("No transferObject found. No trace was started on the " + sdkSide + " side.");
			}
		} else {
			logger.warning("SDK Agent inactive. No trace was started on the " + sdkSide + " side.");
			joinPoint.proceed(); // continue on the intercepted method
		}
	}

	/**
	 * Will find the X-Dynatrace Tag inside the Map, return it and clean the
	 * modified Map.
	 * 
	 * @param args
	 * @return the X-Dynatrace Tag
	 */
	private static String getTagAndClean(Map o) {
		Object t = o.get(SDKUtil.TAG_ID);
		if (t != null && t instanceof String) {
			String tag = (String) t;
			logger.fine("Tag received: " + tag);
			// Remove the tag, try to influence the logic of the application the least
			// possible.
			o.remove(SDKUtil.TAG_ID);
			return tag;
		}
		return null;
	}

	/**
	 * The transfer Object will be searched within the method parameters with the
	 * position index and an access method for deep object access.
	 * 
	 * @param args
	 * @param position
	 * @param access
	 * @return
	 */
	private static Map findTransferObject(Object[] args, int position, String access) {

		Object transferObject = null;
		if (args.length >= position) {
			// Access parameter
			Object o = args[position - 1];

			/*
			 * Access transfer Object with reflection
			 */
			if (!access.isEmpty()) {
				try {
					logger.fine("Finding Object with reflection on " + o.getClass().getName());
					Method method = o.getClass().getDeclaredMethod(access);
					transferObject = method.invoke(o);

				} catch (Exception e) {
					logger.severe("There was an error getting the transfer object:" + e);
					e.printStackTrace();
				}
			} else {
				// Access directly, the parameter is the transfer Object
				transferObject = o;
			}
			logger.info("Transfer Object found: " + transferObject.getClass().getName() + "->" + o);

			/*
			 * Validate transfer Object
			 */
			if (transferObject instanceof Map) {
				logger.fine("Transfer Object valid: " + o.getClass().getName() + "->" + o);
				return (Map) transferObject;
			} else {
				logger.fine("Transfer Object not valid: " + o.getClass().getName() + "->" + o);
				return null;
			}

		} else {
			logger.warning("The parameter of the method on position " + position + " can't be found");
			logger.warning("The method has a signature with " + args.length + " parameters");
		}
		logger.warning("No tagging will be made");
		return null;
	}
}
