package com.ipeer.ytua.engine;

public class User {

	public String nick, identd, address, server, modes, realname;
	
	public User(String identd, String address, String server, String nick, String modes, String realname) { 
		this.identd = identd;
		this.address = address;
		this.server = server;
		this.nick = nick;
		this.modes = modes;
		this.realname = realname;
	}
	
	public boolean isOp() {
		return modes.contains("@");
	}
	
	public boolean isHop() {
		return modes.contains("%");
	}
	
	public boolean isVoice() {
		return modes.contains("+");
	}
	
	public boolean isReg() {
		return !isOp() && !isHop() && !isVoice();
	}
	
	public String getAddress() {
		return address;
	}
	
}
