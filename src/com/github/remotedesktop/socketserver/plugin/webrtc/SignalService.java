package com.github.remotedesktop.socketserver.plugin.webrtc;

public interface SignalService {

	//void send(RTCIceCandidate candidate);
	public void sendIce(int sdpMLineIndex, String sdpMid, String sdp);		


	//void send(RTCSessionDescription description);
	public void sendSdp(String sdpType, String sdp);


}
