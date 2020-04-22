package com.github.remotedesktop.socketserver.client;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.http.HttpTestClient;
import com.github.remotedesktop.socketserver.service.http.HttpClient;
import com.github.remotedesktop.socketserver.service.http.HttpServer;
import com.github.remotedesktop.socketserver.service.http.WebSocketEncoderDecoder;


public class HttpClientIntegrationTest {

	private String address = "localhost";
	private HttpServer server;
	private HttpClient displayServerClient;
	private HttpTestClient httpBrowserClient;
	private HttpTestClient httpBrowserClient2;
	WebSocketEncoderDecoder encoder = new WebSocketEncoderDecoder();
	
	@After
	public void cleanUp() throws Exception {
		displayServerClient.stop();
		httpBrowserClient.stop();
		server.stop();
	}

	@Test(timeout = 2000)
	public void sendTiles_expect_WebServerConstructsTiledoc() throws Exception {
		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(2);

		displayServerClient = new HttpClient("client one", address, port);
		displayServerClient.setResponseHandler(new HttpClient.ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) throws IOException {
				Assert.fail();
			}
		});
		displayServerClient.start();
		httpBrowserClient = new HttpTestClient("client two", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		httpBrowserClient.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		// put client2 in Multicast group "RECEIVER"
		httpBrowserClient.sendMessage(getWSUpgradeMessage());

		// write two images and expect that client2 receives a document containing
		// these images

		writeImageFromDisplayServerToHttpServer("Tux.png");
		writeImageFromDisplayServerToHttpServer("Debian_logo.png");

		latch.await();

	}

	@Test(timeout = 2000)
	public void sendTiles_expect_allClientsReceiveTiles() throws Exception {
		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(3);

		displayServerClient = new HttpClient("client one", address, port);
		displayServerClient.setResponseHandler(new HttpClient.ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) throws IOException {
				Assert.fail();
			}
		});
		displayServerClient.start();
		httpBrowserClient = new HttpTestClient("client two", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		httpBrowserClient.start();
		httpBrowserClient2 = new HttpTestClient("client three", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		httpBrowserClient2.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		// put client2 in Multicast group "RECEIVER"
		httpBrowserClient.sendMessage(getWSUpgradeMessage());

		// write two images and expect that client2 receives a document containing
		// these images

		writeImageFromDisplayServerToHttpServer("Tux.png");
		writeImageFromDisplayServerToHttpServer("Debian_logo.png");

		latch.await();

	}
	@Test(timeout = 2000)
	public void sendTilesAndInput_expect_serverReceivesAllInput() throws Exception {
		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(5);

		displayServerClient = new HttpClient("client one", address, port);
		displayServerClient.setResponseHandler(new HttpClient.ResponseHandler() {

			@Override
			public void onMessage(SelectionKey key, byte[] message) throws IOException {
				System.out.println("got message:: " + encoder.decodeFrames(message));
				latch.countDown();
			}
		});
		displayServerClient.start();
		httpBrowserClient = new HttpTestClient("client two", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		httpBrowserClient.start();
		httpBrowserClient2 = new HttpTestClient("client three", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				latch.countDown();
			}
		});
		httpBrowserClient2.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		// put client2 in Multicast group "RECEIVER"
		httpBrowserClient.sendMessage(getWSUpgradeMessage());
		httpBrowserClient2.sendMessage(getWSUpgradeMessage());
		Thread.sleep(500);

		// write two images and expect that client2 receives a document containing
		// these images

		//writeImageFromDisplayServerToHttpServer("Tux.png");
		//writeImageFromDisplayServerToHttpServer("Debian_logo.png");

		sendKeyEvent(httpBrowserClient2);
		sendKeyEvent(httpBrowserClient);
		latch.await();

	}

	private void sendKeyEvent(HttpTestClient client) throws IOException {
		String message = "GET /sendKey?key=0&mask=0&c=" + Math.random() + "\r\n\r\n";
		client.sendMessage(message);
		
	}

	private String getWSUpgradeMessage() {
		return "GET /sendKey?key=0&mask=0&c=0 HTTP/1.1\r\n" + 
				"Connection: upgrade\r\n" + 
				"Sec-WebSocket-Key: key\r\n"+
				"Upgrade: foo/1, bar/2\r\n\r\n".getBytes();
	}

	private void writeImageFromDisplayServerToHttpServer(String name) throws IOException {
		int x=0;
		int y=0;
		int width=96;
		int height=96;
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
		byte[] data = new byte[req.length + tuxTile.length];

		System.arraycopy(req, 0, data, 0, req.length);
		System.arraycopy(tuxTile, 0, data, req.length, tuxTile.length);
		try {
			System.out.println("displayserver: update tile called");;
			displayServerClient.writeToServer(ByteBuffer.wrap(data));
		} catch (IOException e) {
			e.printStackTrace();
		}
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

}
