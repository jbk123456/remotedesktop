package com.github.remotedesktop.socketserver.plugin.webrtc;

import java.util.List;
import java.util.logging.Logger;

import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.video.VideoTrack;

// @formatter:off
/**
 * A: SDP 3-way handshake: 
 * 1. pc.setLocalDescription(pc.createOffer()) ==offer=> to peer 
 * 2 peer calls: pc.setRemoteDescription(offer);pc.setLocalDescription(pc.createAnswer()); =answer=> to me 
 * 3 pc.setRemoteDescription(answer);
 * 
 * B: ICE if a datachannel or stream is requested, peers are informed about each other: 
 * 1. pc runs: onIceCandidate =candidate=> to peer 
 * 2. peer calls: pc.addIceCandidate(candidate)
 */
// @formatter:on

public class SimpleRTCPeerConnection implements SignalServiceHandler {
	static final Logger LOGGER = Logger.getLogger(SimpleRTCPeerConnection.class.getName());

	private final RTCPeerConnection pc;
	private final PeerConnectionFactory peerConnectionFactory;
	private final RTCConfiguration config;
	private final SignalService sig;
	private final RTCOfferOptions options;
	private final RTCAnswerOptions answerOptions;
	private final PeerConnectionHandler peerConnectionHandler;
	private final SetSessionDescriptionAnswerHandler setSessionDescriptionAnswerHandler;
	private final CreateSessionDescriptionHandler createSessionDescriptionHandler;

	private RTCDataChannel dataChannel;

	private RTCDataChannelObserver dcObserver;
	private final SignalDataChannelCreated dataChannelCreatedHandler;

	/**
	 * Used by pc.createOffer which initiates stage A
	 *
	 */
	private class CreateSessionDescriptionHandler extends SetSessionDescriptionAnswerHandler
			implements CreateSessionDescriptionObserver {
		@Override
		public void onSuccess(final RTCSessionDescription description) {

			LOGGER.fine("CreateOffer or CreateAnswer success. Now setLocalDescription...");
			pc.setLocalDescription(description, new SetSessionDescriptionAnswerHandler() {
				@Override
				public void onSuccess() {
					LOGGER.fine("SetLocalDescription success. Now send SDP to ==> peer...");
					sig.sendSdp(description.sdpType.name().toLowerCase(), description.sdp);
					// sig.send(description);
				}
			});
		}
	}

	/**
	 * Used in stage A to terminate the chain.
	 *
	 */
	private class SetSessionDescriptionAnswerHandler implements SetSessionDescriptionObserver {

		@Override
		public void onSuccess() {
			LOGGER.fine("Received ANSWER <=== from peer");
		}

		@Override
		public void onFailure(String error) {
			LOGGER.warning(String.format("SetSessionDescriptionAnswerHandler failed: %s", error));
		}
	}

	/**
	 * Used by createPeerConnection to initiate stage B
	 *
	 */
	private class PeerConnectionHandler implements PeerConnectionObserver {
		@Override
		public void onIceCandidate(RTCIceCandidate candidate) {
			LOGGER.fine(String.format("Received ice candidate: %s", candidate));
			sig.sendIce(candidate.sdpMLineIndex, candidate.sdpMid, candidate.sdp);
			LOGGER.fine(String.format("Send ice candidate %s to ===> peer"));
			pc.createOffer(options, createSessionDescriptionHandler);
			LOGGER.fine(String.format("Will create OFFER"));
		}

		public void onDataChannel(RTCDataChannel chan) {
			LOGGER.fine("Datachannel CREATED");
			dataChannelCreatedHandler.onCreated(dataChannel = chan);
			dataChannel.registerObserver(dcObserver);
		}

		public void onRenegotiationNeeded() {
			LOGGER.warning("RENEGOTIOATON"); // WTF?!?
		}
	}

	/**
	 * Used by signal server in stage A
	 */
	@Override
	public void signalSdpResponse(String type, String sdp) {
		RTCSdpType sdpType = RTCSdpType.valueOf(type.toUpperCase());

		switch (sdpType) {
		case ANSWER: {
			RTCSessionDescription description = new RTCSessionDescription(sdpType, sdp);
			LOGGER.fine(String.format("Got ANSWER: %s", sdp));
			pc.setRemoteDescription(description, setSessionDescriptionAnswerHandler);
			LOGGER.fine("about to set the remote description");
			break;
		}
		case OFFER: {
			LOGGER.fine("Got OFFER. About to create remote description");
			pc.setRemoteDescription(new RTCSessionDescription(sdpType, sdp), new SetSessionDescriptionAnswerHandler() {
				@Override
				public void onSuccess() {
					LOGGER.fine("Create Remote Description ok, now creating ANSWER");
					pc.createAnswer(answerOptions, createSessionDescriptionHandler);
				}
			});
			break;
		}
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Used by signal server in stage B
	 */
	@Override
	public void signalIceResponse(int sdpMLineIndex, String sdpMid, String sdp) {
		LOGGER.fine(String.format("peer ===> signal server has sent us an ICE response: %s", sdp));
		pc.addIceCandidate(new RTCIceCandidate(sdpMid, sdpMLineIndex, sdp));
		LOGGER.fine(String.format("About to add ICE candidate"));
	}

	public SimpleRTCPeerConnection(SignalService server, RTCDataChannelObserver dcObserver,
			SignalDataChannelCreated dcHandler) {
		sig = server;
		config = new RTCConfiguration();
		peerConnectionFactory = new PeerConnectionFactory();
		options = new RTCOfferOptions();
		answerOptions = new RTCAnswerOptions();
		peerConnectionHandler = new PeerConnectionHandler();
		createSessionDescriptionHandler = new CreateSessionDescriptionHandler();
		pc = peerConnectionFactory.createPeerConnection(config, peerConnectionHandler);

		setSessionDescriptionAnswerHandler = new SetSessionDescriptionAnswerHandler();

		this.dcObserver = dcObserver;
		this.dataChannelCreatedHandler = dcHandler;
	}

	public void start() {
		pc.createOffer(options, createSessionDescriptionHandler);
	}

	public void addTrack(VideoTrack videoTrack, List<String> of) {
		pc.addTrack(videoTrack, of);
	}

	public RTCRtpTransceiver[] getTransceivers() {
		return pc.getTransceivers();
	}

	public RTCDataChannel getDataChannel() {
		return dataChannel;
	}

}
