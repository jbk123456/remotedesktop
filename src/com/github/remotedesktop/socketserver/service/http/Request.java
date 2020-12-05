package com.github.remotedesktop.socketserver.service.http;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Request {
	private static final Logger logger = Logger.getLogger(Request.class.getName());

	private URI uri;
	private Properties params;
	private Properties headers;
	private SelectionKey key;
	private byte[] data;
	private int line;
	private int remaining;
	@SuppressWarnings("unused")
	private String method;

	public Request(SelectionKey key) {
		params = new Properties();
		headers = new Properties();
		line = 0;
		this.key = key;
	}

	public int parse(byte b[]) throws IOException {
		for (int i = 0; i < b.length - 3; i++) {
			if (b[i] == '\r' && b[i + 1] == '\n' && b[i + 2] == '\r' && b[i + 3] == '\n') {
				parse(new StringBuilder(new String(b, 0, i + 4)));
				String contentLenth = getHeader("Content-Length");
				if (contentLenth == null) {
					contentLenth = "0";
				}
				int len = Integer.parseInt(contentLenth);
				remaining = b.length - (i + 4 + len);
				if (remaining >= 0) {
					data = new byte[len];
					System.arraycopy(b, i + 4, data, 0, len);
					return remaining;
				}
			}
		}

		// we weren't able to read the request fully, push it back
		// and read again
		remaining = -1;
		return remaining;
	}

	public String getHeader(String s) {
		return headers.getProperty(s.toLowerCase());
	}

	public byte[] getData() {
		return data;
	}

	public SelectionKey getKey() {
		return key;
	}

	protected void parse(StringBuilder requestBuffer) throws IOException {
		int i = 0;
		String s = "";
		do {
			if ((i = requestBuffer.indexOf("\r\n")) >= 0) {
				s = requestBuffer.substring(0, i);
				requestBuffer.delete(0, i + 2);
			}
			if (i >= 0)
				parseLine(s);
		} while (i >= 0);
	}

	public void parseLine(String s) throws IOException {
		if (line++ == 0) {
			parseRequestLine(s);
		} else {
			parseHeaderLine(s);
		}
	}

	protected void parseHeaderLine(String s) throws IOException {
		int i = s.indexOf(':');
		if (i != -1) {
			headers.put(s.substring(0, i).trim().toLowerCase(), s.substring(i + 1).trim());
		}
	}

	protected void parseRequestLine(String s) throws IllegalArgumentException {
		StringTokenizer stringtokenizer = new StringTokenizer(s);
		method = stringtokenizer.nextToken().toUpperCase();
		try {
			uri = URI.create(
					(new StringBuilder()).append("http://example").append(stringtokenizer.nextToken()).toString());
			if (uri.getQuery() != null) {
				params.putAll(parseUriQuery(uri.getQuery()));
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, "parse request line", t);
		}
	}

	private Properties parseUriQuery(String s) {
		Properties properties = new Properties();
		String as[] = s.split("&");
		for (int i = 0; i < as.length; i++) {
			if (!as[i].contains("="))
				continue;
			String as1[] = as[i].split("=");
			String s1 = as1[0];
			String s2;
			if (as1.length > 1)
				s2 = as1[1];
			else
				s2 = "";
			properties.setProperty(s1, s2);
		}

		return properties;
	}

	public URI getURI() {
		return uri;
	}

	public String getParam(String s) {
		return params.getProperty(s);
	}

}