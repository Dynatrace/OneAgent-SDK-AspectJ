package de.dynatrace.aspectj.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;

public class SDKUtil {

	private static final String CONFIG_PROPERTIES = "config.properties";
	private static final String LOGGER_PROPERTIES = "logger.properties";
	private static final String CONF_DIR = "conf/";

	public static String TAG_ID = "X-Dynatrace";

	public static String methodName = "methodName";
	public static String serviceProtocol = "serviceProtocol";
	public static String serviceEndpoint = "serviceEndpoint";
	public static String serviceName = "serviceName";
	public static String channelEndpoint = "";
	public static String send_parameter_deepaccess = "";
	public static String receive_parameter_deepaccess = "";
	public static boolean clean_after_send = false;
	public static boolean clean_after_receive = false;
	public static int send_parameter_position = 1;
	public static int receive_parameter_position = 1;
	public static ChannelType channelType = com.dynatrace.oneagent.sdk.api.enums.ChannelType.TCP_IP;

	private Properties properties;
	private static Logger logger = Logger.getLogger(Properties.class.getName());

	// Singleton
	private static SDKUtil instance;

	public final OneAgentSDK oneAgentSdk;

	private SDKUtil() {

		initLoger();
		loadProperties();
		logger.info("SDKUtil started");
		oneAgentSdk = OneAgentSDKFactory.createInstance();
		switch (oneAgentSdk.getCurrentState()) {
		case ACTIVE:
			logger.info("SDK is active and capturing.");
			break;
		case PERMANENTLY_INACTIVE:
			logger.severe(
					"SDK is PERMANENTLY_INACTIVE; Probably no OneAgent injected or OneAgent is incompatible with SDK.");
			break;
		case TEMPORARILY_INACTIVE:
			logger.severe("SDK is TEMPORARILY_INACTIVE; OneAgent has been deactivated - check OneAgent configuration.");
			break;
		default:
			logger.severe("SDK is in unknown state.");
			break;
		}
	}

	public static void initLoger() {
		try {
			FileInputStream inStream = new FileInputStream(getFileFromConf(LOGGER_PROPERTIES));
			LogManager.getLogManager().readConfiguration(inStream);
			logger.info("Logger initialized");
		} catch (Exception e) {
			logger.severe("Problem initializing log" + e.getMessage());
		}
	}

	/**
	 * Will look for a file in the conf directory next where this class/jar is being
	 * located. If not found as default solution the file will be looked in a conf
	 * directory relative to the Java execution.
	 * 
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getFileFromConf(String filename) throws FileNotFoundException {

		File file = null;
		String absolutePath = null;
		Class<?> referenceClass = SDKUtil.class;
		URL url = referenceClass.getProtectionDomain().getCodeSource().getLocation();

		try {
			// Get the parent directory
			File CLASSPATH = new File(url.toURI()).getParentFile();
			// get the file from where the jar is located.
			String relativePath = CONF_DIR + filename;
			absolutePath = CLASSPATH.getAbsolutePath() + "/" + relativePath;

			file = new File(absolutePath);

			if (!file.exists()) {
				file = new File(relativePath);
			}
			if (!file.exists()) {
				System.err.println("The file can't be found in " + absolutePath + " nor relative " + relativePath);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return file;
	}

	/**
	 * 
	 * @param ClassFqn
	 * @return
	 */
	public static Logger getLogger(String classFqn) {
		return getInstance().logger.getLogger(classFqn);
	}

	public static SDKUtil getInstance() {
		if (SDKUtil.instance == null) {
			SDKUtil.instance = new SDKUtil();
		}
		return SDKUtil.instance;
	}

	private void loadProperties() {

		try {
			FileInputStream input = new FileInputStream(getFileFromConf(CONFIG_PROPERTIES));
			properties = new Properties();
			properties.load(input);

			String methodName = properties.getProperty("methodName");
			if (methodName != null) {
				SDKUtil.methodName = methodName;
			}
			String serviceProtocol = properties.getProperty("serviceProtocol");
			if (serviceProtocol != null) {
				SDKUtil.serviceProtocol = serviceProtocol;
			}

			String serviceEndpoint = properties.getProperty("serviceEndpoint");
			if (serviceEndpoint != null) {
				SDKUtil.serviceEndpoint = serviceEndpoint;
			}

			String serviceName = properties.getProperty("serviceName");
			if (serviceName != null) {
				SDKUtil.serviceName = serviceName;
			}
			String channelEndpoint = properties.getProperty("channelEndpoint");
			if (channelEndpoint != null) {
				SDKUtil.channelEndpoint = channelEndpoint;
			}
			String send_parameter_position = properties.getProperty("send_parameter_position");
			if (send_parameter_position != null) {
				Integer position = Integer.valueOf(send_parameter_position);
				if (position > 0) {
					SDKUtil.send_parameter_position = position;
				}
			}
			String send_parameter_deepaccess = properties.getProperty("send_parameter_deepaccess");
			if (send_parameter_deepaccess != null) {
				SDKUtil.send_parameter_deepaccess = send_parameter_deepaccess;
			}
			String receive_parameter_position = properties.getProperty("receive_parameter_position");
			if (receive_parameter_position != null) {
				Integer position = Integer.valueOf(receive_parameter_position);
				if (position > 0) {
					SDKUtil.receive_parameter_position = position;
				}
			}
			String receive_parameter_deepaccess = properties.getProperty("receive_parameter_deepaccess");
			if (receive_parameter_deepaccess != null) {
				SDKUtil.receive_parameter_deepaccess = receive_parameter_deepaccess;
			}
			String channelType = properties.getProperty("channelType");
			if (channelType != null) {
				SDKUtil.channelType = ChannelType.valueOf(channelType);
			}
			String clean_after_send = properties.getProperty("clean_after_send");
			if (clean_after_send != null) {
				SDKUtil.clean_after_send = Boolean.valueOf(clean_after_send);
			}
			String clean_after_receive = properties.getProperty("clean_after_receive");
			if (clean_after_receive != null) {
				SDKUtil.clean_after_receive = Boolean.valueOf(clean_after_receive);
			}

			logger.info("Properties loaded:" + properties.size());
			logger.fine("Properties loaded:" + properties.toString());

		} catch (Exception e) {
			logger.severe("Properties could not be loaded: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
