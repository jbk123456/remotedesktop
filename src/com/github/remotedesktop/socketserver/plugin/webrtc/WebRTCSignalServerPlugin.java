package com.github.remotedesktop.socketserver.plugin.webrtc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;
import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.plugin.base.ServiceHandlerPlugin;

import dev.onvoid.webrtc.RTCDataChannelObserver;

public class WebRTCSignalServerPlugin<T extends SocketServer> implements ServiceHandlerPlugin, SignalService {
	static final Logger LOGGER = Logger.getLogger(WebRTCSignalServerPlugin.class.getName());

	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final Encoder BASE64_ENCODER = Base64.getEncoder();

	private final StringBuilder descriptionBuilder;
	private final StringBuilder icecandidateBuilder;

	private final T server;
	private final SimpleRTCPeerConnection pc;

	private SelectionKey responseKey;

	public WebRTCSignalServerPlugin(T server, RTCDataChannelObserver dcObserver, SignalDataChannelCreated dcHandler) {
		this.server = server;
		pc = new SimpleRTCPeerConnection(this, dcObserver, dcHandler);
		descriptionBuilder = new StringBuilder();
		icecandidateBuilder = new StringBuilder();
	}

	@Override
	public boolean service(Request req, Response res) throws IOException {
		String path = req.getURI().getPath();
		switch (path) {
		case "/sdp": { // from browser
			String sdp = new String(BASE64_DECODER.decode(req.getParam("sdp")));
			String type = new String(BASE64_DECODER.decode(req.getParam("type")));
			LOGGER.fine(String.format("DisplayServer got SDP request from browser: %s, %s", sdp, type));
			responseKey = req.getKey();
			pc.signalSdpResponse(type, sdp);
			return true;
		}
		case "/ice": { // from browser
			String sdp = new String(BASE64_DECODER.decode(req.getParam("candidate")));
			int sdpMLineIndex = Integer.parseInt(new String(BASE64_DECODER.decode(req.getParam("label"))));
			String sdpMid = new String(BASE64_DECODER.decode(req.getParam("id")));
			LOGGER.fine(String.format("DisplayServer got ICE request from browser: %s, %s, %s", sdp, sdpMid,
					sdpMLineIndex));
			responseKey = req.getKey();
			pc.signalIceResponse(sdpMLineIndex, sdpMid, sdp);
			return true;
		}
		}
		return true;
	}

	@Override
	public void sendIce(int sdpMLineIndex, String sdpMid, String sdp) {
		icecandidateBuilder.setLength(0);
		sdp = sdp.replace("\r", "\\r");
		sdp = sdp.replace("\n", "\\n");

		icecandidateBuilder.append("{ \"sdpMLineIndex\": \"");
		icecandidateBuilder.append(sdpMLineIndex);
		icecandidateBuilder.append("\", \"sdpMid\": \"");
		icecandidateBuilder.append(sdpMid);
		icecandidateBuilder.append("\", \"sdp\": \"");
		icecandidateBuilder.append(sdp);
		icecandidateBuilder.append("\" }");

		String s = String.format("GET /iceb?v=%s\r\n\r\n",
				BASE64_ENCODER.encodeToString(icecandidateBuilder.toString().getBytes()));
		LOGGER.fine(String.format("DisplayServer send ICE to ===> peer: %s", s));
		try {
			server.writeTo(responseKey, ByteBuffer.wrap(s.getBytes()));
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "sendIce", e);
		}

	}

	@Override
	public void sendSdp(String sdpType, String sdp) {
		descriptionBuilder.setLength(0);
		sdp = sdp.replace("\r", "\\r");
		sdp = sdp.replace("\n", "\\n");

		descriptionBuilder.append("{ \"type\": \"");
		descriptionBuilder.append(sdpType);
		descriptionBuilder.append("\", \"sdp\": \"");
		descriptionBuilder.append(sdp);
		descriptionBuilder.append("\" }");

		String s = String.format("GET /sdpb?v=%s\r\n\r\n",
				BASE64_ENCODER.encodeToString(descriptionBuilder.toString().getBytes()));
		LOGGER.fine(String.format("DisplayServer send SDP to ===> peer: %s", s));
		try {
			server.writeTo(responseKey, ByteBuffer.wrap(s.getBytes()));
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "sendSdp", e);
		}
	}
}
