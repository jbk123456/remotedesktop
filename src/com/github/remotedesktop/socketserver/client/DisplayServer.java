package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;
import com.github.remotedesktop.socketserver.plugin.base.ServiceHandlerPlugin;
import com.github.remotedesktop.socketserver.plugin.webrtc.SignalDataChannelCreated;

import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelObserver;

public class DisplayServer extends SocketServerClient
		implements TileOperations, ServiceHandlerPlugin, RTCDataChannelObserver, SignalDataChannelCreated {

	static final Logger LOGGER = Logger.getLogger(DisplayServer.class.getName());

	private static interface DataHandler {
		public void writeToServer(ByteBuffer buffer) throws IOException;
	};
	private KVMManager kvmman;
	private TileManager tileman;
	private ScreenScanner scanner;
	private KeepAlive keepalive;

	private RTCDataChannel datachannel;
	private final DataHandler defaultDataHandler = (buffer) -> this.writeToServer(buffer);
	private final DataHandler rtcDataHandler = (buffer) -> this.writeToRTCServer(buffer);
	private DataHandler dataHandler = defaultDataHandler;
	
	public DisplayServer() throws IOException, AWTException {
		super();
		kvmman = KVMManager.getInstance();
		tileman = new TileManager();
		scanner = new ScreenScanner(kvmman, tileman, this);
		keepalive = new KeepAlive(kvmman);
	}

	public void writeToRTCServer(ByteBuffer buffer) throws IOException {
		try {
			datachannel.send(new RTCDataChannelBuffer(buffer, true));
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public void startDisplayServer() {
		LOGGER.info("display server start called");
		tileman.startRenderPool();
		scanner.startScreenScanning();
		keepalive.startKeepAlive();
		super.start();
	}

	public void stop() {
		LOGGER.info("display server stop called");
		tileman.stop();
		scanner.stop();
		keepalive.stop();
		super.stop();
	}

	@Override
	public void updateTile(int x, int y) throws IOException {
		Tile tile = tileman.getTile(x, y);
		StringBuilder b = new StringBuilder("PUT /tile?");
		b.append("x=");
		b.append(x);
		b.append("&y=");
		b.append(y);
		b.append("&w=");
		b.append(tile.getWidth());
		b.append("&h=");
		b.append(tile.getHeight());
		b.append("\r\n");
		b.append("Content-Length:");
		b.append(+tile.getData().length);
		b.append("\r\n\r\n");
		byte[] req = b.toString().getBytes();
		byte[] data = new byte[req.length + tile.getData().length];

		System.arraycopy(req, 0, data, 0, req.length);
		System.arraycopy(tile.getData(), 0, data, req.length, tile.getData().length);
	//	writeToServer(ByteBuffer.wrap(data));
		dataHandler.writeToServer(ByteBuffer.wrap(data));
	}

	@Override
	public void updateScreen(String cursor) throws IOException {
		StringBuilder b = new StringBuilder("GET /tiledoc?cursor=");
		b.append(cursor);
		b.append("\r\n\r\n");
		byte[] req = b.toString().getBytes();

		writeToServer(ByteBuffer.wrap(req));
	}

	@Override
	public boolean service(Request req, Response res) throws IOException {
		String path = req.getURI().getPath();

		switch (path) {
		case "/k": {
			kvmman.keyStroke(Integer.parseInt(req.getParam("k")), Integer.parseInt(req.getParam("v")),
					Integer.parseInt(req.getParam("mask")));
			return true;
		}
		case "/m": {
			kvmman.mouseMove(Integer.parseInt(req.getParam("x")), Integer.parseInt(req.getParam("y")));
			return true;
		}
		case "/p": {
			kvmman.mousePress(Integer.parseInt(req.getParam("v")));
			return true;
		}
		case "/r": {
			kvmman.mouseRelease(Integer.parseInt(req.getParam("v")));
			return true;
		}
		}
		return false;
	}

	@Override
	public void onCreated(RTCDataChannel datachannel) {
		LOGGER.info("RTC channel has been created!");
		this.datachannel = datachannel;
	}

	@Override
	public void onBufferedAmountChange(long previousAmount) {
		LOGGER.info("onBufferedAmountChange");

	}

	@Override
	public void onStateChange() {
		switch(datachannel.getState()) {
		case OPEN: 
			LOGGER.info("switching to rtc data handler");
			this.dataHandler = rtcDataHandler;
			break;
		case CLOSING:
		case CLOSED:
			LOGGER.info("switching to default data handler");
			this.dataHandler = defaultDataHandler;
			break;
		default:
			break;
		}

	}

	@Override
	public void onMessage(RTCDataChannelBuffer buffer) {
		LOGGER.info("rtc data received");

	}

}
