package com.github.remotedesktop.socketserver;

import java.awt.AWTException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.remotedesktop.socketserver.plugin.base.DataFilterPlugin;
import com.github.remotedesktop.socketserver.plugin.base.ServiceHandlerPlugin;

public final class SocketServerBuilder<T extends SocketServer> {
	
	private List<DataFilterPlugin> dataFilterPlugin = new ArrayList<>();
	private List<ServiceHandlerPlugin> serviceHandlerPlugin = new ArrayList<>();
	private String host;
	private String name;
	private int port;
	private T t;

	public SocketServerBuilder(T t) {
		this.t = t;
	}

	public SocketServerBuilder<T> withDataFilterPlugin(DataFilterPlugin dataFilterPlugin) {
		this.dataFilterPlugin.add(dataFilterPlugin);
		return this;
	}

	public SocketServerBuilder<T> withServiceHandlerPlugin(ServiceHandlerPlugin serviceHandlerPlugin) {
		this.serviceHandlerPlugin.add(serviceHandlerPlugin);
		return this;
	}

	public SocketServerBuilder<T> withHost(String host) {
		this.host = host;
		return this;
	}

	public SocketServerBuilder<T> withPort(int port) {
		this.port = port;
		return this;
	}

	public SocketServerBuilder<T> withName(String name) {
		this.name = name;
		return this;
	}

	public T build() throws IOException, AWTException {
		t.init(name, host, port);
		if (dataFilterPlugin != null) {
			t.dataFilters.addAll(dataFilterPlugin);
		}
		if (serviceHandlerPlugin != null) {
			t.serviceHandlers.addAll(serviceHandlerPlugin);
		}
		return t;
	}
}