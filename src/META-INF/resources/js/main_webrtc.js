/* -*- mode: JavaScript; tab-width: 4 -*- */

'use strict';

var configuration = {
	'iceServers': [{
		'urls': 'stun:stun.l.google.com:19302' //'stun:stun.l.google.com:19302'
	}]
};

//configuration = null;

var isInitiator;
var serverSocket; // wss to signal server
var selectors = {};
var socket = function () { };
var peerConn;
var dataChannel;
var load = function () {
    connectToServer();
	let canvas = document.getElementById("canvas");
	startKeyListener(keyHandler);
	startMouseMoveListener(canvas, mousemoveHandler);
	startMouseButtonListener(canvas, mousebuttonHandler);

	startVisibilityListener(visibilityListener);

	setTimeout('javascript:updateMouse();', REMOTEDESKTOPUPDATEMOUSEDELAY);

	serverSocket.onopen = function () {
		socket.emit('webrtcloaded');
	}

	socket.on = function (selector, f) {
		selectors[selector] = f;
	}
	socket.emit = function (msg) {
		serverSocket.send("GET /" + msg + "\r\n\r\n");
	}
	serverSocket.onmessage = function (e) {
		console.log("got message:::", e.data);

		var idx0 = e.data.indexOf(" ");
		var idx = e.data.indexOf("?");
		var k = e.data.substring(idx0 + 2, idx);
		var v = e.data.substring(idx + 3);
		selectors[k](v);
	}

	socket.on('created', function (id) {
		isInitiator = true;
		createPeerConnection(isInitiator, configuration);
	});

	socket.on('sdpb', function (message) {
		message = JSON.parse(atob(message));
		console.log('Client received message:', message);
		signalingMessageCallback(message);
	});

	socket.on('iceb', function (message) {
		message = JSON.parse(atob(message));
		console.log('Client received message:', message);
		signalingMessageCallback(message);
	});

	socket.on('message', function (message) {
		message = JSON.parse(atob(message));
		console.log('Client received message:', message);
		signalingMessageCallback(message);
	});


	socket.on('disconnect', function (reason) {
		console.log(`Disconnected: ${reason}.`);
	});
};

function sendMessage(tag, message) {
	console.log('Client sending message: ', tag, message);
	var c = "";
	for (var k in message) { c += (c.length ? "&" : "?") + k + "=" + btoa(message[k]) }
	socket.emit(tag + c);
	//  console.log('Client sent message: ', (tag+c));
}


function signalingMessageCallback(message) {
	if (message.type === 'offer') {
		console.log('Got offer. Sending answer to peer.');
		peerConn.setRemoteDescription(new RTCSessionDescription(message), function () { },
			logError);
		peerConn.createAnswer(onLocalSessionCreated, logError);

	} else if (message.type === 'answer') {
		console.log('Got answer.');
		peerConn.setRemoteDescription(new RTCSessionDescription(message), function () { },
			logError);

	} else if (message.type === 'candidate') {
		console.log("got candidate");
		peerConn.addIceCandidate(new RTCIceCandidate({
			candidate: message.candidate,
			sdpMLineIndex: message.label,
			sdpMid: message.id
		}));

	}
}

function createPeerConnection(isInitiator, config) {
	console.log('Creating Peer connection as initiator?', isInitiator, 'config:',
		config);
	peerConn = new RTCPeerConnection(config);

	// send any ice candidates to the other peer
	peerConn.onicecandidate = function (event) {
		console.log('icecandidate event:', event);
		if (event.candidate) {
			sendMessage("ice", {
				type: 'candidate',
				label: event.candidate.sdpMLineIndex,
				id: event.candidate.sdpMid,
				candidate: event.candidate.candidate
			});
		} else {
			console.log('End of candidates.');
		}
	};

	if (isInitiator) {
		console.log('Creating Data Channel');
		dataChannel = peerConn.createDataChannel('dc');
		onDataChannelCreated(dataChannel);

		console.log('Creating an offer');
		peerConn.createOffer().then(function (offer) {
			return peerConn.setLocalDescription(offer);
		})
			.then(() => {
				console.log('sending local desc:', peerConn.localDescription);
				sendMessage("sdp", peerConn.localDescription);
			})
			.catch(logError);

	} else {
		peerConn.ondatachannel = function (event) {
			console.log('ondatachannel:', event.channel);
			dataChannel = event.channel;
			onDataChannelCreated(dataChannel);
		};
	}
}

function onLocalSessionCreated(desc) {
	console.log('local session created:', desc);
	peerConn.setLocalDescription(desc).then(function () {
		console.log('sending local desc:', peerConn.localDescription);
		sendMessage("sdp", peerConn.localDescription);
	}).catch(logError);
}

function onDataChannelCreated(channel) {
	console.log('onDataChannelCreated:', channel);

	channel.onopen = function () {
		console.log('CHANNEL opened!');
	};

	channel.onclose = function () {
		console.log('Channel closed.');
	}

	channel.onmessage = readDataClosure();
}

function visibilityListener(ev) {
	if (document.visibilityState == 'hidden') {
		if (serverSocket) {
			serverSocket.close();
		}
		//serverSocket = null;
	} else {
		if (serverSocket) {
			serverSocket.close();
		}
		connectToServer();
	}
	return true;
}
function connectToServer() {
	serverSocket = new WebSocket("wss://" + REMOTEDESKTOPHOST);
}
function logError(err) {
	if (!err) return;
	if (typeof err === 'string') {
		console.warn(err);
	} else {
		console.warn(err.toString(), err);
	}
}

