package com.github.remotedesktop.socketserver.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Test;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.service.http.HttpServer;

public class HttpServerIntegrationTest {

	String address = "localhost";
	HttpServer server;
	HttpTestClient client1;

	@After
	public void cleanUp() throws Exception {
		client1.stop();
		server.stop();
	}

	@Test(timeout = 5000)
	public void canHandlePartialMessages() throws Exception {
		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(1);

		client1 = new HttpTestClient("client one", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				assertEquals("message", 13431, message.length());
				latch.countDown();
			}
		});
		client1.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		StringBuilder header = new StringBuilder("GET /remotedesktop.html?");
		header.append("x=");
		header.append(0);
		header.append("&y=");
		header.append(0);
		header.append("&w=");
		header.append("320");
		header.append("&h=");
		header.append("200");
		header.append("\r\n");
		header.append("Content-Length:");
		header.append(1000);
		header.append("\r\n\r\n");
		StringBuilder body = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			body.append(".");
		}

		client1.sendMessage(header.substring(0, 10));
		Thread.sleep(500);
		client1.sendMessage(header.substring(10));
		Thread.sleep(500);
		client1.sendMessage(body.substring(0, 1));
		Thread.sleep(500);
		client1.sendMessage(body.substring(1));
		Thread.sleep(500);

		latch.await();

	}

	int ii = 0;
	int count = 0;

	@Test(timeout = 10000)
	public void canHandleConcatenatedMessages() throws Exception {
		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();
		assertNotEquals("port", port, 0);

		final CountDownLatch latch = new CountDownLatch(6);

		client1 = new HttpTestClient("client one", address, port, new ResponseHandler() {
			@Override
			public void onMessage(String message) {
				assertTrue(message.length() % 13431 == 0);
				latch.countDown();

			}
		});
		client1.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		StringBuilder header = new StringBuilder("GET /remotedesktop.html?");
		header.append("x=");
		header.append(0);
		header.append("&y=");
		header.append(0);
		header.append("&w=");
		header.append("320");
		header.append("&h=");
		header.append("200");
		header.append("\r\n");
		header.append("Content-Length:");
		header.append(1000);
		header.append("\r\n\r\n");
		StringBuilder body = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			body.append(".");
		}
		StringBuilder msg = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			msg.append(header.toString());
			msg.append(body.toString());
		}

		client1.sendMessage(msg.toString());

		latch.await();

	}

}
