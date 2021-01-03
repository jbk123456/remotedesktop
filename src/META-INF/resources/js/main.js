/*-*- mode: JavaScript; tab-width:2 -*-*/

'use strict';

var serverSocket;


function startVisibilityListener(callback) {
    document.addEventListener("visibilitychange", callback, false);
}

function connectToServer() {
    serverSocket = new WebSocket("ws://" + REMOTEDESKTOPHOST);
		async function getBackbufferFromEvent(event) {
				//return event;

				var data = await event.data.arrayBuffer();
				return new Uint8ClampedArray(data);
		}    


    serverSocket.onmessage = readDataClosure();

}
function load() {

    connectToServer();
    var canvas = document.getElementById("canvas");
    startKeyListener(keyHandler);
    startMouseMoveListener(canvas, mousemoveHandler);
    startMouseButtonListener(canvas, mousebuttonHandler);

    startVisibilityListener(visibilityListener);

    setTimeout('javascript:updateMouse();', REMOTEDESKTOPUPDATEMOUSEDELAY);
}

function visibilityListener() {
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

