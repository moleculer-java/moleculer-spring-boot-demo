// --- VARIABLES ---

// Server-independent (Netty or J2EE) WebSocket connection
var ws;

// --- FUNCTIONS ---

// Handle connect
window.addEventListener("load", function(event) {

	// Invoke REST service (get initial data)
	var rest = new XMLHttpRequest();
	rest.open("GET", "jmx/all", true);
	rest.onload = function() {
    	if (rest.readyState === rest.DONE) {
        	if (rest.status === 200) {
            	var array = JSON.parse(rest.responseText);
            	console.log("Initial data:", array);
            	document.getElementById("memory").innerHTML   = array[0].HeapMemoryUsage.used;
				document.getElementById("loaded").innerHTML   = array[1].LoadedClassCount;
				document.getElementById("total").innerHTML    = array[1].TotalLoadedClassCount;
				document.getElementById("unloaded").innerHTML = array[1].UnloadedClassCount;
				document.getElementById("date").innerHTML     = array[2].data;            	
        	}
    	}
	};
	rest.send("");

	// WebSocket connect
	ws = MoleculerWebsocket("ws/jmx", function(msg) {
		
		// Message received from server;
		// message contains the memory usage in bytes
		var packet = JSON.parse(msg);
		if (packet.type == "memory") {
			document.getElementById("memory").innerHTML = packet.data;
			return;
		}
		if (packet.type == "classloader") {
			document.getElementById("loaded").innerHTML   = packet.LoadedClassCount;
			document.getElementById("total").innerHTML    = packet.TotalLoadedClassCount;
			document.getElementById("unloaded").innerHTML = packet.UnloadedClassCount;
			return;
		}
		if (packet.type == "date") {
			document.getElementById("date").innerHTML = packet.data;
			return;
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