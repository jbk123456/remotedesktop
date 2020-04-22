var mouseX;
var mouseY;
var px;
var py;
var serverSocket;

var docMouseX = 0;
var docMouseY = 0;

function setScroll(x, y) {
	dx = x - getScrollX();
	dy = y - getScrollY();
	window.scrollTo(x, y);
	mx = getDocMouseX() + dx;
	my = getDocMouseY() + dy;
	if (mx < 0)
		mx = getDocMouseX();
	if (my < 0)
		my = getDocMouseY();
	docMouseX = mx;
	docMouseY = my;
}

function getDocWidth() {
	if (document.documentElement)
		return document.documentElement.scrollWidth;
	else
		return document.body.scrollWidth;
}

function getDocHeight() {
	if (document.documentElement)
		return document.documentElement.scrollHeight;
	else
		return document.body.scrollHeight;
}

function getDocMouseX() {
	return docMouseX;
}

function getDocMouseY() {
	return docMouseY;
}

function getScrollX() {
	x = 0;
	if (document.body.scrollLeft)
		x = document.body.scrollLeft;
	else if (window.pageXOffset)
		x = window.pageXOffset;
	else if (document.body.parentElement)
		x = document.body.parentElement.scrollLeft;
	else
		x = 0;
	return x;
}

function getScrollY() {
	y = 0;
	if (document.body.scrollTop)
		y = document.body.scrollTop;
	else if (window.pageYOffset)
		y = window.pageYOffset;
	else if (document.body.parentElement)
		y = document.body.parentElement.scrollTop;
	else
		y = 0;
	return y;
}

function getWinWidth() {
	winWidth = 0;
	if (window.innerWidth)
		winWidth = window.innerWidth;
	if (document.body.offsetWidth)
		winWidth = document.body.offsetWidth;
	return winWidth;
}

function getWinHeight() {
	winHeight = 0;
	if (window.innerHeight)
		winHeight = window.innerHeight;
	if (document.body.offsetHeight)
		winHeight = document.body.offsetHeight;
	return winHeight;
}

function getWinMouseX() {
	return getDocMouseX() - getScrollX();
}

function getWinMouseY() {
	return getDocMouseY() - getScrollY();
}

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
	document.addEventListener('keydown', function(e) {
		    var mask = e.shiftKey?1:0 | e.ctrlKey?2:0 | e.altKey?4:0; 
			setTimeout(function() {callback(e.keyCode, mask);}, 1);
			stopPropagateEvent(e);
		});
}

function startMouseMoveListener(callback, opt) {
	// Provide a set of default options
	var default_options = {
		'type' : 'mousemove',
		'propagate' : false,
		'target' : document
	}
	if (!opt)
		opt = default_options;
	else {
		for ( var dfo in default_options) {
			if (typeof opt[dfo] == 'undefined')
				opt[dfo] = default_options[dfo];
		}
	}

	var ele = opt.target
	if (typeof opt.target == 'string')
		ele = document.getElementById(opt.target);
	var ths = this;

	// The function to be called at mouse move
	var func = function(e) {
		e = e || window.event;
		x = 0;
		y = 0;
		// Find mouse position
		if (e.pageX)
			x = e.pageX;
		if (e.clientX)
			x = document.body.scrollLeft + e.clientX;
		if (e.pageY)
			y = e.pageY;
		if (e.clientY)
			y = document.body.scrollTop + e.clientY;
		docMouseX = x;
		docMouseY = y;
		setTimeout(function() {
			callback(x, y);
		}, 1);

		// e.cancelBubble is supported by IE - this will kill the bubbling
		// process.
		e.cancelBubble = true;
		e.returnValue = false;

		// e.stopPropagation works only in Firefox.
		if (e.stopPropagation) {
			e.stopPropagation();
			e.preventDefault();
		}
		return false;
	}

	// Attach the function with the event
	if (ele.addEventListener)
		ele.addEventListener(opt['type'], func, false);
	else if (ele.attachEvent)
		ele.attachEvent('on' + opt['type'], func);
	else
		ele['on' + opt['type']] = func;
}

