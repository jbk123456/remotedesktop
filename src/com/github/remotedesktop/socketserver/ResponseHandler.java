package com.github.remotedesktop.socketserver;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface ResponseHandler {
	void onMessage(SelectionKey key, byte[] message) throws IOException;
}
