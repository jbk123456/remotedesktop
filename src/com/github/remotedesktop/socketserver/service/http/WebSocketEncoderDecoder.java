package com.github.remotedesktop.socketserver.service.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class WebSocketEncoderDecoder {

	private boolean mMasking = false;

	private int mStage;

	private boolean mFinal;
	private boolean mMasked;
	private int mOpcode;
	private int mLengthSize;
	private int mLength;
	private int mMode;

	private byte[] mMask = new byte[0];
	private byte[] mPayload = new byte[0];

	private boolean mClosed = false;

	private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();

	private static final int BYTE = 255;
	private static final int FIN = 128;
	private static final int MASK = 128;
	private static final int RSV1 = 64;
	private static final int RSV2 = 32;
	private static final int RSV3 = 16;
	private static final int OPCODE = 15;
	private static final int LENGTH = 127;

	private static final int MODE_TEXT = 1;
	private static final int MODE_BINARY = 2;

	private static final int OP_CONTINUATION = 0;
	private static final int OP_TEXT = 1;
	private static final int OP_BINARY = 2;
	private static final int OP_CLOSE = 8;
	private static final int OP_PING = 9;
	private static final int OP_PONG = 10;

	private static final List<Integer> OPCODES = Arrays.asList(OP_CONTINUATION, OP_TEXT, OP_BINARY, OP_CLOSE, OP_PING,
			OP_PONG);

	private static final List<Integer> FRAGMENTED_OPCODES = Arrays.asList(OP_CONTINUATION, OP_TEXT, OP_BINARY);

	private static byte[] mask(byte[] payload, byte[] mask, int offset) {
		if (mask.length == 0)
			return payload;

		for (int i = 0; i < payload.length - offset; i++) {
			payload[offset + i] = (byte) (payload[offset + i] ^ mask[i % 4]);
		}
		return payload;
	}

	public byte[] decodeFrames(byte[] data) throws IOException {
		ByteDataInputStream stream = new ByteDataInputStream(new DataInputStream(new ByteArrayInputStream(data)));
		byte[] result = null;
		;
		try {
			while (stream.available() != -1) {
				switch (mStage) {
				case 0:
					parseOpcode(stream.readByte());
					break;
				case 1:
					parseLength(stream.readByte());
					break;
				case 2:
					parseExtendedLength(stream.readBytes(mLengthSize));
					break;
				case 3:
					mMask = stream.readBytes(4);
					mStage = 4;
					break;
				case 4:
					mPayload = stream.readBytes(mLength);
					result = writeDecodedFrames();
					mStage = 0;
					return result;
				}
			}
		} finally {
			stream.close();
		}
		return result;
	}

	private void parseOpcode(byte data) throws IOException {
		boolean rsv1 = (data & RSV1) == RSV1;
		boolean rsv2 = (data & RSV2) == RSV2;
		boolean rsv3 = (data & RSV3) == RSV3;

		if (rsv1 || rsv2 || rsv3) {
			throw new IOException("RSV not zero");
		}

		mFinal = (data & FIN) == FIN;
		mOpcode = (data & OPCODE);
		mMask = new byte[0];
		mPayload = new byte[0];

		if (!OPCODES.contains(mOpcode)) {
			throw new IOException("Bad opcode");
		}

		if (!FRAGMENTED_OPCODES.contains(mOpcode) && !mFinal) {
			throw new IOException("Expected non-final packet");
		}

		mStage = 1;
	}

	private void parseLength(byte data) {
		mMasked = (data & MASK) == MASK;
		mLength = (data & LENGTH);

		if (mLength >= 0 && mLength <= 125) {
			mStage = mMasked ? 3 : 4;
		} else {
			mLengthSize = (mLength == 126) ? 2 : 8;
			mStage = 2;
		}
	}

	private void parseExtendedLength(byte[] buffer) throws IOException {
		mLength = getInteger(buffer);
		mStage = mMasked ? 3 : 4;
	}


	private byte[] writeDecodedFrames() throws IOException {
		byte[] result = null;
		;
		byte[] payload = mask(mPayload, mMask, 0);
		int opcode = mOpcode;

		if (opcode == OP_CONTINUATION) {
			if (mMode == 0) {
				throw new IOException("Mode was not set.");
			}
			mBuffer.write(payload);
			if (mFinal) {
				byte[] message = mBuffer.toByteArray();
				if (mMode == MODE_TEXT) {
					result = onMessage(encode(message));
				} else {
					result = onMessage(message);
				}
				reset();
			}

		} else if (opcode == OP_TEXT) {
			if (mFinal) {
				String messageText = encode(payload);
				result = onMessage(messageText);
			} else {
				mMode = MODE_TEXT;
				mBuffer.write(payload);
			}

		} else if (opcode == OP_BINARY) {
			if (mFinal) {
				result = onMessage(payload);
			} else {
				mMode = MODE_BINARY;
				mBuffer.write(payload);
			}

		} else if (opcode == OP_CLOSE) {
			byte[] message = mBuffer.toByteArray();
			result = onMessage(encode(message));
		}
		return result;
	}

	private byte[] onMessage(String message) throws IOException {
		return message.getBytes("UTF-8");
	}

	private byte[] onMessage(byte[] data) throws IOException {
		return data;
	}

	private void reset() {
		mMode = 0;
		mBuffer.reset();
	}

	
	public byte[] encodeFrame(String data) {
		return encodeFrame(data, OP_TEXT, -1);
	}

	public byte[] encodeFrame(byte[] data) {
		return encodeFrame(data, OP_BINARY, -1);
	}

	private byte[] encodeFrame(byte[] data, int opcode, int errorCode) {
		return encodeFrame((Object) data, opcode, errorCode);
	}

	private byte[] encodeFrame(String data, int opcode, int errorCode) {
		return encodeFrame((Object) data, opcode, errorCode);
	}

	private byte[] encodeFrame(Object data, int opcode, int errorCode) {
		if (mClosed)
			return null;

		byte[] buffer = (data instanceof String) ? decode((String) data) : (byte[]) data;
		int insert = (errorCode > 0) ? 2 : 0;
		int length = buffer.length + insert;
		int header = (length <= 125) ? 2 : (length <= 65535 ? 4 : 10);
		int offset = header + (mMasking ? 4 : 0);
		int masked = mMasking ? MASK : 0;
		byte[] frame = new byte[length + offset];

		frame[0] = (byte) ((byte) FIN | (byte) opcode);

		if (length <= 125) {
			frame[1] = (byte) (masked | length);
		} else if (length <= 65535) {
			frame[1] = (byte) (masked | 126);
			frame[2] = (byte) Math.floor(length / 256);
			frame[3] = (byte) (length & BYTE);
		} else {
			frame[1] = (byte) (masked | 127);
			frame[2] = (byte) (((int) Math.floor(length / Math.pow(2, 56))) & BYTE);
			frame[3] = (byte) (((int) Math.floor(length / Math.pow(2, 48))) & BYTE);
			frame[4] = (byte) (((int) Math.floor(length / Math.pow(2, 40))) & BYTE);
			frame[5] = (byte) (((int) Math.floor(length / Math.pow(2, 32))) & BYTE);
			frame[6] = (byte) (((int) Math.floor(length / Math.pow(2, 24))) & BYTE);
			frame[7] = (byte) (((int) Math.floor(length / Math.pow(2, 16))) & BYTE);
			frame[8] = (byte) (((int) Math.floor(length / Math.pow(2, 8))) & BYTE);
			frame[9] = (byte) (length & BYTE);
		}

		if (errorCode > 0) {
			frame[offset] = (byte) (((int) Math.floor(errorCode / 256)) & BYTE);
			frame[offset + 1] = (byte) (errorCode & BYTE);
		}
		System.arraycopy(buffer, 0, frame, offset + insert, buffer.length);

		if (mMasking) {
			byte[] mask = { (byte) Math.floor(Math.random() * 256), (byte) Math.floor(Math.random() * 256),
					(byte) Math.floor(Math.random() * 256), (byte) Math.floor(Math.random() * 256) };
			System.arraycopy(mask, 0, frame, header, mask.length);
			mask(frame, mask, offset);
		}

		return frame;
	}

	private String encode(byte[] buffer) {
		try {
			return new String(buffer, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] decode(String string) {
		try {
			return (string).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private int getInteger(byte[] bytes) throws IOException {
		long i = byteArrayToLong(bytes, 0, bytes.length);
		if (i < 0 || i > Integer.MAX_VALUE) {
			throw new IOException("Bad integer: " + i);
		}
		return (int) i;
	}

	private static long byteArrayToLong(byte[] b, int offset, int length) {
		if (b.length < length)
			throw new IllegalArgumentException("length must be less than or equal to b.length");

		long value = 0;
		for (int i = 0; i < length; i++) {
			int shift = (length - 1 - i) * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}

	private static class ByteDataInputStream extends DataInputStream {
		public ByteDataInputStream(InputStream in) {
			super(in);
		}

		public byte[] readBytes(int length) throws IOException {
			byte[] buffer = new byte[length];
			readFully(buffer);
			return buffer;
		}
	}
}
