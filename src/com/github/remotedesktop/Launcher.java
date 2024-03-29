package com.github.remotedesktop;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import com.github.remotedesktop.socketserver.SocketServer;
import com.github.remotedesktop.socketserver.client.DisplayServer;
import com.github.remotedesktop.socketserver.service.http.HttpServer;

public class Launcher {
	private static final String PREFIX = "REMOTEDESKTOP_";
	private static final Logger logger = Logger.getLogger(Launcher.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ");
    
	public static void main(String args[]) throws Exception {
		boolean refreshIni = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().startsWith("--help")) {
				System.out.println("Usage: java -jar remotedesktop.jar --service=true --port=6502:  "
						+ "Starts a HTTP Server listening for requests.\r\n"
						+ "Usage: java -jar remotedesktop.jar --service=false --server=IP --port=6502: "
						+ "Connects to HTTP Server reading mouse and keyboard data and sending it the current "
						+ "desktop as a video stream.\r\nOther options: daemon (true/false) host, port, quality (0.0-1.0), "
						+ "fps, debug (0-3), lock(true/false), lockscreen (in sec).");
				System.exit(1);
			}
			if (args[i].toLowerCase().startsWith("--debug=")) {
				Config.default_log_level = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--service=")) {
				Config.default_start_as_service = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--daemon=")) {
				Config.default_start_as_daemon = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--host=") || args[i].toLowerCase().startsWith("--server=")) {
				Config.default_start_as_service = "false";
				Config.default_http_server = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--port=")) {
				Config.default_http_port = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--quality=")) {
				Config.default_jpeg_quality = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--fps=")) {
				Config.default_fps = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--lock=")) {
				Config.default_lock = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--lockscreen=")) {
				Config.default_lockscreen = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--threads=")) {
				Config.default_threads = args[i].split("=")[1];
				refreshIni = true;
			}
		}
		if (System.getProperty(PREFIX + Config.LOG_LEVEL) != null) {
			Config.default_log_level = System.getProperty(PREFIX + Config.LOG_LEVEL);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.START_AS_SERVICE) != null) {
			Config.default_start_as_service = System.getProperty(PREFIX + Config.START_AS_SERVICE);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.START_AS_DAEMON) != null) {
			Config.default_start_as_daemon = System.getProperty(PREFIX + Config.START_AS_DAEMON);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.HTTP_SERVER) != null) {
			Config.default_http_server = System.getProperty(PREFIX + Config.HTTP_SERVER);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.HTTP_PORT) != null) {
			Config.default_http_port = System.getProperty(PREFIX + Config.HTTP_PORT);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.JPEG_QUALITY) != null) {
			Config.default_jpeg_quality = System.getProperty(PREFIX + Config.JPEG_QUALITY);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.FPS) != null) {
			Config.default_fps = System.getProperty(PREFIX + Config.FPS);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.LOCK) != null) {
			Config.default_lock = System.getProperty(PREFIX + Config.LOCK);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.LOCKSCREEN) != null) {
			Config.default_lockscreen = System.getProperty(PREFIX + Config.LOCKSCREEN);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.THREADS) != null) {
			Config.default_threads = System.getProperty(PREFIX + Config.THREADS);
			refreshIni = true;
		}
		Config.load("remotedesktop.ini", refreshIni);

		if (Config.start_as_daemon && !Boolean.getBoolean("IGNORE_DAEMON_FLAG")) {
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

		boolean startAsService = Config.start_as_service || GraphicsEnvironment.isHeadless();
		if (!startAsService) {
			setupLogger("remotedesktop_displayserver_log.txt");
			logger.info(
					"Display server started. Press BRK key (FN-p) 3 times to terminate screen lock. Reporting to http server: "
							+ Config.http_server + ":" + Config.http_port);
			while (true) {
				DisplayServer displayServer = new DisplayServer("displayserver", Config.http_server, Config.http_port);
				displayServer.startDisplayServer(); 
				displayServer.waitForFinish();
				displayServer.stop();
				logger.info("fatal exception occured (display change?) restarting...");
				Thread.sleep(SocketServer.RETRY_TIMEOUT);
				logger.info("... restart done");
			}
		}
		if (startAsService) {
			setupLogger("remotedesktop_httpserver_log.txt");
			logger.info("http server started on port: " + Config.http_port);

			HttpServer server = new HttpServer(null, Config.http_port);
			server.start();
		}

	}

	private static void setupLogger(String file) {
		Logger logger = Logger.getLogger("");
		Handler[] handlers = logger.getHandlers();
		if (handlers[0] instanceof ConsoleHandler) {
			logger.removeHandler(handlers[0]);
		}
		logger.setLevel(getConfigLogLevel());
		StreamHandler handler;
		try {
			handler = new FileHandler(file);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			handler = new ConsoleHandler();
		}
		SimpleFormatter formatterTxt = new SimpleFormatter() {
			public String format(LogRecord r) {
				StringBuilder sb = new StringBuilder();
				sb.append(DATE_FORMAT.format(new Date(r.getMillis())));
				sb.append(r.getSourceClassName()).append(" ");
				sb.append(r.getSourceMethodName()).append(" ");
				sb.append(r.getLevel().getName()).append(": ");
				sb.append(formatMessage(r)).append(" ");
				if (r.getThrown() != null) {
					StringWriter sw = new StringWriter();
					PrintWriter w = new PrintWriter(sw);
					r.getThrown().printStackTrace(w);
					sb.append(sw.getBuffer().toString());
				}
				sb.append(System.lineSeparator());
				return sb.toString();
			}
		};

		handler.setFormatter(formatterTxt);
		logger.addHandler(handler);
	}

	private static Level getConfigLogLevel() {
		switch (Config.log_level) {
		default:
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
		}
	}
}
