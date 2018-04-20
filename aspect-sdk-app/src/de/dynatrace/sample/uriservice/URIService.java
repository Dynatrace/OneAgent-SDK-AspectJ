package de.dynatrace.sample.uriservice;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class URIService {
	
	private URIService() {
		
	}
	
	/**
	 * 
	 * @param urlString
	 * @throws Exception
	 */
	public static String doCheckUrl(String urlString) throws Exception {
		String status = "NOK";
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "text/html");
		conn.connect();
		Map<String, List<String>> headers = conn.getHeaderFields();
		System.out.println("Connected to " + urlString + ". Headers -> " + headers);
		return status;
	}
	
	/**
	 * Testing calling with reflection.
	 * 
	 * @param url
	 */
	public static void doReflection(String url) {
		try {
			System.out.println("calling with reflection.");
			Class<?> executeTask = Class.forName("de.dynatrace.sample.uriservice.URIService");
			Method method = executeTask.getDeclaredMethod("doCheckUrl", String.class);
			method.invoke(null, url);
		} catch (Exception e) {
			System.out.println("Exception handled by program: " + e);
			e.printStackTrace();
		}
	}

}
