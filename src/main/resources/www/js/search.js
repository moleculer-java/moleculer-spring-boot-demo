function search() {
	var query = document.getElementById("query").value;
	if (query === "") {
		return
	}	
	console.log("Invoking api/search/" + query + "...");
	var rest = new XMLHttpRequest();

	// "jmx.findObjects" URI mapped to "api/search/:query" Service in MoleculerApplication.java
	rest.open("GET", "api/search/" + query, true);
	rest.onload = function() {
		if (rest.readyState === rest.DONE && rest.status === 200) {
			console.log("Response: ", rest.responseText);
			var json =  JSON.parse(rest.responseText);
			document.getElementById("rsp").innerHTML = JSON.stringify(json, null, 4);
		}
	};
	rest.send("");
	
	// Focus on query-field
	document.getElementById("query").focus();
}

function clearResults() {
	document.getElementById("query").value = "";
	document.getElementById("rsp").innerHTML = "[empty]";
}

// Page loaded
window.addEventListener("load", function(event) {
	
	// Focus on query-field
	document.getElementById("query").focus();
});