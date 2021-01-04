package com.github.remotedesktop;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.HttpServer;
import com.github.remotedesktop.socketserver.SocketServerBuilder;
import com.github.remotedesktop.socketserver.client.DisplayServer;
import com.github.remotedesktop.socketserver.plugin.base.DefaultRemoteDesktopServiceHandlerPlugin;
import com.github.remotedesktop.socketserver.plugin.ping.RemoteDesktopServicePingPlugin;
import com.github.remotedesktop.socketserver.plugin.webrtc.WebRTCServerPlugin;
import com.github.remotedesktop.socketserver.plugin.webrtc.WebRTCSignalServerPlugin;
import com.github.remotedesktop.socketserver.plugin.websocket.WebSocketDataFilterPlugin;

public class Launcher {
	static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());

	private static final String ENV_PREFIX = "REMOTEDESKTOP_";
	private static final String OPT_PREFIX = "--";

	public static void main(String args[]) throws Exception {
		parseOpts(args);

		if (Config.start_as_daemon && !Boolean.getBoolean("IGNORE_DAEMON_FLAG")) {
			startAsDaemon();
		}

		boolean startAsService = Config.start_as_service || GraphicsEnvironment.isHeadless();
		if (!startAsService) {
			setupLogger("remotedesktop_displayserver_log.txt");
			LOGGER.info(
					"Display server started. Locking input. Press BRK key (FN-p) 3 times to unlock. Reporting to http server: "
							+ Config.http_server + ":" + Config.http_port);
			while (true) {
				DisplayServer displayServer = getDisplayServerWithPlugins();
				displayServer.startDisplayServer();
				displayServer.waitForFinish();
				LOGGER.info("restarting displayserver");
			}
		}
		if (startAsService) {
			setupLogger("remotedesktop_httpserver_log.txt");
			LOGGER.info("http server started on port: " + Config.http_port);

			HttpServer server = getHttpServerWithPlugins();
			server.start();
		}
	}

	private static void startAsDaemon() throws IOException, InterruptedException {
		final String[] newargs = new String[4];

		newargs[0] = "java";
		newargs[1] = "-DIGNORE_DAEMON_FLAG=true";

		newargs[2] = "-jar";
		newargs[3] = "remotedesktop.jar";

		System.in.close();
		System.out.close();
		System.err.close();

		new Thread(new Runnable() {

			public void run() {
				try {
					Runtime.getRuntime().exec(newargs);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(13);
				}
			}
		}).start();
		Thread.sleep(20000);
		System.exit(0);
	}

	private static void parseOpts(String[] args) {
		boolean override = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().startsWith("--help")) {
				System.out.println("Usage: java -jar remotedesktop.jar --service=true --port=6502:  "
						+ "Starts a HTTP Server listening for requests.\r\n"
						+ "Usage: java -jar remotedesktop.jar --service=false --server=IP --port=6502: "
						+ "Connects to HTTP Server reading mouse and keyboard data and sending it the current "
						+ "desktop as a video stream.\r\nOther options: daemon (true/false) host, port, quality (0.0-1.0), "
						+ "fps, debug (0-3), lock(true/false), threads (1-...), tiles (2-...), udp (true/false).");
				System.exit(1);
			}
		}
		for (int i = 0; i < args.length; i++) {
			for (String k : Config.ENV.keySet()) {
				String opt = Config.ENV.get(k).toLowerCase();
				if (args[i].toLowerCase().startsWith(OPT_PREFIX + opt)) {
					Config.DEFAULTS.put("default_" + k.toLowerCase(), args[i].split("=")[1]);
					override = true;
				}
			}
		}

		for (int i = 0; i < args.length; i++) {
			for (String k : Config.ENV.keySet()) {
				String envEntry = System.getProperty(ENV_PREFIX + Config.ENV.get(k));
				if (envEntry != null) {
					Config.DEFAULTS.put(k, envEntry);
					override = true;
				}
			}
		}

		Config.refresh("remotedesktop.ini", override);
	}

	private static DisplayServer getDisplayServerWithPlugins()
			throws IOException, AWTException, NoSuchAlgorithmException {
		final DisplayServer server = new DisplayServer();
		if (Config.udp) {
			return new SocketServerBuilder<>(server).withHost(Config.http_server).withName("WebRTCDisplayServer")
					.withPort(Config.http_port).withDataFilterPlugin(new WebSocketDataFilterPlugin<>(server))
					.withServiceHandlerPlugin(new RemoteDesktopServicePingPlugin())
					.withServiceHandlerPlugin(new DefaultRemoteDesktopServiceHandlerPlugin<>(server))
					.withServiceHandlerPlugin(new WebRTCSignalServerPlugin<>(server, server, server)).build();
		} else {
			return new SocketServerBuilder<>(server).withHost(Config.http_server).withName("DisplayServer")
					.withPort(Config.http_port).withDataFilterPlugin(new WebSocketDataFilterPlugin<>(server))
					.withServiceHandlerPlugin(new RemoteDesktopServicePingPlugin())
					.withServiceHandlerPlugin(new DefaultRemoteDesktopServiceHandlerPlugin<>(server)).build();
		}
	}

	private static HttpServer getHttpServerWithPlugins() throws KeyManagementException, Exception {

		final HttpServer server = new HttpServer();

		if (Config.udp) {
			return new SocketServerBuilder<>(server).withPort(Config.http_port).withName("WebRTCHttpServer")
					.withDataFilterPlugin(new WebSocketDataFilterPlugin<>(server))
					.withServiceHandlerPlugin(new RemoteDesktopServicePingPlugin())
					.withServiceHandlerPlugin(new WebRTCServerPlugin<>(server))
					.withServiceHandlerPlugin(new DefaultRemoteDesktopServiceHandlerPlugin<>(server))
					.build();
		} else {
			return new SocketServerBuilder<>(server).withPort(Config.http_port).withName("HttpServer")
					.withDataFilterPlugin(new WebSocketDataFilterPlugin<>(server))
					.withServiceHandlerPlugin(new RemoteDesktopServicePingPlugin())
					.withServiceHandlerPlugin(new DefaultRemoteDesktopServiceHandlerPlugin<>(server)).build();
		}
	}

	private static void setupLogger(String file) throws SecurityException, IOException {
		InputStream in = Launcher.class.getResourceAsStream("/META-INF/resources/logging.properties");
		LogManager.getLogManager().readConfiguration(in);

		Level logLevel = getConfigLogLevel();
		if (logLevel == null) {
			return;
		}

		Logger rootLogger = Logger.getLogger("");
		FileHandler handler = new FileHandler(file);

		handler.setLevel(logLevel);
		rootLogger.setLevel(logLevel);

		rootLogger.addHandler(handler);
	}

	private static Level getConfigLogLevel() {
		switch (Config.log_level) {
		case 0:
			return Level.INFO;
		case 1:
			return Level.FINE;
		case 2:
			return Level.FINER;
		case 3:
			return Level.FINEST;
		case 9:
			return Level.ALL;
		case -1:
			return Level.OFF;
		default:
		case -2:
			return null;
		}
	}
}
