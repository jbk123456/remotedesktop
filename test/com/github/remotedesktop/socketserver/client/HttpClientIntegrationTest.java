package com.github.remotedesktop.socketserver.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.github.remotedesktop.socketserver.HttpServer;
import com.github.remotedesktop.socketserver.SocketServerBuilder;
import com.github.remotedesktop.socketserver.http.HttpClientTestAdapter;
import com.github.remotedesktop.socketserver.http.ResponseHandler;
import com.github.remotedesktop.socketserver.plugin.websocket.WebSocketEncoderDecoder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HttpClientIntegrationTest {

	static final Logger logger = Logger.getLogger(HttpClientIntegrationTest.class.getName());
	private String address = "localhost";
	private HttpClientTestAdapter displayServerClient;
	private HttpServer server;
	private HttpClientTestAdapter httpBrowserClient;
	private HttpClientTestAdapter httpBrowserClient2;
	private WebSocketEncoderDecoder encoder = new WebSocketEncoderDecoder();
	private byte[] img1;
	private byte[] img2;

	@After
	public void cleanUp() throws Exception {
		displayServerClient.stop();
		httpBrowserClient.stop();
		if (httpBrowserClient2 != null) {
			httpBrowserClient2.stop();
		}
		server.stop();
	}

	@Test(timeout = 2000)
	public void sendTiles_expect_WebServerConstructsTiledoc() throws Exception {

		server = new SocketServerBuilder<>(new HttpServer()).withHost("localhost").withPort(0).build();
		server.start();
		int port = server.getPort();
		assertNotEquals("port", port, 0);
		List<byte[]> results = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(4);
		final CountDownLatch browserLatch = new CountDownLatch(2);

		displayServerClient = new HttpClientTestAdapter("displayServerClient", address, port, new ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) throws IOException {
				Assert.fail();
			}
		});
		displayServerClient.start();

		httpBrowserClient = new HttpClientTestAdapter("httpBrowserClient", address, port, new ResponseHandler() {
			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				results.add(message);
				browserLatch.countDown();
				latch.countDown();
			}
		});
		httpBrowserClient.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		// put client2 in Multicast group "RECEIVER"
		httpBrowserClient.sendMessage(getWSUpgradeMessage());
		avoidPackageMerging();

		// write two images and expect that client2 receives a document containing
		// these images
		img1 = writeImageFromDisplayServerToHttpServer("Tux.png", 0, 0);
		img2 = writeImageFromDisplayServerToHttpServer("Debian_logo.png", 1, 1);
		writeImageFromDisplayServerToHttpServerFinish();

		// wait until the browser has received the document
		browserLatch.await();
		browserFetchImage(0, 0);
		avoidPackageMerging();
		browserFetchImage(1, 1);
		latch.await();

		checkResults(results);
	}

	@Test(timeout = 2000)
	public void sendTiles_expect_allClientsReceiveTiles() throws Exception {
		server = new SocketServerBuilder<>(new HttpServer()).withHost("localhost").withPort(0).build();
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(3);

		displayServerClient = new HttpClientTestAdapter("client one", address, port);
		displayServerClient.setResponseHandler(new ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) throws IOException {
				Assert.fail();
			}
		});
		displayServerClient.start();
		httpBrowserClient = new HttpClientTestAdapter("client two", address, port, new ResponseHandler() {
			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				latch.countDown();
			}
		});
		httpBrowserClient.start();
		httpBrowserClient2 = new HttpClientTestAdapter("client three", address, port, new ResponseHandler() {
			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				latch.countDown();
			}
		});
		httpBrowserClient2.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		// put client2 in Multicast group "RECEIVER"
		httpBrowserClient.sendMessage(getWSUpgradeMessage());
		httpBrowserClient2.sendMessage(getWSUpgradeMessage());
		avoidPackageMerging(); // avoid package merging, as we count the received packed

		// write two images and expect that client2 receives a document containing
		// these images
		writeImageFromDisplayServerToHttpServer("Tux.png", 0, 0);
		writeImageFromDisplayServerToHttpServerFinish();

		latch.await();

	}

	@Test(timeout = 2000)
	public void sendTilesAndInput_expect_serverReceivesAllInput() throws Exception {
		server = new SocketServerBuilder<>(new HttpServer()).withHost("localhost").withPort(0).build();
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(6);
		final CountDownLatch browserLatch = new CountDownLatch(4);

		displayServerClient = new HttpClientTestAdapter("client one", address, port);
		displayServerClient.setResponseHandler(new ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) throws IOException {
				assertTrue(new String(message).contains("/k")); // send keys
				assertEquals(25, message.length);
				latch.countDown();
			}
		});
		displayServerClient.start();
		httpBrowserClient = new HttpClientTestAdapter("client two", address, port, new ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				latch.countDown();
				browserLatch.countDown();
			}
		});
		httpBrowserClient.start();
		httpBrowserClient2 = new HttpClientTestAdapter("client three", address, port, new ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				latch.countDown();
				browserLatch.countDown();
			}
		});
		httpBrowserClient2.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		// put client2 in Multicast group "RECEIVER"
		httpBrowserClient.sendMessage(getWSUpgradeMessage());
		httpBrowserClient2.sendMessage(getWSUpgradeMessage());
		avoidPackageMerging(); // avoid package merging, as we count the received packed

		// write two images and expect that client2 receives a document containing
		// these images
		writeImageFromDisplayServerToHttpServer("Tux.png", 0, 0);
		writeImageFromDisplayServerToHttpServerFinish();
		browserLatch.await();

		browserSendKeyEvent(httpBrowserClient2);
		avoidPackageMerging(); // avoid package merging, as we count the received packed
		browserSendKeyEvent(httpBrowserClient);
		latch.await();

	}

	private void avoidPackageMerging() throws InterruptedException {
		Thread.sleep(200);
	}

	private void browserSendKeyEvent(HttpClientTestAdapter client) throws IOException {
		String message = "GET /sendKey?key=0&code=0&mask=0&c=" + Math.random() + "\r\n\r\n";
		client.sendMessage(encoder.encodeFrame(message));

	}

	private byte[] getWSUpgradeMessage() {
		return ("GET /sendKey?key=0&mask=0&c=0 HTTP/1.1\r\n" + "Connection: upgrade\r\n" + "Sec-WebSocket-Key: key\r\n"
				+ "Upgrade: foo/1, bar/2\r\n\r\n").getBytes();
	}

	private void writeImageFromDisplayServerToHttpServerFinish() {
		StringBuilder b = new StringBuilder("GET /tiledoc");
		b.append("\r\n\r\n");
		byte[] document = b.toString().getBytes();
		try {
			logger.fine("displayserver: update tileDoc called");
			displayServerClient.writeToServer(ByteBuffer.wrap(document));
		} catch (IOException e) {
			logger.log(Level.SEVERE, "write image", e);
		}
	}

	private byte[] writeImageFromDisplayServerToHttpServer(String name, int x, int y) throws IOException {
		int width = 96;
		int height = 96;
		byte[] tuxTile = getFileContent(name);
		StringBuilder b = new StringBuilder("PUT /tile?");
		b.append("x=");
		b.append(x);
		b.append("&y=");
		b.append(y);
		b.append("&w=");
		b.append(width);
		b.append("&h=");
		b.append(height);
		b.append("\r\n");
		b.append("Content-Length:");
		b.append(+tuxTile.length);
		b.append("\r\n\r\n");
		byte[] req = b.toString().getBytes();
		byte[] document = new byte[req.length + tuxTile.length];

		System.arraycopy(req, 0, document, 0, req.length);
		System.arraycopy(tuxTile, 0, document, req.length, tuxTile.length);
		logger.fine("displayserver: update tile called");
		displayServerClient.writeToServerBuffer(ByteBuffer.wrap(document));
		return document;
	}

	private byte[] getFileContent(String fn) throws IOException {
		InputStream in = getClass().getResourceAsStream(fn);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		for (int c = 0; (c = in.read(buf, c, buf.length - c)) >= 0;) {
			out.write(buf, 0, c);
		}
		return out.toByteArray();
	}

	private boolean isWebSocketResponse(byte[] message) {
		boolean res = (message[0] == 'H' && new String(message).contains("Upgrade: websocket"));
		return res;
	}

	private boolean isTileDoc(byte[] message) throws IOException {
		boolean res = (message[0] == -127 && new String(encoder.decodeFrames(message)).contains("getTile"));
		return res;
	}

	private boolean isImage(byte[] img1, byte[] message) {
		boolean res = (message[0] == 'H' && Arrays.equals(getImageContent(img1), getImageContent(message)));
		return res;
	}

	private byte[] getImageContent(byte[] message) {
		int n = 0;
		while (message[n] != '\r' || message[n + 1] != '\n' || message[n + 2] != '\r' || message[n + 3] != '\n') {
			n++;
		}
		byte[] result = new byte[message.length - n - 4];
		System.arraycopy(message, n + 4, result, 0, message.length - n - 4);
		return result;
	}

	private void checkResults(List<byte[]> results) throws IOException {
		assertEquals(4, results.size());
		for (byte[] result : results) {
			assertTrue(
					isWebSocketResponse(result) || isTileDoc(result) || isImage(img1, result) || isImage(img2, result));
		}

	}

	private void browserFetchImage(int x, int y) throws IOException {
		httpBrowserClient.sendMessage(encoder.encodeFrame("GET /getTile?x=" + x + "&y=" + y + "\r\n\r\n"));
	}
}
