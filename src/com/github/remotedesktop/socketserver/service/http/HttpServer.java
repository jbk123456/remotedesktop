package com.github.remotedesktop.socketserver.service.http;

import static com.github.remotedesktop.socketserver.SocketServerAttachment.addWriteBuffer;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.getMulticastGroup;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.setDebugContext;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.setMulticastGroup;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.setPushBackBuffer;
import static java.lang.System.arraycopy;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.remotedesktop.Config;
import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.SocketServerMulticastGroup;
import com.github.remotedesktop.socketserver.service.KeyboardAndMouseSerializer;
import com.github.remotedesktop.socketserver.service.TileSerializationManaer;
import com.github.remotedesktop.socketserver.service.TileSerializer;

public class HttpServer extends SocketServer {
	private static final Logger logger = Logger.getLogger(HttpServer.class.getName());

	private static final String UTF_8 = "UTF-8";

	private static final Pattern WS_MATCHER = Pattern.compile("Sec-WebSocket-Key: (.*)");

	private static final Pattern GET_MATCHER = Pattern.compile("^GET");

	public static final String HTTP_OK = "200 OK";
	public static final String HTTP_NOTFOUND = "404 Not Found";
	public static final String HTTP_FORBIDDEN = "403 Forbidden";
	public static final String HTTP_INTERNALERROR = "500 Internal Server Error";
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
	public static final String TILEEXT = "JPG";

	private static final byte[] NO_WEBSOCKET_REQUEST = new byte[0];
	private static final String GET_CMD = "GET /";
	private static final String HEADER_SEPARATOR = "\r\n\r\n";

	private final TileSerializationManaer tileman;
	private final KeyboardAndMouseSerializer kvmman;
	private final WebSocketEncoderDecoder websocketProtocolParser;
	private final Map<String, String> updateableConfigs = new HashMap<>();

	public HttpServer(String hostname, int port) throws IOException {
		super("HttpServer", hostname, port);
		kvmman = new KeyboardAndMouseSerializer(GET_CMD, HEADER_SEPARATOR);
		tileman = new TileSerializationManaer();
		websocketProtocolParser = new WebSocketEncoderDecoder();

		updateableConfigs.put("quality", String.format(Locale.US, "%.2f", Config.jpeg_quality));
		updateableConfigs.put("fps", String.format("%d", (int) Config.fps));
	}

