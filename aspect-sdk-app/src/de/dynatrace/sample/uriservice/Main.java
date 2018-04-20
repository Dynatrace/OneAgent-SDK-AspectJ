package de.dynatrace.sample.uriservice;

public class Main {

	/**
	 * Class for starting the Client or the Server application.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length <= 0) {
			System.out.println("Usage: client url1 [url2] [-p:12345 -h:localhost]");
			System.out.println("Usage: server [-p:12345]");
		} else {
			if (args[0].contains("client")) {
				Client.main(arrayWithoutFirstElement(args));
			} else if (args[0].contains("server")) {
				Server.main(arrayWithoutFirstElement(args));
			}
		}
	}

	/**
	 * Cleans
	 * 
	 * @param args
	 * @return
	 */
	private static String[] arrayWithoutFirstElement(String[] args) {
		String[] arr = new String[args.length - 1];
		java.lang.System.arraycopy(args, 1, arr, 0, args.length - 1);
		return arr;
	}

}
