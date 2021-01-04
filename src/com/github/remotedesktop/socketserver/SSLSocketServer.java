package com.github.remotedesktop.socketserver;

import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.github.remotedesktop.socketserver.niossl.PlaintextConnectionException;
import com.github.remotedesktop.socketserver.niossl.SSLServerSocketChannel;
import com.github.remotedesktop.socketserver.niossl.SSLSocketChannel;

public abstract class SSLSocketServer extends SocketServer {
	static final Logger LOGGER = Logger.getLogger(SSLSocketServer.class.getName());

	protected SSLServerSocketChannel sslChannel;
	private SSLContext sslContext;
	private ThreadPoolExecutor sslThreadPool;

	protected AbstractSelectableChannel getChannel() {
		return sslChannel;
	}

	protected SelectableChannel getChannel(SelectionKey key) {
		return Attachment.channel(key);
	}

	public SSLSocketServer() throws KeyManagementException, Exception {
		super();
		sslContext = SSLContext.getInstance("TLS");
		sslContext.init(createKeyManagers("/server.jks", "storepass", "keypass"),
				createTrustManagers("/trustedCerts.jks", "storepass"), new SecureRandom());

		sslThreadPool = new ThreadPoolExecutor(5, 20, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	protected void cleanUp() {
		super.cleanUp();
		sslThreadPool.shutdownNow();
	}

	@Override
	protected AbstractSelectableChannel channel(InetSocketAddress address) throws IOException {
		ServerSocketChannel channel = ServerSocketChannel.open();
		sslChannel = new SSLServerSocketChannel(channel, sslContext, sslThreadPool);

		channel.configureBlocking(false);
		channel.socket().bind(address);

		return channel;
	}

	protected void accept(SelectionKey serverKey) throws IOException {
		SSLSocketChannel socketChannel = (SSLSocketChannel) sslChannel.accept();

		socketChannel.configureBlocking(false);
		SelectionKey key;
		enableAttachments(key = socketChannel.getWrappedSocketChannel().register(selector, OP_READ));
		Attachment.setChannel(key, (SSLSocketChannel) socketChannel);
	}

	private KeyManager[] createKeyManagers(String fn, String keystorePassword, String keyPassword) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		InputStream keyStoreIS = HttpServer.getFileInputStream(fn);
		try {
			keyStore.load(keyStoreIS, keystorePassword.toCharArray());
		} finally {
			if (keyStoreIS != null) {
				keyStoreIS.close();
			}
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, keyPassword.toCharArray());
		return kmf.getKeyManagers();
	}

	protected TrustManager[] createTrustManagers(String fn, String keystorePassword) throws Exception {
		KeyStore trustStore = KeyStore.getInstance("JKS");
		InputStream trustStoreIS = HttpServer.getFileInputStream(fn);
		try {
			trustStore.load(trustStoreIS, keystorePassword.toCharArray());
		} finally {
			if (trustStoreIS != null) {
				trustStoreIS.close();
			}
		}
		TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustFactory.init(trustStore);
		return trustFactory.getTrustManagers();
	}

	protected void read(SelectionKey key) throws IOException {
		try {
			super.read(key);
		} catch (PlaintextConnectionException oneShotContinuation) {
			Attachment.setChannel(key, key.channel());
			handleIncomingData(key, oneShotContinuation.getData());
			super.read(key);
		}
	}
}
