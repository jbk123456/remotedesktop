package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import com.github.remotedesktop.socketserver.service.http.HttpClient;
import com.github.remotedesktop.socketserver.service.http.HttpServer;
import com.github.remotedesktop.socketserver.service.http.Request;
import com.github.remotedesktop.socketserver.service.http.Response;

public class DisplayServer extends HttpClient implements HttpClient.ResponseHandler, Tile.Observable {

	private KVMManager kvmman;
	private TileManager tileman;
	private ScreenScanner scanner;

	public DisplayServer(String id, String hostname, int port) throws IOException, AWTException {
		super(id, hostname, port);

		kvmman = new KVMManager();
		tileman = new TileManager();
		scanner = new ScreenScanner(kvmman, tileman, this);
		scanner.startScreenScanning();
		setResponseHandler(this);
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
	public void updateTileFinish() {
		StringBuilder b = new StringBuilder("GET /tiledoc");
		b.append("\r\n\r\n");
		byte[] req = b.toString().getBytes();
		
		boolean written = false;
		try {
			written = writeToServer(ByteBuffer.wrap(req));
		} catch (Exception e) {
			written = false;
		}
		if (!written) {
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
				setDataBuffer(key, ByteBuffer.wrap(data));
				return; // partial read, get the rest
			}
			Response res = new Response();

			try {
				setDebugContext(key, req.getURI().getPath());
				handle(req, res);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (remaining > 0);

	}

	private void handle(Request req, Response res) throws IOException {
		String path = req.getURI().getPath();
//		System.out.println("displayserver: ctrl data received: " + path);

		switch (path) {
		case "/k": {
			kvmman.keyStroke(Integer.parseInt(req.getParam("k")), Integer.parseInt(req.getParam("v")), Integer.parseInt(req.getParam("mask")));
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
		default: {
			res.exception(HttpServer.HTTP_NOTFOUND, path + " not found");
			break;
		}

		}
	}
	public void cancelKey(SelectionKey key) {
	   key.cancel();
	}
}
