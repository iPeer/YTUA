package com.ipeer.ytua.engine;

import java.io.IOException;
import java.text.NumberFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VideoInfo {

	public VideoInfo() { }

	public static void getVideoInfo(Engine engine, int outType, String channel, String ID) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		String URL = "http://gdata.youtube.com/feeds/api/videos/"+ID;
		DocumentBuilder a = f.newDocumentBuilder();
		Document doc = a.newDocument();
		doc = a.parse(URL);
		Element e = doc.getDocumentElement();
		e.normalize();
		NodeList author = e.getElementsByTagName("author");
		String user = author.item(0).getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
		NodeList title = e.getElementsByTagName("title");
		String title1 = title.item(0).getChildNodes().item(0).getNodeValue();
		NodeList videoInfo = e.getElementsByTagName("media:content");
		String duration = videoInfo.item(0).getAttributes().item(0).getNodeValue();
		NodeList views = e.getElementsByTagName("yt:statistics");
		String viewCount = NumberFormat.getInstance().format(Integer.parseInt(views.item(0).getAttributes().item(1).getNodeValue()));
		NodeList descriptionInfo = e.getElementsByTagName("media:description");
		String description = "";
		try {
			description = descriptionInfo.item(0).getChildNodes().item(0).getNodeValue().replaceAll("\n", " ").replaceAll("  ", " ");
			if (description.length() > 150)
				description = description.substring(0, 150)+"...";
		}

		catch (NullPointerException e1) {
			description = "";
		}
		int time = Integer.parseInt(duration);
		int minutes = time / 60;
		int seconds = time % 60;
		int hours = 0;
		while (minutes > 60) {
			hours++;
			minutes -= 60;
		}
		String newDuration = (hours > 0 ? (hours < 10 ? "0"+hours : hours)+":" : "")+(minutes < 10 ? "0"+minutes : minutes)+":"+(seconds < 10 ? "0"+seconds : seconds);
		char dash = 6;
		String out = Engine.colour+"14["+Engine.colour+"13"+user+Engine.colour+"14] "+Engine.colour+"13"+title1+Engine.colour+"14 ["+Engine.colour+"13"+newDuration+Engine.colour+"14] ("+Engine.colour+"13"+viewCount+Engine.colour+"14 views) "+dash+" "+Engine.colour+"13http://youtu.be/"+ID;
		String out2 = Engine.colour+"14Description: "+Engine.colour+"13"+description;
		String[] outArray = {out, out2};
		if (Engine.channels.size() > 0) 
			if (description.equals(""))
				engine.msg(channel, out);
			else
				if (outType == 1)
					engine.msgArray(channel, outArray);
				else
					engine.noticeArray(channel, outArray);

		else
			System.out.println(out);
	}

	public static void main(String[] args) {
		try {
			getVideoInfo(Engine.engine, 0, "#Peer.dev", "J88J4B9rstY");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

}
