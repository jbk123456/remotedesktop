package com.github.remotedesktop.socketserver.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.service.http.HttpServer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HttpServerIntegrationTest {

	private String address = "localhost";
	private HttpServer server;
	private HttpClientTestAdapter httpBrowserClient;

	@After
	public void cleanUp() throws Exception {
		httpBrowserClient.stop();
		server.stop();
	}

	@Test(timeout = 5000)
	public void canHandlePartialMessages() throws Exception {
		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();

		assertNotEquals("port", port, 0);

		StringBuilder body = getBody();
		StringBuilder expectedResponseHeader = getExpectedResponseHeader(body.length());
		StringBuilder header = getHeader(body.length());
		final int expectedMessageLength = expectedResponseHeader.length() + body.length();

		final CountDownLatch latch = new CountDownLatch(1);

		httpBrowserClient = new HttpClientTestAdapter("httpBrowserClient", address, port, new ResponseHandler() {
			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				assertEquals("message", expectedMessageLength, message.length);
				latch.countDown();
			}
		});
		httpBrowserClient.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		httpBrowserClient.sendMessage(header.substring(0, 10));
		Thread.sleep(500);
		httpBrowserClient.sendMessage(header.substring(10));
		Thread.sleep(500);
		httpBrowserClient.sendMessage(body.substring(0, 1));
		Thread.sleep(500);
		httpBrowserClient.sendMessage(body.substring(1));
		Thread.sleep(500);

		latch.await();

	}

	@Test(timeout = 10000)
	public void canHandleConcatenatedMessages() throws Exception {

		server = new HttpServer("localhost", 0);
		server.start();
		int port = server.getPort();
		assertNotEquals("port", port, 0);

		StringBuilder body = getBody();
		StringBuilder expectedResponseHeader = getExpectedResponseHeader(body.length());
		StringBuilder header = getHeader(body.length());
		final int expectedMessageLength = expectedResponseHeader.length() + body.length();

		final int numMessages = 10000;
		StringBuilder msg = new StringBuilder();
		for (int i = 0; i < numMessages; i++) {
			msg.append(header.toString());
			msg.append(body.toString());
		}


		/*
		 * The server splits the packets and parses the header. If it understands
		 * the message, it will send the message back.
		 * But this doesn't mean that we'll receive the message that way. It might
		 * be that we receive only a part of the message or if we receive 
		 * concatenated messages. So we check if we have received all messages
		 * back from the server, which means that it was able to understand them.
		 * 
		 */
		final int[] latch = new int[] { expectedMessageLength * numMessages };

		httpBrowserClient = new HttpClientTestAdapter("httpBrowserClient", address, port, new ResponseHandler() {
			@Override
			public void onMessage(SelectionKey key, byte[] message) {
				synchronized (latch) {
					latch[0] -= message.length;
					if (latch[0] == 0) {
						latch.notify();
					}
				}

			}
		});
		httpBrowserClient.start();

		// Allow a bit of time for both connections to be established
		Thread.sleep(500);

		httpBrowserClient.sendMessage(msg.toString());

		synchronized (latch) {
			while (latch[0] > 0) {
				latch.wait();
			}
		}
		assertEquals(0, latch[0]);
	}

	private StringBuilder getBody() {
		StringBuilder body = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			body.append(".");
		}
		return body;
	}

	private StringBuilder getExpectedResponseHeader(int bodyLength) {
		StringBuilder header = new StringBuilder("HTTP/1.1 200 OK");
		header.append("\r\n");
		header.append("Content-Length: ");
		header.append(bodyLength);
		header.append("\r\n");
		header.append("Content-Type: text/plain");
		header.append("\r\n\r\n");
		return header;
	}

	private StringBuilder getHeader(int bodyLength) {
		StringBuilder header = new StringBuilder("GET /ping");
		header.append("\r\n");
		header.append("Host: 120.0.0.1");
		header.append("\r\n");
		header.append("Content-Type: text/plain");
		header.append("\r\n");
		header.append("Content-Length: ");
		header.append(bodyLength);
		header.append("\r\n\r\n");
		return header;
	}
}
