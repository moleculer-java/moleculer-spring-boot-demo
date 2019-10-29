function clearCache() {
	console.log("Invoking api/clear...");
	var rest = new XMLHttpRequest();

	// "blog.clear" Action mapped to "api/clear" URL in MoleculerApplication.java
	rest.open("GET", "../api/clear" + name, true);
	rest.onload = function() {
		if (rest.readyState === rest.DONE && rest.status === 200) {
			alert("Cache cleared!");
			location.reload();
		}
	};
	rest.send("");
}