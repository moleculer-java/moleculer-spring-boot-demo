// --- VARIABLES ---

// Server-independent (Netty or J2EE) WebSocket connection
var ws;

// Canvas object
var canvas = document.querySelector("canvas");

// 2D content of the canvas
var context = canvas.getContext("2d");

// Drawing?
var drawing = false;

// Last mouse position
var pos = {};

// Unique client (browser) ID
var id = Math.random().toString(36).substring(2, 15);

// Previous line width
var previousLineWidth = 2;

// --- FUNCTIONS ---

// Handle resize
function resize() {
	canvas.width = window.innerWidth;
	canvas.height = window.innerHeight;
}
resize();
window.addEventListener("resize", resize, false);

// Handle connect
window.addEventListener("load", function(event) {
	ws = MoleculerWebsocket("/ws/drawing", function(msg) {
		
		// Message received from server;
		// message contains the positions and clientID
		var data = JSON.parse(msg);
		if (data.id != id) {
			var w = canvas.width;
			var h = canvas.height;
			draw(data.x1 * w, data.y1 * h, data.x2 * w, data.y2 * h, data.id);
		}
		
	}, {
				
		// Set the WebSocket connection parameters
		heartbeatInterval: 5 * 1000,
		heartbeatTimeout: 1 * 1000,
		debug: true
		
	});
	ws.connect();
});

// Handle disconnect
window.addEventListener("unload", function(event) {
	if (ws) {
		ws.disconnect();
		ws = null;
	}
});

// Handle mouse down event       
canvas.addEventListener("mousedown", function(event) {
	drawing = true;
	pos.x = event.clientX;
	pos.y = event.clientY;
});

// Drawing
function draw(x1, y1, x2, y2, str) {

	// Start drawing...
	context.beginPath();
	context.moveTo(x1, y1);
	context.lineTo(x2, y2);	

	// Generate color code from "client ID"
    var hash = 0;
    for (var i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    var colour = "#";
    for (var i = 0; i < 3; i++) {
        var value = (hash >> (i * 8)) & 0xFF;
        colour += ("00" + value.toString(16)).substr(-2);
    }
	context.strokeStyle = colour;
	
	// Calculate line with by the "speed" of the pointer
	var lineWidth = 3 * Math.log(Math.abs(x1 - x2) + Math.abs(y1 - y2)) + 1;
	context.lineWidth = (lineWidth + previousLineWidth) / 2;
	previousLineWidth = context.lineWidth;
	
	// End of drawing
	context.lineCap = "round";
	context.stroke();
	context.closePath();
}

// Handle mouse move event
canvas.addEventListener("mousemove", function(event) {
	if (!drawing) {
		return;
	}
	var w = canvas.width;
	var h = canvas.height;

	// Invoke REST service (send positions and client ID to the APIGateway)
	// "drawing" URI mapped to "drawing.send" Service in MoleculerApplication.java
	var post = new XMLHttpRequest();
	post.open("POST", "drawing", true);
	post.setRequestHeader("Content-type", "application/json");
	post.send(JSON.stringify({
		id: id,
		x1: pos.x / w,
		y1: pos.y / h,
		x2: event.clientX / w,
		y2: event.clientY / h
	}));

	// Drawing to the local canvas
	draw(pos.x, pos.y, event.clientX, event.clientY, id);

	pos.x = event.clientX;
	pos.y = event.clientY;
	
}, false);

// Handle mouse up event
canvas.addEventListener("mouseup", function(event) {
	drawing = false;
}, false);

// Handle mouse out event
canvas.addEventListener("mouseout", function(event) {
	drawing = false;
}, false);