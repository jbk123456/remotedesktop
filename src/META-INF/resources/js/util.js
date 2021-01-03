/*-*- mode: JavaScript; tab-width:2 -*-*/

'use strict';

function readDataClosure() {
    var backbuffer = new Uint8ClampedArray();
    var props = [];
    var bodyLength = -1;

    function concat (a, b) {
				var c = new Uint8ClampedArray(a.length + b.length);
				c.set(a);
				c.set(b, a.length);
				return c;
    }
    function appendToBackbuffer(buf) {
				backbuffer = concat(backbuffer, buf);
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
						console.log("line:::" + line);
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
										props[ar[0].trim()]=parseInt(ar[1].trim());
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

    function parseData(arrayBuffer) {
				appendToBackbuffer(new Uint8ClampedArray(arrayBuffer));
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
						//console.log("result:::", headerLength, bodyLength, backbuffer.length);
						renderTile(props, body);
				}
    }

    return function onmessage(event) {
				withBackbuffer(event, parseData);
    }
}

function withBackbuffer(event, succ) {
    //succ(event);

    event.data.arrayBuffer().then(succ);
}    


function renderTile(props, data) {
    var x = props["x"];
    var y = props["y"]
    var w = props["w"];
    var h = props["h"];
		
    var img = document.getElementById("c"+x+"_"+y);
		img.width = w;
		img.height = h;
		var blob = new Blob([data]);
		var reader = new FileReader();
		reader.onload=function(event) {
				img.src = event.target.result;
		}
		reader.readAsDataURL(blob);

		//var dataurl= btoa(String.fromCharCode.apply(null, data));
		//img.src='data:image/jpg;base64,'+dataurl;
		
    // var canvas = document.getElementById("c"+x+"_"+y);
    // if (canvas.width!= w) {
		// 		canvas.width = w;
    // }
    // if (canvas.height!=h) {
		// 		canvas.height=h;
    // }
    // var context = canvas.getContext('2d');
    // var img = context.createImageData(w, h);
    // img.data.set(data);
    // context.putImageData(img, 0, 0);
}

