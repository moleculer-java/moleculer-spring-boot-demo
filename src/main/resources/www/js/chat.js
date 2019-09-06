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
	field = document.getElementById("user");
	var user = field.value;
	if (user === "") {
		return;
	}
	
	// Show text
	addMessage(id, user, text);
	
	// Invoke REST service (send positions and client ID to the APIGateway)
	var post = new XMLHttpRequest();
	
	// "chatService.sendMessage" Action mapped to "api/send" URL in ChatService.java
	post.open("POST", "api/send", true);
	post.setRequestHeader("Content-type", "application/json");
	post.send(JSON.stringify({
		id: id,
		user: user,
		text: text
	}));
	
	// Focus on message-field
	document.getElementById("message").focus();
}

function addMessage(id, user, text) {
	if (!id || !user || !text) {
		return;
	}	
	var msgNode = document.createElement("div");
	msgNode.classList.add(this.id == id ? "mine" : "yours");
	addDiv(msgNode, user, "sender");
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
	
	// "chatService.getHistory" Action mapped to "api/history" URL in ChatService.java
	rest.open("GET", "api/history", true);
	rest.onload = function() {
    	if (rest.readyState === rest.DONE && rest.status === 200) {
           	var root = JSON.parse(rest.responseText);
			console.log("History:", root);
			for (var i = 0; i < root.history.length; i++) {
				var data = root.history[i];
				addMessage(data.id, data.user, data.text);
			}
    	}
	};
	rest.send("");
	
	// Create WebSocket connection
	ws = MoleculerWebsocket("ws/chat", function(msg) {
		
		// Message received from server;
		// message contains the positions and clientID
		var data = JSON.parse(msg);
		if (data.id != id) {
			addMessage(data.id, data.user, data.text);
		}
		
	}, {
				
		// Set the WebSocket connection parameters
		heartbeatInterval: 15 * 1000,
		heartbeatTimeout: 5 * 1000,
		debug: true
		
	});
	ws.connect();
	
	// Generate random user name
	document.getElementById("user").value = generateRandomName();
	
	// Focus on message-field
	document.getElementById("message").focus();
});

// Handle disconnect
window.addEventListener("unload", function(event) {
	if (ws) {
		ws.disconnect();
		ws = null;
	}
});

// Generate random name
function generateRandomName() {
	var firstname = [
        "Johnathon",
        "Anthony",
        "Erasmo",
        "Raleigh",
        "Nancie",
        "Tama",
        "Camellia",
        "Augustine",
        "Christeen",
        "Luz",
        "Diego",
        "Lyndia",
        "Thomas",
        "Georgianna",
        "Leigha",
        "Alejandro",
        "Marquis",
        "Joan",
        "Stephania",
        "Elroy",
        "Zonia",
        "Buffy",
        "Sharie",
        "Blythe",
        "Gaylene",
        "Elida",
        "Randy",
        "Margarete",
        "Margarett",
        "Dion",
        "Tomi",
        "Arden",
        "Clora",
        "Laine",
        "Becki",
        "Margherita",
        "Bong",
        "Jeanice",
        "Qiana",
        "Lawanda",
        "Rebecka",
        "Maribel",
        "Tami",
        "Yuri",
        "Michele",
        "Rubi",
        "Larisa",
        "Lloyd",
        "Tyisha",
        "Samatha"
	];
    var lastname = [
        "Mischke",
        "Serna",
        "Pingree",
        "Mcnaught",
        "Pepper",
        "Schildgen",
        "Mongold",
        "Wrona",
        "Geddes",
        "Lanz",
        "Fetzer",
        "Schroeder",
        "Block",
        "Mayoral",
        "Fleishman",
        "Roberie",
        "Latson",
        "Lupo",
        "Motsinger",
        "Drews",
        "Coby",
        "Redner",
        "Culton",
        "Howe",
        "Stoval",
        "Michaud",
        "Mote",
        "Menjivar",
        "Wiers",
        "Paris",
        "Grisby",
        "Noren",
        "Damron",
        "Kazmierczak",
        "Haslett",
        "Guillemette",
        "Buresh",
        "Center",
        "Kucera",
        "Catt",
        "Badon",
        "Grumbles",
        "Antes",
        "Byron",
        "Volkman",
        "Klemp",
        "Pekar",
        "Pecora",
        "Schewe",
        "Ramage"
	];
    return firstname[Math.floor(Math.random() * firstname.length)] + " " + lastname[Math.floor(Math.random() * lastname.length)];
}