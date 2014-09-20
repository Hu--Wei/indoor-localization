package com.jsondemo.activity;

public class ServerIP {
	private String serverIp;

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String str) {
		this.serverIp = str;
	}

	private static final ServerIP serverIP = new ServerIP();

	public static ServerIP getInstance() {
		return serverIP;
	}
}
