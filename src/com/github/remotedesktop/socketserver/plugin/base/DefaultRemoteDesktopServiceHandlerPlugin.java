package com.github.remotedesktop.socketserver.plugin.base;

import static com.github.remotedesktop.socketserver.Attachment.getMulticastGroup;
import static com.github.remotedesktop.socketserver.Attachment.setMulticastGroup;
import static com.github.remotedesktop.socketserver.Group.SERVERS;
import static com.github.remotedesktop.socketserver.HttpServer.HTTP_OK;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.github.remotedesktop.Config;
import com.github.remotedesktop.socketserver.Group;
import com.github.remotedesktop.socketserver.HttpServer;
import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;
import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.plugin.websocket.WebSocketEncoderDecoder;

public class DefaultRemoteDesktopServiceHandlerPlugin<T extends SocketServer> implements ServiceHandlerPlugin {

	static final Logger LOGGER = Logger.getLogger(DefaultRemoteDesktopServiceHandlerPlugin.class.getName());

	public static final String TILES = getCanvasTiles();
	public static final String TILEEXT = "JPG";
	private static final String TILE_MIME_TYPE = "image/" + TILEEXT;

	private final WebSocketEncoderDecoder websocketProtocolParser = new WebSocketEncoderDecoder();

	private final KeyboardAndMouseSerializer kvmman;

	private T server;
	private static final String GET_CMD = "GET /";
	private static final String HEADER_SEPARATOR = "\r\n\r\n";

	public DefaultRemoteDesktopServiceHandlerPlugin(T server) throws IOException {
		kvmman = new KeyboardAndMouseSerializer(GET_CMD, HEADER_SEPARATOR);
		this.server = server;
	}

	private static final String getCanvasTiles() {
		StringBuilder b = new StringBuilder();
		for (int y = 0; y < Config.max_tile; y++) {
			b.append("<div class=\"flex-container\">");
			for (int x = 0; x < Config.max_tile; x++) {
				b.append("<img class=\"flex-item\" id=\"c");
				b.append(x);
				b.append("_");
				b.append(y);
				b.append("\">");
				b.append("</img>");
			}
			b.append("</div>");
		}
		return b.toString();
	}

	@Override
	public boolean service(Request req, Response res) throws IOException {
		String path = req.getURI().getPath();
		switch (path) {

		case "/": {
			String host = req.getHeader("host");

			String text = new String(HttpServer.getFileContent(path + "index.html"));

			text = text.replaceFirst("<DYNAMICTEXT>", TILES);
			text = text.replaceAll("<REMOTEDESKTOPHOST>", host);
			text = text.replaceAll("<REMOTEDESKTOPUPDATEMOUSEDELAY>", String.valueOf((int) (1000 / Config.fps)));
			res.message(HTTP_OK, "text/html", text);

			return true;
		}
		case "/js/input.js":
		case "/js/util.js":
		case "/js/main.js": {
			byte[] b = HttpServer.getFileContent(path);
			res.message(HTTP_OK, "text/javascript", new String(b));
			return true;
		}
		case "/css/main.css": {
			byte[] b = HttpServer.getFileContent(path);
			res.message(HTTP_OK, "text/css", new String(b));
			return true;
		}
		case "/favicon.ico": {
			byte[] b = HttpServer.getFileContent(path);
			res.message(HTTP_OK, "image/vnd", new String(b));
			return true;
		}
		case "/tiledoc": { // tiles prrocessed, write back document containing the links to the images
//			StringBuilder sb = new StringBuilder();
//			sb.append("document.getElementById(\"canvas\").style.cursor=\"");
//			sb.append(req.getParam("cursor"));
//			sb.append("\";");
//			if (sb.length() > 0) {
//				byte[] getImagesData = websocketProtocolParser.encodeFrame(sb.toString());
//				server.writeToGroup(Group.BROWSERS, ByteBuffer.wrap(getImagesData));
//				LOGGER.finest(
//						String.format("SendDocument: %s", sb.toString()));
//			}
			return true;
		}
		case "/tile": { // tile prrocessed,
			if (getMulticastGroup(req.getKey()) == null) {
				setMulticastGroup(req.getKey(), Group.SERVERS);
				LOGGER.fine(
						String.format("DisplayServer connected: %s (%s) ", req.getKey(), req.getKey().attachment()));
			}

			int x = Integer.parseInt(req.getParam("x"));
			int y = Integer.parseInt(req.getParam("y"));
			int w = Integer.parseInt(req.getParam("w"));
			int h = Integer.parseInt(req.getParam("h"));

			StringBuilder b = new StringBuilder("PUT /tile?");
			b.append("x=");
			b.append(x);
			b.append("&y=");
			b.append(y);
			b.append("&w=");
			b.append(w);
			b.append("&h=");
			b.append(h);
			b.append("\r\n");
			b.append("Content-Length:");
			b.append(req.getData().length);
			b.append("\r\n\r\n");
			byte[] data = websocketProtocolParser.encodeFrame(b.toString());
			res.dataStream(HTTP_OK, TILE_MIME_TYPE, data);
			LOGGER.finest(String.format("SendTile: %d (%d, %d, %d, %d)", data.length, x, y, w, h));
			server.writeTo(req.getKey(), ByteBuffer.wrap(res.getResponse()));
			return true;
		}

		case "/sendKey": {
			kvmman.keyStroke(Integer.parseInt(req.getParam("key")), Integer.parseInt(req.getParam("code")),
					Integer.parseInt(req.getParam("mask")));
			server.writeToGroup(SERVERS, ByteBuffer.wrap(kvmman.getBytes()));
			return true;
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
			server.writeToGroup(SERVERS, ByteBuffer.wrap(kvmman.getBytes()));

			return true;
		}
		}
		return false;
	}
}
