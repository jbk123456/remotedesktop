package com.github.remotedesktop.socketserver.plugin.websocket;

import static com.github.remotedesktop.socketserver.Attachment.addWriteBuffer;
import static com.github.remotedesktop.socketserver.Attachment.getMulticastGroup;
import static com.github.remotedesktop.socketserver.Attachment.setDebugContext;
import static com.github.remotedesktop.socketserver.Attachment.setMulticastGroup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.remotedesktop.socketserver.Group;
import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.plugin.base.DataFilterPlugin;

public final class WebSocketDataFilterPlugin<T extends SocketServer> implements DataFilterPlugin {

	protected static final Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final String STRING = "HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
			+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: ";

	static final Logger LOGGER = Logger.getLogger(WebSocketDataFilterPlugin.class.getName());

	private static final Pattern WS_MATCHER = Pattern.compile("Sec-WebSocket-Key: (.*)");
	private static final String HEADER_SEPARATOR = "\r\n\r\n";
	private static final Pattern GET_MATCHER = Pattern.compile("^GET");
	private static final String ID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private StringBuilder wsresponse = new StringBuilder();
	private StringBuilder setWebSocketAccept = new StringBuilder();
	private final MessageDigest sha1;
	protected final WebSocketEncoderDecoder websocketProtocolParser;
	private SocketServer server;

	public WebSocketDataFilterPlugin(T server) throws IOException, NoSuchAlgorithmException {
		this.server = server;
		sha1 = MessageDigest.getInstance("SHA-1");
		websocketProtocolParser = new WebSocketEncoderDecoder();
	}

	private byte[] getWebsocketData(SelectionKey key, byte inputData[]) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(inputData);
		Scanner s = new Scanner(in, StandardCharsets.UTF_8.name());
		try {
			String upgradeData = s.useDelimiter("\\r\\n\\r\\n").next();
			Matcher get = GET_MATCHER.matcher(upgradeData);
			if (get.find()) {
				Matcher match = WS_MATCHER.matcher(upgradeData);
				boolean find = match.find();
				if (!find) {
					return inputData;
				}
				byte[] response = getWebsocketResponse(match);
				addWriteBuffer(key, ByteBuffer.wrap(response));
				if (getMulticastGroup(key) == null) {
					setDebugContext(key, "websocket request");
					setMulticastGroup(key, Group.BROWSERS);
					LOGGER.info("Browser connected: " + key.attachment());
				}
				server.write(key);
				return null;
			} else if (getMulticastGroup(key) == Group.BROWSERS) {
				return websocketProtocolParser.decodeFrames(inputData);
			}
		} catch (NoSuchAlgorithmException e) {
			// ignore
		} finally {
			s.close();
		}

		return inputData;
	}

	private byte[] getWebsocketResponse(Matcher match) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		wsresponse.setLength(0);

		wsresponse.append(STRING);
		wsresponse.append(getSecWebSocketAccept(match.group(1)));
		wsresponse.append(HEADER_SEPARATOR);
		return wsresponse.toString().getBytes(StandardCharsets.UTF_8.name());
	}

	private String getSecWebSocketAccept(String match) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		setWebSocketAccept.setLength(0);

		setWebSocketAccept.append(match);
		setWebSocketAccept.append(ID);

		return BASE64_ENCODER
				.encodeToString(sha1.digest((setWebSocketAccept.toString().getBytes(StandardCharsets.UTF_8.name()))));
	}

	@Override
	public byte[] filter(SelectionKey key, byte[] data) throws IOException {
		return getWebsocketData(key, data);
	}
}
