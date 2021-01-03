'use strict';

function str2ab(str) {
  var buf = new ArrayBuffer(str.length);
  var bufView = new Uint8Array(buf);
  for (var i=0, strLen=str.length; i<strLen; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return buf;
}

//console.log(new Uint8ClampedArray(str2ab("123abc")));

function _(str) {
    return  new Uint8ClampedArray(str2ab(str));
}
    function concat (a, b) {
	var c = new Uint8ClampedArray(a.length + b.length);
	c.set(a);
	c.set(b, a.length);
	return c;
    }

function getData(event, from, to) {
    //return new Uint8ClampedArray(await event.data.arrayBuffer())
    console.log(event);
    return event.subarray(from, to);
}
function getBackbufferFromEvent(event) {
    //return new Uint8ClampedArray(await event.data.arrayBuffer())
    return event;
}    

function readDataClosure() {
    var backbuffer = new Uint8ClampedArray();
    var values = [];
    var bodyLength = -1;

    function concat (a, b) {
	var c = new Uint8ClampedArray(a.length + b.length);
	c.set(a);
	c.set(b, a.length);
	return c;
    }
    function appendToBackbuffer(event) {
	backbuffer = concat(backbuffer, getBackbufferFromEvent(event));
    }
    function parseHeaderLine(n) {
	var line = '';
	for(var i=n; i<backbuffer.length; i++) {
    	    if (backbuffer[i]==13&&backbuffer[i+1]==10) {
      		return line; 
	    }
	    line+=String.fromCharCode(backbuffer[i]);
	}
	return null;
    }

    function parseHeaders() {
	var pos = 0;
	while(true) {
	    var line = parseHeaderLine(pos);
	    if(line==null) {

		break;
	    }
	    pos+=line.length+2;
	    if (line.length==0) { // end of header
		return pos;
	    }
	    if (line.startsWith("PUT")) {// PUT /tile?
		var pairs = line.substring(10).split("&");
	    	for (var p in pairs) {
		    var ar = pairs[p].split("=");
		    values[ar[0].trim()]=ar[1].trim();
	    	}

	    }
	    
	    if (line.startsWith("Content-Length")) {
	   	bodyLength = parseInt(line.split(":")[1].trim());
	    }

	}
	return -1;  // no complete header yet
    }
    function parseBody(pos) {
	if (pos+bodyLength>backbuffer.length) {
	    return null;
	}
	return backbuffer.slice(pos, pos+bodyLength);
    }

    return function onmessage(event) {
	appendToBackbuffer(event);
	while(true) {
	    var headerLength = parseHeaders();
	    if (headerLength==-1) {
		return;
	    }
	    var body = parseBody(headerLength);
	    if (body==null) {
		return;
	    }

	    backbuffer = backbuffer.slice(headerLength+bodyLength);
	    console.log("result:::", headerLength, bodyLength, backbuffer.length);
	}
    }
}

let f = readDataClosure();
var testdata=_("PUT /file?x=1&y=1&w=10&h=11\r\nContent-Length: 3\r\n\r\n123");
var testdata0=_("");
var testdata1=_("PUT /file?x=1&y=1&w=10&h=11\r\nContent-Length: ");
var testdata2=_("3\r");
var testdata3=_("\n");
var testdata4=_("\r");
var testdata5=_("\n1");
var testdata6=_("2");
var testdata7=_("3XXXX");

f(concat(testdata,testdata0))
f(testdata1)
f(testdata2)
f(testdata3)
f(testdata4)
f(testdata5)
f(testdata6)
f(testdata7)
