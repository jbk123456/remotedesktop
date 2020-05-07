var mouseX;
var mouseY;
var px;
var py;
var serverSocket;

function stopPropagateEvent(e) {
	e.cancelBubble = true;
	e.returnValue = false;

	// e.stopPropagation works only in Firefox.
	if (e.stopPropagation) {
		e.stopPropagation();
		e.preventDefault();
	}
}

function startKeyListener(callback) {
	document.body.addEventListener('keydown', function(e) {
		    var mask = e.shiftKey?1:0 | e.ctrlKey?2:0 | e.altKey?4:0; 
		    //console.log("KEY:::", e.keyCode, e.key, e.code );
			stopPropagateEvent(e);
			setTimeout(function() {callback(e.key, e.keyCode, mask);}, 1);

			return true;
		});
}

function startVisibilityListener(callback) {
	  document.addEventListener("visibilitychange", callback, false);
}

function startMouseMoveListener(callback) {
	
	document.body.addEventListener("mousemove",  e => {

		stopPropagateEvent(e);
		
		var x = document.body.scrollLeft + e.clientX;
		var y = document.body.scrollTop + e.clientY;

		setTimeout(function() {
			callback(x, y);
		}, 1);
		
      return true;
	});
}

function startMouseButtonListener(callback) {
	var buttons = e.buttons;

	document.body.addEventListener("mousedown", e=> {

		stopPropagateEvent(e);

		setTimeout(function() {
			callback('press', buttons);
		}, 1);


		return true;
	});
	document.body.addEventListener("mouseup", e=> {

		stopPropagateEvent(e);

		setTimeout(function() {
			callback('release', buttons);
		}, 1);

		return true;
	});

}

function sendCtrl(url) {
	if(document.visibilityState !== 'hidden') {
		serverSocket.send("GET /" + url + "\r\n\r\n");
	}
}

function updateMouse() {

	if (px != mouseX || py != mouseY) {
		sendCtrl("sendMouse?x=" + mouseX + "&y=" + mouseY
				+ "&act=none&button=0&c=" + Math.random());
		px = mouseX;
		py = mouseY;
	}
	setTimeout('javascript:updateMouse();', 250);
}

function connectToServer() {
	console.log("connect to server");
	serverSocket = new WebSocket("ws://" + REMOTEDESKTOPHOST);
	serverSocket.onmessage = function(event) {
		//console.log(event.data);
		setTimeout(function() {eval(event.data);}, 1);

	}
}
function load() {

	connectToServer();
	startKeyListener(keyHandler);
	startMouseMoveListener(mousemoveHandler);
	startMouseButtonListener(mousebuttonHandler);

	startVisibilityListener(visibilityListener);
	
	setTimeout('javascript:updateMouse();', 250);
}

function visibilityListener(ev) {
	if(document.visibilityState == 'hidden') {
    	if (serverSocket) {
    		console.log("close socket");
    		serverSocket.close();
    	}
    	console.log("socket closed");
    	//serverSocket = null;
    } else {
    	if (serverSocket) {
    		serverSocket.close();
    	}
		console.log("open socket");
		connectToServer();
    }
    return true;
}


function keyHandler(key, code, mask) {
	sendCtrl("sendKey?key=" + key.charCodeAt(0) +"&code=" + code + "&mask=" + mask+ "&c=" + Math.random());
    return true;
}

function mousemoveHandler(x, y) {
	mouseX = x;
	mouseY = y;
}

function mousebuttonHandler(act, button) {
	sendCtrl("sendMouse?x=" + mouseX + "&y=" + mouseY + "&act=" + act
			+ "&button=" + button + "&c=" + Math.random());
}
