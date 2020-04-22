package com.github.remotedesktop.socketserver.service.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

public class Response {
	private Properties headers;
	private StringBuffer headersBuffer;
	private int state;
	private String httpStatus;
	private byte[] data;

	private byte[] getHeaders() {
		StringBuilder b = new StringBuilder();
		b.append("HTTP/1.1 ").append(httpStatus).append("\r\n");
		for (Entry<Object, Object> h : headers.entrySet()) {
			b.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
		}

		b.append("\r\n");
		return b.toString().getBytes();
	}

	public byte[] getResponse() throws IOException {
		headers.put("Content-length", data.length);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		bout.write(getHeaders());
		bout.write(data);
		return bout.toByteArray();
	}

	public void dataStream(String status, String mime, byte[] data) throws IOException {
		httpStatus = status;
		setMimeType(mime);
		this.data = data;
		setReady();
	}

	public Response() {
		headers = new Properties();
		headersBuffer = new StringBuffer();
		state = 0;
		httpStatus = "200 OK";
		data = null;
//			bufStream = null;
		headers.clear();
		headersBuffer.delete(0, headersBuffer.length());
		setMimeType("text/plain");
	}

	public void setHeader(String s, String s1) {
		headers.setProperty(s, s1);
	}

	public String getHeader(String s) {
		return headers.getProperty(s);
	}

	public void setMimeType(String s) {
		setHeader("Content-Type", s);
	}

	public boolean isReady() {
		return state > 0;
	}

	public void setReady() {
		state = 2;
	}

	public boolean isFinished() {
		return state == 4;
	}

	public String getMimeType() {
		return getHeader("Content-Type");
	}

	public void redirect(String s) {
		String s1 = (new StringBuilder()).append("<html><body>Redirected: <a href=\"").append(s).append("\">").append(s)
				.append("</a></body></html>").toString();
		httpStatus = "301 Moved Permanently";
		setMimeType("text/html");
		setHeader("Location", s);
		data = (s1.getBytes());
		setReady();
	}

	public void message(String s, String s1, String s2) {
		httpStatus = s;
		setMimeType(s1);
		data = (s2.getBytes());
		setReady();
	}

	public void message(String s, String s1) {
		message(s, getMimeType(), s1);
	}

	public void exception(String s, String s1) throws IOException {
		message(s, s1);
		throw new IOException(s1);
	}
}