// --- VARIABLES ---

// Server-independent (Netty or J2EE) WebSocket connection
var ws;

// Unique client (browser) ID
var id = Math.random().toString(36).substring(2, 15);

// --- FUNCTIONS ---

// Send message
function send() {

	// Get text
    var field = document.getElementById("message");
	var text = field.value;
	if (text === "") {
		return;
	}
	field.value = "";
	
	// Show text
	addMessage(id, text);
	
	// Invoke REST service (send positions and client ID to the APIGateway)
	var post = new XMLHttpRequest();
	post.open("POST", "chatService.sendMessage", true);
	post.setRequestHeader("Content-type", "application/json");
	post.send(JSON.stringify({
		id: id,
		text: text
	}));
}

function addMessage(id, text) {
	if (!id || !text) {
		return;
	}	
	var msgNode = document.createElement("div");
	msgNode.classList.add(this.id == id ? "mine" : "yours");
	addDiv(msgNode, id, "sender");
	addDiv(msgNode, text, "content");
		
	var history = document.getElementById("history");
	if (history) {
		history.appendChild(msgNode);
		history.appendChild(document.createElement("br"));
		while (history.childElementCount > 20) {
			history.removeChild(history.childNodes[0]);
		}
	}
}

function addDiv(root, text, style) {
	var node = document.createElement("div");
	node.classList.add(style);
	node.appendChild(document.createTextNode(text));
	root.appendChild(node);
}

// Handle connect
window.addEventListener("load", function(event) {

	// Invoke REST service (get initial data)
	var rest = new XMLHttpRequest();
	rest.open("GET", "chatService.getHistory", true);
	rest.setRequestHeader("Content-type", "application/json");
	rest.onload = function() {
    	if (rest.readyState === rest.DONE && rest.status === 200) {
           	var root = JSON.parse(rest.responseText);
			console.log("History:", root);
			for (var i = 0; i < root.history.length; i++) {
				var data = root.history[i];
				addMessage(data.id, data.text);
			}
    	}
	};
	rest.send("");
	
	// Create WebSocket connection
	ws = MoleculerWebsocket("/ws/chat", function(msg) {
		
		// Message received from server;
		// message contains the positions and clientID
		var data = JSON.parse(msg);
		if (data.id != id) {
			addMessage(data.id, data.text);
		}
		
	}, {
				
		// Set the WebSocket connection parameters
		heartbeatInterval: 5 * 1000,
		heartbeatTimeout: 1 * 1000,
		debug: true
		
	});
	ws.connect();
	
	// Focus to textfield
	document.getElementById("message").focus();
});

// Handle disconnect
window.addEventListener("unload", function(event) {
	if (ws) {
		ws.disconnect();
		ws = null;
	}
});