function hello() {
	var name = document.getElementById("name").value;
	if (name === "") {
		return
	}
	console.log("Invoking api/hello/" + name + "...");
	var rest = new XMLHttpRequest();

	// "greeter.hello" Action mapped to "api/hello/:name" URL in MoleculerApplication.java
	rest.open("GET", "api/hello/" + name, true);
	rest.onload = function() {
		if (rest.readyState === rest.DONE && rest.status === 200) {
			console.log("Response: ", rest.responseText);
			document.getElementById("rsp").innerHTML = rest.responseText;
		}
	};
	rest.send("");
}