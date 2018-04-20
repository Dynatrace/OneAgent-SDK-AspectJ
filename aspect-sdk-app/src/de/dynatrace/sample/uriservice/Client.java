package de.dynatrace.sample.uriservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Client implements Runnable {

	private static ArrayList<String> urls;
	private static int port = 12345;
	private static String host = "localhost";

	/**
	 * 
	 * @param urls
	 * @throws Exception
	 */
	public void run() {
		try {
			checkMultipleUrls();
			System.out.println("\nDone with checking the urls. Want to start a new thread again? y = yes");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		urls = new ArrayList<String>();
		for (String arg : args) {
			if (arg.contains("-p:")) {
				port = Integer.valueOf(arg.split("-p:")[1]);
			} else if (arg.contains("-h:")) {
				host = arg.split("-h:")[1];
			} else {
				urls.add(arg);
			}
		}

		if (urls.size() <= 0) {
			System.out.println("Usage: client url1 [url2] [-p:12345 -h:localhost]");
		} else {
			boolean keepRunning = true;
			while (keepRunning) {
				(new Thread(new Client())).start();
				String nextLine = new BufferedReader(new InputStreamReader(System.in)).readLine();
				keepRunning = nextLine.startsWith("y");
			}
		}
	}

	/**
	 * 
	 * @param urls
	 * @throws Exception
	 */
	static void checkMultipleUrls() throws Exception {

		System.out.println("Running with TID:" + Thread.currentThread().getId());
		for (String url : urls) {
			checkUrl(url);
		}
	}

	/**
	 * 
	 * @param url
	 * @throws Exception
	 */
	static void checkUrl(String url) throws Exception {

		URIService.doCheckUrl(url);

		URIService.doReflection(url);

		// A Map will be used as a transport Object.
		Map transportObject = getTransportObject(url);

		notifyServer(transportObject);
	}

	private static Map getTransportObject(String url) {
		Map transportObject = new HashMap<String, String>();
		transportObject.put("message", url);
		return transportObject;
	}

	/**
	 * 
	 * @param transportObject
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	private static void notifyServer(Map transportObject) throws UnknownHostException, IOException {
		Socket socket = null;
		
		
		try {
			socket = new Socket(host, port);
			System.out.println("Connected to " + socket.getInetAddress().getHostName() + ":" + socket.getPort());

			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(transportObject);

		} catch (ConnectException e) {
			System.err.println("Server is not Online:" + host + ":" + port);
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}





}
