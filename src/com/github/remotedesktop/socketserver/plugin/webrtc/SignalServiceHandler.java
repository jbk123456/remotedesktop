package com.github.remotedesktop.socketserver.plugin.webrtc;

public interface SignalServiceHandler {

	void signalIceResponse(int sdpMLineIndex, String sdpMid, String sdp);

	void signalSdpResponse(String type, String sdp);

}
