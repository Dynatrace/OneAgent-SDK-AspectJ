package de.dynatrace.sample.uriservice;

import java.util.Map;

public class TransferObject {
	private Map<String, String> clientValues;

	public TransferObject(Map clientValues) {
		super();
		this.clientValues = clientValues;
	}

	public Map getClientValues() {
		return clientValues;
	}

	public void setClientValues(Map clientValues) {
		this.clientValues = clientValues;
	}

	public String getMessage() {
		return clientValues.get("message");
	}

	@Override
	public String toString() {
		return "TransferObject [clientValues=" + clientValues + "]";
	}

}
