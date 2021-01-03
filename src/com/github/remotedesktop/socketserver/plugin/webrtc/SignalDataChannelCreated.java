package com.github.remotedesktop.socketserver.plugin.webrtc;

import dev.onvoid.webrtc.RTCDataChannel;

public interface SignalDataChannelCreated {
	
	void onCreated(RTCDataChannel channel);

}
