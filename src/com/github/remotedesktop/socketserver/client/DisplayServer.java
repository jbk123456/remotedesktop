package com.github.remotedesktop.socketserver.client;

import static com.github.remotedesktop.socketserver.SocketServerAttachment.setDebugContext;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.setPushBackBuffer;
import static java.nio.channels.SelectionKey.OP_READ;

import java.awt.AWTException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.SocketServerClient;
import com.github.remotedesktop.socketserver.service.http.HttpServer;
import com.github.remotedesktop.socketserver.service.http.Request;
import com.github.remotedesktop.socketserver.service.http.Response;

public class DisplayServer extends SocketServerClient implements ResponseHandler, TileOperations {

	private static final Logger logger = Logger.getLogger(DisplayServer.class.getName());

	private KVMManager kvmman;
	private TileManager tileman;
	private ScreenScanner scanner;
	private KeepAlive keepalive;
	private LockScreen lockscreen;

	public DisplayServer(String id, String hostname, int port) throws IOException, AWTException {
		super(id, hostname, port);

		kvmman = KVMManager.getInstance();
		tileman = new TileManager();
		scanner = new ScreenScanner(kvmman, tileman, this);
		keepalive = new KeepAlive(kvmman);
		lockscreen = new LockScreen(kvmman);

		setResponseHandler(this);
	}

	public void startDisplayServer() {
		logger.info("display server start called");
		tileman.startRenderPool();
		scanner.startScreenScanning();
		keepalive.startKeepAlive();
		lockscreen.startLockScreen();
		super.start();
	}

	public void stop() {
		logger.info("display server stop called");
		tileman.stop();
		scanner.stop();
		keepalive.stop();
		lockscreen.stop();
		super.stop();
	}

	@Override
	public void updateTile(int x, int y) {
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
		writeToServerBuffer(ByteBuffer.wrap(data));
	}

	@Override
	public void updateTileFinish(String cursor) {
		StringBuilder b = new StringBuilder("GET /tiledoc?cursor=");
		b.append(cursor);
		b.append("\r\n\r\n");
		byte[] req = b.toString().getBytes();

		try {
			writeToServer(ByteBuffer.wrap(req));
		} catch (IOException e) {
			tileman.setDirty();
		}
	}

	@Override
	public void onMessage(SelectionKey key, byte[] data) throws IOException {
		Request req = null;
		int remaining = 0;
		do {
			if (remaining > 0) {

				byte[] newdata = new byte[remaining];
				System.arraycopy(data, data.length - remaining, newdata, 0, remaining);
				data = newdata;
			}

			req = new Request(key);
			remaining = req.parse(data);
			if (remaining < 0) {
				logger.fine("could not read data yet, sleeping...");
				setPushBackBuffer(key, ByteBuffer.wrap(data));
				key.interestOps(OP_READ);
				return; // partial read, get the rest
			}
			Response res = new Response();

			try {
				setDebugContext(key, req.getURI().getPath());
				handle(req, res);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "on message", e);
				cancelKey(key);
			}
		} while (remaining > 0);

	}

	private void handle(Request req, Response res) throws IOException {
		String path = req.getURI().getPath();

		lockscreen.hideLockScreen();
		
		switch (path) {
		case "/k": {
			kvmman.keyStroke(Integer.parseInt(req.getParam("k")), Integer.parseInt(req.getParam("v")),
					Integer.parseInt(req.getParam("mask")));
			break;
		}
		case "/m": {
			kvmman.mouseMove(Integer.parseInt(req.getParam("x")), Integer.parseInt(req.getParam("y")));
			break;
		}
		case "/p": {
			kvmman.mousePress(Integer.parseInt(req.getParam("v")));
			break;
		}
		case "/r": {
			kvmman.mouseRelease(Integer.parseInt(req.getParam("v")));
			break;
		}
		case "/u": {
			updateConfig(req.getParam("k"), req.getParam("v"));
			break;
		}
		default: {
			res.exception(HttpServer.HTTP_NOTFOUND, path + " not found");
			break;
		}

		}
	}

	private void updateConfig(String key, String value) {
		switch (key) {
		case "fps":
			float fps = Float.parseFloat(value);
			scanner.updateFps(fps);
			break;
		case "quality":
			float quality = Float.parseFloat(value);
			tileman.updateQuality(quality);
			break;
		default:
			throw new IllegalArgumentException(key);
		}
	}
}
