package com.github.remotedesktop.socketserver.plugin.base;

import java.io.IOException;

import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;

public interface ServiceHandlerPlugin {
	boolean service(Request req, Response res) throws IOException;
}