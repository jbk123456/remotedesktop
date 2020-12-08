package com.github.remotedesktop.socketserver.service;

public class TileSerializer {

	private byte[] streamData;
	private boolean dirty;
	private int width;
	private int height;

	public TileSerializer() {
		dirty = false;
		width = 0;
		height = 0;
		streamData= new byte[0];
		width = 96; 
		height = 96;
	}

	public int fileSize() {
		return streamData.length;
	}

	public void updateImage2(byte[] streamData, int width, int height) {
		this.streamData = streamData;
		this.width = width;
		this.height = height;
		dirty = true;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clearDirty() {
		dirty = false;
	}

	public byte[] getData() {
		return streamData;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
