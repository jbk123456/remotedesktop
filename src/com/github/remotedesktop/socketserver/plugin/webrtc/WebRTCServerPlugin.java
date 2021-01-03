package com.github.remotedesktop.socketserver.plugin.webrtc;

import static com.github.remotedesktop.socketserver.Attachment.getMulticastGroup;
import static com.github.remotedesktop.socketserver.Attachment.setMulticastGroup;
import static com.github.remotedesktop.socketserver.HttpServer.HTTP_OK;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.logging.Logger;

import com.github.remotedesktop.Config;
import com.github.remotedesktop.socketserver.Group;
import com.github.remotedesktop.socketserver.HttpServer;
import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;
import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.plugin.base.DefaultRemoteDesktopServiceHandlerPlugin;
import com.github.remotedesktop.socketserver.plugin.base.ServiceHandlerPlugin;
import com.github.remotedesktop.socketserver.plugin.websocket.WebSocketEncoderDecoder;

public class WebRTCServerPlugin<T extends SocketServer> implements ServiceHandlerPlugin {
	static final Logger LOGGER = Logger.getLogger(WebRTCServerPlugin.class.getName());

	private static final Decoder BASE64_DECODER = Base64.getDecoder();

	private final WebSocketEncoderDecoder websocketProtocolParser = new WebSocketEncoderDecoder();
	private final T server;

	public WebRTCServerPlugin(T server) {
		this.server = server;
	}

	@Override
	public boolean service(Request req, Response res) throws IOException {

		String path = req.getURI().getPath();
		switch (path) {

		case "/": {
			String host = req.getHeader("host");

			String text = new String(HttpServer.getFileContent(path + "index_webrtc.html"));

			text = text.replaceFirst("<DYNAMICTEXT>", DefaultRemoteDesktopServiceHandlerPlugin.TILES);
			text = text.replaceAll("<REMOTEDESKTOPHOST>", host);
			text = text.replaceAll("<REMOTEDESKTOPUPDATEMOUSEDELAY>", String.valueOf((int) (1000 / Config.fps)));
			res.message(HTTP_OK, "text/html", text);

			return true;
		}
		case "/adapter/adapter-latest.js":
		case "/js/main_webrtc.js": {
			byte[] b = HttpServer.getFileContent(path);
			res.message(HTTP_OK, "text/javascript", new String(b));
			return true;
		}
		case "/webrtcloaded": {
			LOGGER.fine("WebRTC client initialized");
			byte[] data = websocketProtocolParser.encodeFrame("GET /created 1\r\n\r\n");
			server.writeTo(req.getKey(), ByteBuffer.wrap(data));

			return true;
		}
		// FIXME: DisplayServer should send a "connected" message
		case "/tile": { // tile prrocessed,
			if (getMulticastGroup(req.getKey()) == null) {
				setMulticastGroup(req.getKey(), Group.SERVERS);
				LOGGER.fine("DisplayServer connected: " + req.getKey().attachment());
			}
			return true;
		}

		case "/sdp": {
			String sdp = req.getParam("sdp");
			String type = req.getParam("type");
			LOGGER.fine(String.format("RELAY SDP request from browser: %s, %s", new String(BASE64_DECODER.decode(sdp)),
					type));
			String s = String.format("GET /sdp?sdp=%s&type=%s\r\n\r\n", sdp, type);
			server.writeToGroup(Group.SERVERS, ByteBuffer.wrap(s.getBytes()));
			return true;
		}
		case "/ice": { // from browser
			String sdp = req.getParam("candidate");
			String sdpMLineIndex = req.getParam("label");
			String sdpMid = req.getParam("id");
			LOGGER.fine(String.format("RELAY ICE request from browser: %s, %s, %s",
					new String(BASE64_DECODER.decode(sdp)), sdpMid, sdpMLineIndex));
			String s = String.format("GET /ice?candidate=%s&label=%s&id=%s\r\n\r\n", sdp, sdpMLineIndex, sdpMid);
			server.writeToGroup(Group.SERVERS, ByteBuffer.wrap(s.getBytes()));
			return true;
		}
		case "/sdpb": {
			String v = req.getParam("v");
			String s = "GET /sdpb?v=" + v + "\r\n\r\n";
			LOGGER.fine(String.format("RELAY SDP response to browser: %s", new String(BASE64_DECODER.decode(v))));
			byte[] data = websocketProtocolParser.encodeFrame(s);
			server.writeToGroup(Group.BROWSERS, ByteBuffer.wrap(data));
			return true;
		}

		case "/iceb": { // from display server
			String v = req.getParam("v");
			String s = "GET /iceb?v=" + v + "\r\n\r\n";
			LOGGER.fine(String.format("RELAY ICE response to browser: %s", new String(BASE64_DECODER.decode(v))));
			byte[] data = websocketProtocolParser.encodeFrame(s);
			server.writeToGroup(Group.BROWSERS, ByteBuffer.wrap(data));
			return true;
		}
		}
		return false;
	}
}
