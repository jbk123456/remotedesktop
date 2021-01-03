package com.github.remotedesktop.socketserver.http;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface ResponseHandler {

	void onMessage(SelectionKey key, byte[] data) throws IOException;

}
