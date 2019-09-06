function addNumbers() {
	var a = document.getElementById("a").value;
	var b = document.getElementById("b").value;
	if (a === "" || b === "") {
		return
	}	
	console.log("Invoking api/add/" + a + "/" + b + "...");
	var rest = new XMLHttpRequest();

	// "math.add" Action mapped to "api/add/:a/:b" URL in
	// MoleculerApplication.java
	rest.open("GET", "api/add/" + a + "/" + b, true);
	rest.onload = function() {
		if (rest.readyState === rest.DONE && rest.status === 200) {
			var response = JSON.parse(rest.responseText);
			console.log("Response: ", response);
			document.getElementById("aspan").innerHTML = response.a;
			document.getElementById("bspan").innerHTML = response.b;
			document.getElementById("cspan").innerHTML = response.c;
		}
	};
	rest.send("");
}

// Numeric filter
function validate(evt) {
	var e = evt || window.event;
	if (e.type === 'paste') {

		// Handle paste
		key = event.clipboardData.getData('text/plain');
	} else {

		// Handle key press
		var key = e.keyCode || e.which;
		key = String.fromCharCode(key);
	}
	var regex = /[0-9]|\./;
	if (!regex.test(key)) {
		e.returnValue = false;
		if (e.preventDefault) {
			e.preventDefault();
		}
	}
}