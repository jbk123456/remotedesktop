package com.github.remotedesktop.socketserver;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.logging.Logger;

public class HttpServer extends SocketServer {
	static final Logger LOGGER = Logger.getLogger(HttpServer.class.getName());

	public static final String HTTP_OK = "200 OK";
	public static final String HTTP_NOTFOUND = "404 Not Found";
	public static final String HTTP_FORBIDDEN = "403 Forbidden";
	public static final String HTTP_INTERNALERROR = "500 Internal Server Error";
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

	public HttpServer(){
		super();
	}

	public int getPort() throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) channel;
		return serverChannel.socket().getLocalPort();
	}

	@Override
	protected Selector openSelector() throws IOException {
		return SelectorProvider.provider().openSelector();
	}

	@Override
	protected SelectionKey channelRegister(Selector selector) throws ClosedChannelException {
		return channel.register(selector, OP_ACCEPT);
	}

	@Override
	protected AbstractSelectableChannel channel(InetSocketAddress address) throws IOException {
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(address);
		return channel;
	}

	public static InputStream getFileInputStream(String fn) {
		return HttpServer.class.getResourceAsStream("/META-INF/resources" + fn);
	}
	public static byte[] getFileContent(String fn) throws IOException {
		InputStream in = getFileInputStream(fn);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		for (int c = 0; (c = in.read(buf, c, buf.length - c)) >= 0;) {
			out.write(buf, 0, c);
		}
		return out.toByteArray();
	}

	public void cancelKey(SelectionKey key) {
		LOGGER.info(String.format( "HttpServer: cancel key %s (%s)", key, key.attachment()));
		key.cancel();
	}
}