	public int getPort() throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) channel;
		return serverChannel.socket().getLocalPort();
	}

	@Override
	protected Selector openSelector() throws IOException {
		return SelectorProvider.provider().openSelector();
	}

	@Override
	protected SelectionKey channelRegister(Selector selector) throws ClosedChannelException {
		return channel.register(selector, OP_ACCEPT);
	}

	@Override
	protected AbstractSelectableChannel channel(InetSocketAddress address) throws IOException {
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(address);
		return channel;
	}

	@Override
	protected void handleIncomingData(SelectionKey key, byte[] data) throws IOException {

		byte[] websocketData = getWebsocketData(key, data);
		if (websocketData == null) {
			return;// upgrade
		}
		if (websocketData != NO_WEBSOCKET_REQUEST) {
			data = websocketData;
		}
		try {
			handle(key, data);
		} catch (Exception e) {
			// we shouldn't kill the http server if the client has sent garbage data for
			// example
			throw new IOException("httpserver could not create or handle response", e);
		}
	}

	private byte[] getWebsocketData(SelectionKey key, byte inputData[]) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(inputData);
		Scanner s = new Scanner(in, UTF_8);
		try {
			String upgradeData = s.useDelimiter("\\r\\n\\r\\n").next();
			Matcher get = GET_MATCHER.matcher(upgradeData);
			if (get.find()) {
				Matcher match = WS_MATCHER.matcher(upgradeData);
				boolean find = match.find();
				if (!find) {
					return NO_WEBSOCKET_REQUEST;
				}
				byte[] response = getWebsocketResponse(match);
				addWriteBuffer(key, ByteBuffer.wrap(response));
				if (getMulticastGroup(key) == null) {
					setDebugContext(key, "websocket request");
					setMulticastGroup(key, SocketServerMulticastGroup.RECEIVER);
					logger.fine("Browser connected: " + key.attachment());
				}
				write(key);
				return null;
			} else if (getMulticastGroup(key) == SocketServerMulticastGroup.RECEIVER) {
				return websocketProtocolParser.decodeFrames(inputData);
			}
		} catch (NoSuchAlgorithmException e) {
			// ignore
		} finally {
			close(s);
		}

		return NO_WEBSOCKET_REQUEST;
	}

	private byte[] getWebsocketResponse(Matcher match) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		return ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n" + "Upgrade: websocket\r\n"
				+ "Sec-WebSocket-Accept: "
				+ Base64.getEncoder()
						.encodeToString(MessageDigest.getInstance("SHA-1")
								.digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(UTF_8)))
				+ HEADER_SEPARATOR).getBytes(UTF_8);
	}

	private void handle(SelectionKey key, byte[] data) throws IOException {
//		setDebugDatat(key, data);
		Request req = null;
		int remaining = 0;
		do {
			if (remaining > 0) {

				byte[] newdata = new byte[remaining];
				arraycopy(data, data.length - remaining, newdata, 0, remaining);
				data = newdata;
			}
			req = new Request(key);
			remaining = req.parse(data);
			if (remaining < 0) {
				assert (getMulticastGroup(key) != SocketServerMulticastGroup.RECEIVER);
				setPushBackBuffer(key, ByteBuffer.wrap(data));
				return; // partial read, get the rest
			}
			Response res = new Response();
			String path = req.getURI().getPath();
			setDebugContext(key, path);
			switch (path) {

			case "/tiledoc": { // tiles prrocessed, write back document containing the links to the images
				StringBuilder sb = new StringBuilder();
				sb.append("document.getElementById(\"canvas\").style.cursor=\"");
				sb.append(req.getParam("cursor"));
				sb.append("\";");
				Random r = new Random(System.currentTimeMillis());
				for (int i = 0; i < tileman.getNumXTile(); i++) {
					for (int j = 0; j < tileman.getNumYTile(); j++) {
						TileSerializer tile = tileman.getTile(i, j);
						if (tile.isDirty()) {
							sb.append("document.getElementById('i");
							sb.append(i + "_" + j);
							sb.append("').src='getTile?x=");
							sb.append(i);
							sb.append("&y=");
							sb.append(j);
							sb.append("&c=");
							sb.append(r.nextInt(100000000));
							sb.append("';");
						}
					}
				}
				if (sb.length() > 0) {
					byte[] getImagesData = websocketProtocolParser.encodeFrame(sb.toString());
					int n = writeToGroup(SocketServerMulticastGroup.RECEIVER, ByteBuffer.wrap(getImagesData));
					if (n <= 0) { // notify last browser disconnect, stops screen capturer to send any further images
						kvmman.disconnect(0);
						writeToGroup(SocketServerMulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));
					}
				}
				break;
			}

			case "/tile": { // tile prrocessed,
				if (getMulticastGroup(key) == null) {
					setMulticastGroup(key, SocketServerMulticastGroup.SENDER);
					logger.fine("DisplayServer connected: " + key.attachment());
				}

				tileman.processImage(req.getData(), Integer.parseInt(req.getParam("x")),
						Integer.parseInt(req.getParam("y")), Integer.parseInt(req.getParam("w")),
						Integer.parseInt(req.getParam("h")));

				break;
			}

			case "/ping": {
				res.message(req.getData());
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
				break;
			}
			case "/": {
				res.redirect(String.format(Locale.US, "/remotedesktop.html?quality=%s&fps=%s",
						updateableConfigs.get("quality"), updateableConfigs.get("fps")));
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
				break;
			}
			case "/sendKey": {
				kvmman.keyStroke(Integer.parseInt(req.getParam("key")), Integer.parseInt(req.getParam("code")),
						Integer.parseInt(req.getParam("mask")));
				writeToGroup(SocketServerMulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));
				break;
			}
			case "/connect": {
				if (getMulticastGroup(key) == null) {
					setDebugContext(key, "websocket request");
					setMulticastGroup(key, SocketServerMulticastGroup.RECEIVER);
					logger.fine("Browser connected: " + key.attachment());
				}
				kvmman.connect(0);
				writeToGroup(SocketServerMulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));
				break;
			}
			case "/disconnect": {
				kvmman.disconnect(0);
				writeToGroup(SocketServerMulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));
				break;
			}
			case "/sendMouse": {
				int x = Integer.parseInt(req.getParam("x"));
				int y = Integer.parseInt(req.getParam("y"));
				String act = req.getParam("act");
				int button = Integer.parseInt(req.getParam("button"));

				kvmman.mouseMove(x, y);

				if (act.equals("press")) {
					kvmman.mousePress(button);
				}
				if (act.equals("release")) {
					kvmman.mouseRelease(button);
				}
				if (act.equals("click")) {
					kvmman.mouseStroke(button);
				}
				if (act.equals("dblclick")) {
					kvmman.mouseStroke(button);
					kvmman.mouseStroke(button);
				}
				writeToGroup(SocketServerMulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));

				break;
			}
			case "/getTile": {
				int x = Integer.parseInt(req.getParam("x"));
				int y = Integer.parseInt(req.getParam("y"));
				String mimeType = "image/" + TILEEXT;
				TileSerializer tile = tileman.getTile(x, y);
				synchronized (tile) {
					res.dataStream(HTTP_OK, mimeType, tile.getData());
					writeTo(key, ByteBuffer.wrap(res.getResponse()));
					tile.clearDirty();
				}
				break;
			}
			case "/remotedesktop.html": {
				updateConfig("quality", req.getParam("quality"));
				updateConfig("fps", req.getParam("fps"));
				writeToGroup(SocketServerMulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));

				StringBuffer sb = new StringBuffer();
				String host = req.getHeader("host");
				sb.append("<table cellspacing='0' cellpadding='0'>");
				for (int y = 0; y < tileman.getNumYTile(); y++) {
					sb.append("<tr>\n");
					for (int x = 0; x < tileman.getNumXTile(); x++) {
						sb.append("<td>");
						sb.append("<img src='getTile?x=");
						sb.append(x);
						sb.append("&y=");
						sb.append(y);
						sb.append("&c=");
						sb.append(0);
						sb.append("' id='i");
						sb.append(x + "_" + y);
						sb.append("' height='" + tileman.getTile(x, y).getHeight() + "' width='"
								+ tileman.getTile(x, y).getWidth() + "' ></td>\n");

					}
					sb.append("</tr>");
				}
				sb.append("</table>");

				String text = new String(getFileContent(path));

				text = text.replaceFirst("<DYNAMICTEXT>", sb.toString());
				text = text.replaceFirst("<TIMEOUT>", Integer.toString(500));
				text = text.replaceAll("<REMOTEDESKTOPHOST>", host);
				text = text.replaceAll("<REMOTEDESKTOPUPDATEMOUSEDELAY>", String.valueOf((int) (1000 / Config.fps)));
				res.message(HTTP_OK, "text/html", text);
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
				break;
			}
			case "/ajaxvnc.js": {
				byte[] b = getFileContent(path);
				res.message(HTTP_OK, "text/javascript", new String(b));
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
				break;
			}
			case "/favicon.ico": {
				byte[] b = getFileContent(path);
				res.message(HTTP_OK, "image/vnd", new String(b));
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
				break;

			}
			default: {
				res.exception(HTTP_NOTFOUND, path + " not found");
				break;
			}
			}
		} while (remaining > 0);
	}

	private void updateConfig(String key, String val) {
		if (val != null && !val.equals(updateableConfigs.get(key))) {
			updateableConfigs.put(key, val);
			kvmman.updateConfig(key, val);

		}
	}

	private byte[] getFileContent(String fn) throws IOException {
		InputStream in = getClass().getResourceAsStream("/META-INF/resources" + fn);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		for (int c = 0; (c = in.read(buf, c, buf.length - c)) >= 0;) {
			out.write(buf, 0, c);
		}
		return out.toByteArray();
	}

	public void cancelKey(SelectionKey key) {
		logger.fine("HttpServer: cancel key for: " + key.channel() + " " + key.attachment());
		key.cancel();
	}

}
