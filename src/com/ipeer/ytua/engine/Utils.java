package com.ipeer.ytua.engine;

public class Utils {

	protected Engine engine;

	public Utils(Engine e) {
		this.engine = e;
	}

	public boolean addressesEqual(Channel c, String n) {
		String mynick = engine.MY_NICK;
		String a1 = c.getUserList().get(n).getAddress();
		String a2 = c.getUserList().get(mynick).getAddress();
		return a1.equals(a2);
	}

	public boolean isAdmin(Channel c, String n) {
		return c.getUserList().get(n).isOp() && addressesEqual(c, n);
	}

}
