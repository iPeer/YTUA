package com.ipeer.ytua.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class Engine implements Runnable {

	private String server = "irc.swiftirc.net";
	private int port = 6667;
	private String nick = "iYouTube";
	private File configFile;
	private Properties config;
	private BufferedWriter out;
	private BufferedReader in;
	private Map<String, String> networkSettings = new HashMap<String, String>();
	public static Map<String, Channel> channels = new HashMap<String, Channel>();
	public static Map<String, String> networks = new HashMap<String, String>();
	public static VideoChecker videoChecker;
	public long lastUpdateTime;
	public String MY_NICK;

	public static Engine engine;

	protected Utils utils;

	public static final char colour = 03;
	public static final char bold = 02;
	public static final char underline = (char)1f;
	public static final char italics = (char)1d;
	public static final char reverse = 16;
	public static final char endall = (char)0f;

	protected static Socket connection;

	public Engine(String server, int port, String nick) throws FileNotFoundException, IOException {
		this.server = server;
		this.port = port;
		this.nick = nick;
		File lockFile = new File("F:\\Dropbox\\Java\\YTUA\\lock.lock");
		lockFile.createNewFile();
		lockFile.deleteOnExit();
		videoChecker = new VideoChecker(this);
		config = new Properties();
		configFile = new File("F:\\Dropbox\\Java\\YTUA\\config\\config.cfg");
		System.out.println(configFile+", "+configFile.exists());
		utils = new Utils(this);
		if (configFile.exists())
			config = loadConfig();
		else {
			generateDefaultConfig();
		}
	}

	public void start() {
		(new Thread(this)).start();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Engine engine = new Engine("irc.swiftirc.net", 6667, "iYouTube");
		engine.start();
	}

	public void run() {
		try {
			connection = new Socket(server, port);
			//nick = "iJava";
			out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			out.write("NICK "+nick+"\r\n");
			out.write("USER "+nick+" ipeer.ipeerftw.co.cc "+nick+": YT Notify\r\n");
			out.flush();

			this.MY_NICK = nick;

			System.out.println("Logged in, waiting for connection success...");

			// Wait for connection status
			String line = null;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
				if (line.indexOf("001") >= 0)
					this.server = line.split(" ")[0].substring(1);
				if (line.indexOf("005") >= 0) {					
					String[] a = line.split(" ");
					for (int x = 3; x < a.length; x++) {
						if (a[x].contains("=")) {
							String[] b = a[x].split("=");
							networkSettings.put(b[0], b[1]);
						}
					}
				}
				if (line.indexOf("251") >= 0) {
					System.out.println("Network settings...");
					String network = null;
					if ((network = networkSettings.get("NETWORK")) != null && !networks.containsKey(network))
						networks.put(network, server);
					else {
						int u = 1;
						while (networks.containsKey("Unknown"+u))
							u++;
						networks.put("Unknown"+u, server);
					}
					break;
				}
				if (line.indexOf("004") >= 0) {
					System.out.println("Connection success.\nIdentifying with server...");
					identify();
					setSelfAsBot();
				}
				else if (line.indexOf("433") >= 0) {
					System.out.println("Server says nick is in use.");
					nick = "iJava";
					out.write("NICK iJava\r\n");
					out.write("USER "+nick+" ipeer.ipeerftw.co.cc "+nick+": YT Notify\r\n");
					out.flush();
				}
			}

			join("#QuestHelp");
			join("#Peer.dev");
			// Read from the server while the connection is active

			line = null;
			while ((line = in.readLine()) != null) {
				parseLine(line);
			}
			System.out.println("Server closed the connection");
			System.exit(0);

		} catch (Exception e) {
			try {
				quit("SEVERE: "+e.toString()+" @ "+e.getStackTrace()[0]);
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(0);
			e.printStackTrace();
		}
	}

	public void generateDefaultConfig() throws FileNotFoundException, IOException {
		if (config == null)
			config = new Properties();
		config.put("ircName", nick);
		config.put("ircServer", server);
		config.put("ircPort", Integer.toString(port));
		config.put("commandChars", "!.@#");
		config.put("botVersion", Double.toString(0.1));
		config.put("networkPass", "");
		if (!config.isEmpty())
			config.store(new FileOutputStream(configFile), "Java IRC Bot Config File");
	}

	public Properties loadConfig() throws FileNotFoundException, IOException {
		Properties o = new Properties();
		try {
			o.load(new FileInputStream(configFile));
			return o;
		} catch (Exception e) {
			generateDefaultConfig();
			e.printStackTrace();
			return new Properties();
		}

	}

	private void parseLine(String line) throws IOException {
		//System.out.println(line);
		if (line.startsWith("PING ")) {
			out.write("PONG "+line.substring(5)+"\r\n");
			System.out.println(line);
			System.out.println("PONG "+line.substring(5));
			out.flush();
		}
		else if (line.split(" ")[1].equals("MODE")) {
			String[] raw = line.split(" ");
			String channel = raw[2];
			System.out.println(line);
			for (int x = 4; x < raw.length; x++) {
				if (!raw[x-1].equals(raw[x])) {
					who("+cn "+channel+" "+raw[x]);
				}
			}
		}
		else if (line.split(" ")[1].equals("NICK")) {
			String nick = getNick(line);
			String newNick = line.split(":")[2];
			System.out.println(nick+" changed their nick to "+newNick);
			for (Channel c : channels.values()) {
				if (c.getUserList().containsKey(nick)) {
					User a = c.getUserList().get(nick);
					User b = new User(a.identd, a.address, a.server, newNick, a.modes, a.realname);
					c.getUserList().remove(nick);
					c.getUserList().put(newNick, b);
				}
			}
		}
		else if (line.split(" ")[1].equals("QUIT")) {
			System.out.println(line);
			String nick = getNick(line);
			for (Channel c : channels.values())
				if (c.getUserList().containsKey(nick))
					c.getUserList().remove(nick);
		}
		else if (line.split(" ")[1].equals("PART")) {
			System.out.println(line);
			String nick = getNick(line);
			String channel = line.split(" ")[2];
			Channel c = channels.get(channel.toLowerCase());
			c.getUserList().remove(nick);
		}
		else if (line.split(" ")[1].equals("JOIN")) {
			System.out.println(line);
			String nick = getNick(line);
			String channel = line.split(" ")[2].substring(1);
			who("+cn "+channel+" "+nick);
		}
		else if (line.split(" ")[1].equals("INVITE")) {
			System.out.println(line);
			String nick = getNick(line);
			String channel = line.split(":")[2];
			System.out.println("Invited to channel "+channel+" by "+nick);
			join(channel);
		}
		else if (line.split(" ")[1].equals("352")) {
			System.out.println(line);
			String[] a = line.split(" ");
			String channel = a[3].toLowerCase();
			String realName = line.split(":")[2];
			User b = new User(a[4], a[5], a[6], a[7], a[8], realName);
			channels.get(channel).getUserList().put(a[7], b);
		}

		else if (line.split(" ")[1].equals("315")) {
			if (!videoChecker.isRunning) {
				videoChecker.isRunning = true;
				System.out.println("[DEBUG] videoChecker Started "+videoChecker.toString());
				//(new Thread(videoChecker)).start();
				videoChecker.start();
			}
		}
		else if (line.contains("PRIVMSG")) {
			String[] args = makePRIVMSGBotFriendly(line);
			String noticeChars = ".!";
			String channel = args[1];
			String nick = args[0];
			String message = args[2];
			Channel channelObject = channels.get(channel.toLowerCase());
			Channel adminChannelObject = channels.get("#peer.dev");
			System.out.println(nick+": "+message);
			if (message.startsWith("")) {			
				String CTCPType = message.replaceAll("", "");
				System.out.println(CTCPType+" from "+nick);
				if (CTCPType.equals("VERSION"))
					notice(nick,"VERSION YTUA v"+config.getProperty("botVersion")+" by iPeer");
				else if (CTCPType.startsWith("PING"))
					notice(nick,"PING "+message.substring(6));
				else if (CTCPType.equals("TIME"))
					notice(nick,"TIME "+new SimpleDateFormat().format(System.currentTimeMillis()));
			}
			if (config.getProperty("commandChars").contains(message.substring(0, 1))) {
				String command = message.substring(1);
				String commandPrefix = message.substring(0, 1);
				
				if (command.startsWith("quit") && utils.isAdmin(adminChannelObject, nick)) {
					try {
						String message2 = line.split(".quit ")[1];
						quit("Quit from "+nick+" ("+message2+")");
					}
					catch (ArrayIndexOutOfBoundsException e) {
						quit("Quit from "+nick);
					}
				}

				//			if (message.startsWith(".connectTo") && utils.isAdmin(adminChannelObject, nick)) {
				//				String[] args1 = message.split(" ");
				//				String server = args1[1];
				//				int port = Integer.parseInt(args1[2]);
				//				String nick1 = args1[3];
				//				System.out.println(server+":"+port+" "+nick1);
				//				Engine e2 = new Engine(server, port, nick1);
				//				e2.start();
				//			}


				if (command.startsWith("adduser") && utils.isAdmin(adminChannelObject, nick)) {
					String user = line.split(".adduser ")[1];
					videoChecker.addUser(user, channel);
				}

				if (command.startsWith("deluser") && utils.isAdmin(adminChannelObject, nick)) {
					String user = line.split(".deluser ")[1];
					videoChecker.removeUser(user, channel);
				}

				if (command.equals("listusernames") && utils.isAdmin(adminChannelObject, nick)) {
					if (noticeChars.contains(commandPrefix))
						notice(nick, videoChecker.getUsernames().toString());
					else
						msg(channel, videoChecker.getUsernames().toString());
				}

				if (command.equals("listnetworks") && utils.isAdmin(adminChannelObject, nick)) {
					if (noticeChars.contains(commandPrefix))
						notice(nick, networks.toString());
					else
						msg(channel, networks.toString());
				}

				if (command.equals("listusers") && utils.isAdmin(adminChannelObject, nick))
					if (!noticeChars.contains(commandPrefix))
						msg(channel, channels.get(channel.toLowerCase()).getUserList().toString());
					else
						notice(nick, channels.get(channel.toLowerCase()).getUserList().toString());

				if (command.startsWith("rank")) {
					try {
						String newUser = "";
						try {
							newUser = message.split(".rank ")[1];
						}
						catch (Exception e) {
							newUser = nick;
						}
						String newChannel = channel.toLowerCase();
						User u = channels.get(newChannel).getUserList().get(newUser);
						msg(channel, "@:"+u.isOp()+", %:"+u.isHop()+", +:"+u.isVoice()+", R:"+u.isReg());
					}
					catch (NullPointerException e) {
						msg(channel, "NullPointer all up in yo face!");
					}
				}

				if (command.equals("amIadmin"))
					msg(channel, utils.isAdmin(adminChannelObject, nick));
				
				if (command.startsWith("hasIAL") && utils.isAdmin(adminChannelObject, nick)) {
					String channel2 = "";
					try {
						channel2 = message.split(".hasIAL ")[1];
					}
					catch (ArrayIndexOutOfBoundsException a) {
						msg(channel, "You must specify a channel!");
					}
					boolean Empty;
					try {
						Empty = !channels.get(channel2.toLowerCase()).getUserList().isEmpty();
					}
					catch (NullPointerException e) {
						Empty = false;
					}
					msg(channel, Empty);
				}

				if (command.equals("updateIAL") && utils.isAdmin(adminChannelObject, nick)) {
					channels.get(channel.toLowerCase()).getUserList().clear();
					who(channel);
				}

				if ((command.equals("forceuserupdate") || command.equals("forceupdate") ) && utils.isAdmin(adminChannelObject, nick)) {
					msg(channel, "Forcing update, please stand by...");
					videoChecker.updateUserNames();
					msg(channel, "Finished forcing update on "+videoChecker.getUsernames().size()+" users!");
				}

				if (command.equals("part") && utils.isAdmin(adminChannelObject, nick)) {
					part(channel);
				}

				if (command.equals("info")) {
					long totalMemory = Runtime.getRuntime().totalMemory();
					long freeMemory = Runtime.getRuntime().freeMemory();
					long usedMemory = totalMemory - freeMemory;
					String memory = (usedMemory / 1024L / 1024L)+"MB/"+(totalMemory / 1024L / 1024L)+"MB";
					System.out.println(memory);
					String out =  "Memory Usage: "+memory+" | "+channels.size()+" channels | "+videoChecker.getUsernames().size()+" watched users.";
					if (noticeChars.contains(commandPrefix))
						notice(nick, out);
					else
						msg(channel, out);
				}
				if (command.startsWith("lastvideo")) {
					int outType = message.startsWith("@") ? 1 : 0;
					GetLatestVideo.lookupLatestVideo(this, outType, outType == 1 ? channel : nick, message.split(".lastvideo ")[1].split(" ")[0]);
				}
			}
			else if (message.equals("+voice"))
				send("MODE "+channel+" +v "+nick);

			else if (message.replaceAll("https://", "http://").contains("http://www.youtube.com/") && line.indexOf("video_response_view_all") < 0 && !nick.equals("jYouTube")) {
				message = message.replaceAll("https://", "http://");
				String videoID = null;
				int indexOf = message.indexOf("http://www.youtube.com/watch?");
				String newMessage = message.substring(indexOf);
				try {
					videoID = newMessage.split(" ")[0].split("v=")[1].split("&")[0];
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				if (videoID != null)
					try {
						VideoInfo.getVideoInfo(this, 1, channel, videoID);
					} catch (Exception e) {
						msg(channel, "Error getting video info: "+e.toString());
						e.printStackTrace();
					} 
			}

			else if (message.replaceAll("https://", "http://").contains("http://youtu.be/") && !nick.equals("jYouTube")) {
				message = message.replaceAll("https://", "http://");
				String a = message.split("http://youtu.be/")[1].split("&")[0].split(" ")[0];
				System.out.println(a);
				try {
					VideoInfo.getVideoInfo(this, 1, channel, a);
				} catch (Exception e) {
					msg(channel, "Error getting video info: "+e.toString());
					e.printStackTrace();
				} 
			}
		}
		else
			System.out.println(line);
	}

	private String getNick(String line) {
		return line.split("!")[0].substring(1);
	}

	private void identify() throws IOException {
		String pass = config.getProperty("networkPass");
		if (pass == null || pass.equals("")) {
			System.out.println("No password is set!");
			return;
		}
		msg("NickServ", "identify "+pass);
	}

	private String[] makePRIVMSGBotFriendly(String line) {
		String nick = getNick(line);
		String channel = line.split(" ")[2];
		String[] message = line.split(":");
		String message1 = "";
		for (int x = 2; x < message.length; x++) {
			message1 = message1+":"+message[x];
		}
		String finalmessage = message1.substring(1);
		if (!channel.startsWith(networkSettings.get("CHANTYPES")))
			channel = nick;
		String[] a = {nick, channel, finalmessage};
		return a;
	}


	/* Commands to send Data to the IRC server */

	public void msg (String channel, boolean b) {
		msg(channel, Boolean.toString(b));
	}

	public void msg(String rec, String message) {
		if (rec == null || rec == "") {
			System.out.println("Invalid recipient");
			return;
		}
		if (message == null || message == "") {
			System.out.println("No text to send!");
			return;
		}
		try {
			out.write("PRIVMSG "+rec+" :"+message+"\r\n");
			out.flush();
			System.out.println(MY_NICK+": "+message);
		}
		catch (Exception e) {
			System.out.println("SEVERE: Unable to send message to server");
		}
	}

	private void notice(String rec, String message) throws IOException {
		if (rec == null || rec == "") {
			System.out.println("Invalid recipient");
			return;
		}
		if (message == null || message == "") {
			System.out.println("No text to send!");
			return;
		}
		out.write("NOTICE "+rec+" :"+message+"\r\n");
		out.flush();
	}

	private void quit(String message) throws IOException {
		out.write("QUIT :"+message+"\r\n");
		out.flush();
	}

	public void msgArray(String channel, String[] outArray) throws IOException {
		for (int x = 0; x < outArray.length; x++) {
			out.write("PRIVMSG "+channel+" :"+outArray[x]+"\r\n");
		}
		out.flush();
	}

	public void msgList(String channel, List<String> outList) throws IOException {
		Iterator<String> i = outList.iterator();
		while (i.hasNext())
			out.write("PRIVMSG "+channel+" :"+i.next()+"\r\n");
		out.flush();
	}

	private void setSelfAsBot() throws IOException {
		System.out.println("Setting myself as a bot on this network...");
		out.write("MODE "+MY_NICK+" +B\r\n");
		out.flush();		
	}

	private void join (String channel) throws IOException {
		out.write("JOIN "+channel+"\r\n");
		channels.put(channel.toLowerCase(), new Channel(channel));
		System.out.println("Now in "+channels.size()+" channel(s)");
		out.flush();
		who(channel);
	}

	private void part(String channel) throws IOException {
		channels.remove(channel);
		out.write("PART "+channel+"\r\n");
		out.flush();
	}

	private void who(String s) throws IOException {
		out.write("WHO "+s+"\r\n");
		out.flush();
	}

	public void send(String s) throws IOException {
		out.write(s+"\r\n");
		out.flush();
	}

	public void noticeArray(String user, String[] outArray) throws IOException {
		for (int x = 0; x < outArray.length; x++) {
			out.write("NOTICE "+user+" :"+outArray[x]+"\r\n");
		}
		out.flush();
	}

}
