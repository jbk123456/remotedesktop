/*
 * Copyright 2015 Corey Baswell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.remotedesktop.socketserver.niossl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

class SSLEngineBuffer {
	static final Logger LOGGER = Logger.getLogger(SSLEngineBuffer.class.getName());

	private final SocketChannel socketChannel;

	private final SSLEngine sslEngine;

	private final ExecutorService executorService;

	private final ByteBuffer networkInboundBuffer;

	private final ByteBuffer networkOutboundBuffer;

	private final int minimumApplicationBufferSize;

	private final ByteBuffer unwrapBuffer;

	private final ByteBuffer wrapBuffer;

	private boolean firstByte = true;

	public SSLEngineBuffer(SocketChannel socketChannel, SSLEngine sslEngine, ExecutorService executorService) {
		this.socketChannel = socketChannel;
		this.sslEngine = sslEngine;
		this.executorService = executorService;

		SSLSession session = sslEngine.getSession();
		int networkBufferSize = session.getPacketBufferSize();

		networkInboundBuffer = ByteBuffer.allocate(networkBufferSize);

		networkOutboundBuffer = ByteBuffer.allocate(networkBufferSize);
		networkOutboundBuffer.flip();

		minimumApplicationBufferSize = session.getApplicationBufferSize();
		unwrapBuffer = ByteBuffer.allocate(minimumApplicationBufferSize);
		wrapBuffer = ByteBuffer.allocate(minimumApplicationBufferSize);
		wrapBuffer.flip();
	}

	int unwrap(ByteBuffer applicationInputBuffer) throws IOException, PlaintextConnectionException {
		if (applicationInputBuffer.capacity() < minimumApplicationBufferSize) {
			throw new IllegalArgumentException(
					"Application buffer size must be at least: " + minimumApplicationBufferSize);
		}

		if (unwrapBuffer.position() != 0) {
			unwrapBuffer.flip();
			while (unwrapBuffer.hasRemaining() && applicationInputBuffer.hasRemaining()) {
				applicationInputBuffer.put(unwrapBuffer.get());
			}
			unwrapBuffer.compact();
		}

		int totalUnwrapped = 0;
		int unwrapped, wrapped;

		do {
			totalUnwrapped += unwrapped = doUnwrap(applicationInputBuffer);
			wrapped = doWrap(wrapBuffer);
		} while (unwrapped > 0
				|| wrapped > 0 && (networkOutboundBuffer.hasRemaining() && networkInboundBuffer.hasRemaining()));

		return totalUnwrapped;
	}

	int wrap(ByteBuffer applicationOutboundBuffer) throws IOException, PlaintextConnectionException {
		firstByte = false;
		int wrapped = doWrap(applicationOutboundBuffer);
		doUnwrap(unwrapBuffer);
		return wrapped;
	}

	int flushNetworkOutbound() throws IOException {
		return send(socketChannel, networkOutboundBuffer);
	}

	int send(SocketChannel channel, ByteBuffer buffer) throws IOException {
		int totalWritten = 0;
		while (buffer.hasRemaining()) {
			int written = channel.write(buffer);

			if (written == 0) {
				break;
			} else if (written < 0) {
				return (totalWritten == 0) ? written : totalWritten;
			}
			totalWritten += written;
		}
		LOGGER.fine(String.format("sent: %d out to socket", totalWritten));
		return totalWritten;
	}

	void close() {
		try {
			sslEngine.closeInbound();
		} catch (Exception e) {
		}

		try {
			sslEngine.closeOutbound();
		} catch (Exception e) {
		}
	}

	private int doUnwrap(ByteBuffer applicationInputBuffer) throws IOException, PlaintextConnectionException {
		LOGGER.fine("unwrap:");

		int totalReadFromChannel = 0;

		// Keep looping until peer has no more data ready or the
		// applicationInboundBuffer is full
		UNWRAP: do {
			// 1. Pull data from peer into networkInboundBuffer

			int readFromChannel = 0;
			while (networkInboundBuffer.hasRemaining()) {
				int read = socketChannel.read(networkInboundBuffer);
				LOGGER.fine(
						String.format("unwrap: socket read %d (%d , %d)", read, readFromChannel, totalReadFromChannel));
				if (read <= 0) {
					if ((read < 0) && (readFromChannel == 0) && (totalReadFromChannel == 0)) {
						// No work done and we've reached the end of the channel from peer
						LOGGER.fine("unwrap: exit: end of channel");
						return read;
					}
					break;
				} else {
					readFromChannel += read;
				}
			}

			networkInboundBuffer.flip();
			if (!networkInboundBuffer.hasRemaining()) {
				networkInboundBuffer.compact();
				// wrap(applicationOutputBuffer, applicationInputBuffer, false);
				return totalReadFromChannel;
			}

			totalReadFromChannel += readFromChannel;

			try {
				if (firstByte  && networkInboundBuffer.array()[0]!=0x16) {
					byte[] d = new byte[networkInboundBuffer.remaining()];
					networkInboundBuffer.get(d);
					throw new PlaintextConnectionException(d);
				}
				firstByte=false;
				SSLEngineResult result = sslEngine.unwrap(networkInboundBuffer, applicationInputBuffer);
				LOGGER.fine(String.format("unwrap: result: %s",result));

				switch (result.getStatus()) {
				case OK:
					SSLEngineResult.HandshakeStatus handshakeStatus = result.getHandshakeStatus();
					switch (handshakeStatus) {
					case NEED_UNWRAP:
						break;

					case NEED_WRAP:
						break UNWRAP;

					case NEED_TASK:
						runHandshakeTasks();
						break;

					case NOT_HANDSHAKING:
					default:
						break;
					}
					break;

				case BUFFER_OVERFLOW:
					LOGGER.fine("unwrap: buffer overflow");
					break UNWRAP;

				case CLOSED:
					LOGGER.fine("unwrap: exit: ssl closed");
					return totalReadFromChannel == 0 ? -1 : totalReadFromChannel;

				case BUFFER_UNDERFLOW:
					LOGGER.fine("unwrap: buffer underflow");
					break;
				}
			} finally {
				networkInboundBuffer.compact();
			}
		} while (applicationInputBuffer.hasRemaining());

		return totalReadFromChannel;
	}

	private int doWrap(ByteBuffer applicationOutboundBuffer) throws IOException {
		LOGGER.fine("wrap:");
		int totalWritten = 0;

		// 1. Send any data already wrapped out channel

		if (networkOutboundBuffer.hasRemaining()) {
			totalWritten = send(socketChannel, networkOutboundBuffer);
			if (totalWritten < 0) {
				return totalWritten;
			}
		}

		// 2. Any data in application buffer ? Wrap that and send it to peer.

		WRAP: while (true) {
			networkOutboundBuffer.compact();
			SSLEngineResult result = sslEngine.wrap(applicationOutboundBuffer, networkOutboundBuffer);
			LOGGER.fine("wrap: result: " + result);

			networkOutboundBuffer.flip();
			if (networkOutboundBuffer.hasRemaining()) {
				int written = send(socketChannel, networkOutboundBuffer);
				if (written < 0) {
					return totalWritten == 0 ? written : totalWritten;
				} else {
					totalWritten += written;
				}
			}

			switch (result.getStatus()) {
			case OK:
				switch (result.getHandshakeStatus()) {
				case NEED_WRAP:
					break;

				case NEED_UNWRAP:
					break WRAP;

				case NEED_TASK:
					runHandshakeTasks();
					LOGGER.fine("wrap: exit: need tasks");
					break;

				case NOT_HANDSHAKING:
					if (applicationOutboundBuffer.hasRemaining()) {
						break;
					} else {
						break WRAP;
					}
				case FINISHED:
					break;
				case NEED_UNWRAP_AGAIN:
					break;
				default:
					throw new IllegalStateException("unknown status");
				}
				break;

			case BUFFER_OVERFLOW:
				LOGGER.fine("wrap: exit: buffer overflow");
				break WRAP;

			case CLOSED:
				LOGGER.fine("wrap: exit: closed");
				break WRAP;

			case BUFFER_UNDERFLOW:
				LOGGER.fine("wrap: exit: buffer underflow");
				break WRAP;
			}
		}

		LOGGER.fine(String.format("wrap: return: %d", totalWritten));
		return totalWritten;
	}

	private void runHandshakeTasks() {
		while (true) {
			final Runnable runnable = sslEngine.getDelegatedTask();
			if (runnable == null) {
				break;
			} else {
				executorService.execute(runnable);
			}
		}
	}
}
