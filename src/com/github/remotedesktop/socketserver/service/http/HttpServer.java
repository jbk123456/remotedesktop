package com.github.remotedesktop.socketserver.service.http;

import static java.lang.System.arraycopy;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.remotedesktop.Config;
import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.service.KeyboardAndMouseSerializer;
import com.github.remotedesktop.socketserver.service.TileSerializer;
import com.github.remotedesktop.socketserver.service.TileSerializationManaer;

public class HttpServer extends SocketServer {
	public static final String HTTP_OK = "200 OK";
	public static final String HTTP_NOTFOUND = "404 Not Found";
	public static final String HTTP_FORBIDDEN = "403 Forbidden";
	public static final String HTTP_INTERNALERROR = "500 Internal Server Error";
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
	public static final String TILEEXT = "JPG";
	private static final byte[] NO_WEBSOCKET_REQUEST = new byte[0];

	private final TileSerializationManaer tileman;
	private final KeyboardAndMouseSerializer kvmman;
	private final WebSocketEncoderDecoder websocketProtocolParser;

	public HttpServer(String hostname, int port) throws IOException {
		super("HttpServer", hostname, port);
		kvmman = new KeyboardAndMouseSerializer();
		tileman = new TileSerializationManaer();
		websocketProtocolParser = new WebSocketEncoderDecoder();
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
		handle(key, data);
	}

	private byte[] getWebsocketData(SelectionKey key, byte inputData[]) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(inputData);
		Scanner s = new Scanner(in, "UTF-8");
		try {
			String upgradeData = s.useDelimiter("\\r\\n\\r\\n").next();
			Matcher get = Pattern.compile("^GET").matcher(upgradeData);
			if (get.find()) {
				Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(upgradeData);
				boolean find = match.find();
				if (!find) {
					return NO_WEBSOCKET_REQUEST;
				}
				byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
						+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: "
						+ Base64.getEncoder()
								.encodeToString(MessageDigest.getInstance("SHA-1").digest(
										(match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
						+ "\r\n\r\n").getBytes("UTF-8");
				setDataBuffer(key, ByteBuffer.wrap(response));
				if (getMulticastGroup(key)==null) {
					setDebugContext(key, "websocket request");
					setMulticastGroup(key, MulticastGroup.RECEIVER);
					System.out.println("Browser connected: " + key.attachment());
				}
				write(key);
				return null;
			} else if (getMulticastGroup(key) == MulticastGroup.RECEIVER) {
				return websocketProtocolParser.decodeFrames(inputData);
			}
		} catch (NoSuchAlgorithmException e) {
			// ignore
		} finally {
			close(s);
		}

		return NO_WEBSOCKET_REQUEST;
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
				assert (getMulticastGroup(key) != MulticastGroup.RECEIVER);
				setDataBuffer(key, ByteBuffer.wrap(data));
				return; // partial read, get the rest
			}
			Response res = new Response();
			String path = req.getURI().getPath();
			setDebugContext(key, path);
			switch (path) {
			
			case "/tiledoc": { // tiles prrocessed, write back document containing the links to the images
				StringBuilder sb = new StringBuilder();
				Random r = new Random(System.currentTimeMillis());
				for (int i = 0; i < tileman.getNumXTile(); i++) {
					for (int j = 0; j < tileman.getNumYTile(); j++) {
						if (tileman.getTile(i, j).isDirty()) {
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
						writeToGroup(MulticastGroup.RECEIVER, ByteBuffer.wrap(getImagesData));
				}
				break;
			}
			
			case "/tile": { // tile prrocessed, 
				if (getMulticastGroup(key)==null) {
					setMulticastGroup(key, MulticastGroup.SENDER);
					System.out.println("DisplayServer connected: " + key.attachment());
				}
				
				tileman.processImage(req.getData(), Integer.parseInt(req.getParam("x")),
						Integer.parseInt(req.getParam("y")), Integer.parseInt(req.getParam("w")),
						Integer.parseInt(req.getParam("h")));

				break;
			}

			case "/": {
				res.redirect("/remotedesktop.html");
				try {
					writeTo(key, ByteBuffer.wrap(res.getResponse()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
			case "/sendKey": {
//				System.out.println("httpserver: ctrl data received: /sendkey");
				kvmman.keyStroke(Integer.parseInt(req.getParam("key")),Integer.parseInt(req.getParam("code")), Integer.parseInt(req.getParam("mask")));
				try {
					writeToGroup(MulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));
				} catch (Exception e) {
					e.printStackTrace();
				}
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
				try {
					writeToGroup(MulticastGroup.SENDER, ByteBuffer.wrap(kvmman.getBytes()));
				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
			}
			case "/getTile": {
				int x = Integer.parseInt(req.getParam("x"));
				int y = Integer.parseInt(req.getParam("y"));
				TileSerializer tile = tileman.getTile(x, y);
				synchronized (tile) {
					String mimeType = "image/" + TILEEXT;
					tile.clearDirty();
					res.dataStream(HTTP_OK, mimeType, tile.getData());
				}
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
				break;
			}
			case "/remotedesktop.html": {
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
				text = text.replaceAll("<REMOTEDESKTOPUPDATEMOUSEDELAY>", String.valueOf((int)(1000/Config.fps)));
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
		  System.out.println("HttpServer: cancel key for: " + key.channel()  + " " +key.attachment());
	      key.cancel();
	   }

	@Override
	protected void select() throws IOException {
		selector.select();
	}
}