function startMouseButtonListener(callback, opt) {
	// Provide a set of default options
	var default_options = {
		'type' : 'mousedown',
		'type2' : 'mouseup',
		'propagate' : false,
		'target' : document
	}
	if (!opt)
		opt = default_options;
	else {
		for ( var dfo in default_options) {
			if (typeof opt[dfo] == 'undefined')
				opt[dfo] = default_options[dfo];
		}
	}

	var ele = opt.target
	if (typeof opt.target == 'string')
		ele = document.getElementById(opt.target);
	var ths = this;

	// The function to be called at mouse button press
	var func = function(e) {
		e = e || window.event;
		button = 0;

		// Find mouse button
		if (e.which)
			button = e.which;
		if (e.button)
			button = e.button;
		setTimeout(function() {
			callback('press', button);
		}, 1);

		// e.cancelBubble is supported by IE - this will kill the bubbling
		// process.
		e.cancelBubble = true;
		e.returnValue = false;

		// e.stopPropagation works only in Firefox.
		if (e.stopPropagation) {
			e.stopPropagation();
			e.preventDefault();
		}
		return false;
	}

	// The function to be called at mouse button release
	var func2 = function(e) {
		e = e || window.event;
		button = 0;
		// Find mouse button
		if (e.which)
			button = e.which;
		if (e.button)
			button = e.button;
		setTimeout(function() {
			callback('release', button);
		}, 1);

		// e.cancelBubble is supported by IE - this will kill the bubbling
		// process.
		e.cancelBubble = true;
		e.returnValue = false;

		// e.stopPropagation works only in Firefox.
		if (e.stopPropagation) {
			e.stopPropagation();
			e.preventDefault();
		}
		return false;
	}

	// Attach the function with the mouse down
	if (ele.addEventListener)
		ele.addEventListener(opt['type'], func, false);
	else if (ele.attachEvent)
		ele.attachEvent('on' + opt['type'], func);
	else
		ele['on' + opt['type']] = func;
	// Attach the function with the mouse up
	if (ele.addEventListener)
		ele.addEventListener(opt['type2'], func2, false);
	else if (ele.attachEvent)
		ele.attachEvent('on' + opt['type2'], func2);
	else
		ele['on' + opt['type2']] = func2;

}

function startWindowResizeListener(callback, opt) {
	// Provide a set of default options
	var default_options = {
		'type' : 'resize',
		'propagate' : false,
		'target' : window
	}
	if (!opt)
		opt = default_options;
	else {
		for ( var dfo in default_options) {
			if (typeof opt[dfo] == 'undefined')
				opt[dfo] = default_options[dfo];
		}
	}

	var ele = opt.target
	if (typeof opt.target == 'string')
		ele = document.getElementById(opt.target);
	var ths = this;

	// The function to be called at mouse move
	var func = function(e) {
		e = e || window.event;
		x = 0;
		y = 0;
		// Find window size
		if (e.x)
			x = e.x;
		if (e.y)
			y = e.y;
		setTimeout(function() {
			callback(x, y);
		}, 1);

		// e.cancelBubble is supported by IE - this will kill the bubbling
		// process.
		e.cancelBubble = true;
		e.returnValue = false;

		// e.stopPropagation works only in Firefox.
		if (e.stopPropagation) {
			e.stopPropagation();
			e.preventDefault();
		}
		return false;
	}

	// Attach the function with the event
	if (ele.addEventListener)
		ele.addEventListener(opt['type'], func, false);
	else if (ele.attachEvent)
		ele.attachEvent('on' + opt['type'], func);
	else
		ele['on' + opt['type']] = func;
}

function sendCtrl(url) {
	serverSocket.send("GET /" + url + "\r\n\r\n");
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

function load() {

	serverSocket = new WebSocket("ws://" + REMOTEDESKTOPHOST);
	serverSocket.onmessage = function(event) {
		//console.log(event.data);
		setTimeout(function() {eval(event.data);}, 1);

	}

	startKeyListener(keyHandler);
	startMouseMoveListener(mousemoveHandler);
	startMouseButtonListener(mousebuttonHandler);

	setTimeout('javascript:updateMouse();', 250);
}

function keyHandler(code, mask) {
	sendCtrl("sendKey?key=" + code + "&mask=" + mask+ "&c=" + Math.random());
}

function mousemoveHandler(x, y) {

	mouseX = x;
	mouseY = y;
}

function mousebuttonHandler(act, button) {
	sendCtrl("sendMouse?x=" + mouseX + "&y=" + mouseY + "&act=" + act
			+ "&button=" + button + "&c=" + Math.random());
}
