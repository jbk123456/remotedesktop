package com.github.remotedesktop.socketserver;

public interface ResponseHandler {
	void onMessage(byte[] message);
}
