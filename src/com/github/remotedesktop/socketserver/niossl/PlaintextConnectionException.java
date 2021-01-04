package com.github.remotedesktop.socketserver.niossl;

public class PlaintextConnectionException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;
	private byte[] data;

	public PlaintextConnectionException(byte[] data) {
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

}
