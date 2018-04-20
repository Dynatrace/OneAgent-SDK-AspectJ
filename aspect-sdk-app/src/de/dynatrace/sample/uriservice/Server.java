package de.dynatrace.sample.uriservice;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class Server {

	private static int port = 12345;

	/**
	 * Server listens on a specific port for a connection. Reads the first Object
	 * from the InputStream, closes the connection and waits for the next one.
	 * 
	 * @param port
	 * @throws Exception
	 */
	public Server(int port) throws Exception {

		ServerSocket serverSocket = new ServerSocket(port);
		Socket client = null;
		try {
			System.out.println("Waiting for clients on interface:port " + serverSocket.getInetAddress().getHostName()
					+ ":" + serverSocket.getLocalPort());

			boolean keepRunning = true;
			while (keepRunning) {
				try {
					System.out.println("Listening for Message");
					// blocks connection and waits until someone connects
					client = serverSocket.accept();
					System.out.println(
							"Client " + client.getInetAddress().getHostName() + ":" + client.getPort() + " connected");
					ObjectInputStream in = new ObjectInputStream(client.getInputStream());
					TransferObject transfer = new TransferObject((Map) in.readObject());

					handleMessage(transfer);

				} finally {
					client.close();
				}
			}
		} finally {
			serverSocket.close();
		}
	}

	/**
	 * Simulated Method where the transferObject is received.
	 * 
	 * @param client
	 * @throws Exception
	 */
	private void handleMessage(TransferObject transfer) throws Exception {
		System.out.println("Received TransferObject: " + transfer.getClass().getName() + "->" + transfer);
		doCheckUrlFromServerSide(transfer.getMessage());
	}

	/**
	 * 
	 * @param urlString
	 * @throws Exception
	 */
	static void doCheckUrlFromServerSide(String urlString) throws Exception {
		URIService.doCheckUrl(urlString);
	}

	/**
	 * Start the Server
	 * 
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String args[]) throws Exception {
		System.out.println("*************************************************************");
		System.out.println("**       Running remote call server                        **");
		System.out.println("*************************************************************");

		if (args.length > 0 && args[0] != null && args[0].contains("-p:")) {
			port = Integer.valueOf(args[0].split("-p:")[1]);
		}
		new Server(port);
	}

}
