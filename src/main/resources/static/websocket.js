// ---------------- SAMPLE ----------------------
//
// Create websocket-handler instance: 
// var ws = MoleculerWebsocket("ws/chat", function(msg) {
//      console.log("Message received:", msg);
// }, {
// 	heartbeatInterval: 5 * 1000,
// 	debug: true,
// 	onopen: function() {
//      console.log("Websocket opened.");
// 	},
// 	onreconnect: function() {
//      console.log("Websocket reconnecting...");
// 	},
// 	onclose: function() {
//      console.log("Websocket closed.");
// 	},
// });
// 
// Connect to Moleculer-Java server (or servlet):
// ws.connect();
// 
// Disconnect from Moleculer-Java server (or servlet):
// ws.disconnect();
// 
function MoleculerWebsocket(channel, handler, opts) {

	if (!channel) {
		channel = "ws/common";
	}

	var webSocket = null;
	var heartbeatTimer = null;

	var submittedAt = 0;
	var receivedAt = 0;

	opts = opts || {};

	var heartbeatInterval = opts.heartbeatInterval != null ? opts.heartbeatInterval : 6 * 1000;
	var heartbeatTimeout = opts.heartbeatTimeout != null ? opts.heartbeatTimeout : 1 * 1000;

	function absolutize(url) {
		var a = document.createElement("a");
		a.style.display = "none";
		document.body.appendChild(a);
		a.href = url;
		return a.href;
	}
	
	function connect() {
		var url = absolutize(channel);
		url = url.replace(/^http/, "ws");
		if (opts.debug && console) { 
			console.log("Connecting to " + url + "...");
		}
		webSocket = new WebSocket(url);
		webSocket.onopen = function (evt) {
			if (opts.debug && console) { 
				console.log("WebSocket channel opened.");
			}
			if (opts.onopen) {
				opts.onopen(evt);
			}
			receivedAt = submittedAt = new Date().valueOf();				
			if (heartbeatInterval > 0 && heartbeatTimeout > 0) {
				startHeartbeatTimer();
			}
		}

		webSocket.onmessage = function(evt) {
			var msg = evt.data;
			if (msg == "!") {
				receivedAt = new Date().valueOf();
				if (opts.debug && console) {
					console.log("Heartbeat response received from server.");
				}				
				return;
			}	    
			if (opts.debug && console) { 
				console.log("WebSocket message received: ", msg);
			}
			if (handler) { 
				handler(msg);
			}
		}

		webSocket.onerror = function (evt) { 
			onConnectionLost();
		}

		webSocket.onclose = function (evt) {
			if (opts.debug && console) {
				console.log("WebSocket channel closed.");
			}
			if (opts.onclose) {
				opts.onclose();
			}
		}
	}

	function disconnect() {
		if (webSocket) {
			if (opts.debug && console) { 
				console.log("WebSocket channel disconnecting...");
			}
			webSocket.close();
			webSocket = null;
		}
	}

	function startHeartbeatTimer() {
		stopHeartbeatTimer();
		heartbeatTimer = setInterval(function() {		
			var now = new Date().valueOf();
			if (now - submittedAt > heartbeatInterval) {
				if (opts.debug && console) {
					console.log("Sending heartbeat message to server...");
				}
				submittedAt = now;
				webSocket.send("!");
				return;
			}
			if ((submittedAt - receivedAt) >= heartbeatTimeout
					&& (now - submittedAt) >= heartbeatTimeout) {
				if (opts.debug && console) {
					console.log("Heartbeat response message timeouted.");
				}
				onConnectionLost();		        	
			}        
		}, heartbeatInterval / 3);
		if (opts.debug && console) {
			console.log("Websocket heartbeat timer started.");
		}
	}

	function stopHeartbeatTimer() {
		if (heartbeatTimer != null) {
			clearInterval(heartbeatTimer);
			heartbeatTimer = null;
			if (opts.debug && console) {
				console.log("Websocket heartbeat timer stopped.");
			}
		}	
	}

	function onConnectionLost() {
		if (!webSocket) {
			return;
		}
		if (opts.debug && console) {
			console.log("WebSocket connection lost.");
		}
		disconnect();
		stopHeartbeatTimer();
		setTimeout(function() {
			if (opts.onreconnect) {
				opts.onreconnect();
			}
			connect();
		}, 3000);	
	}

	return {
		channel: channel,
		connect: connect,
		disconnect: disconnect
	}
	
}