function reloadClock() {
	var clock = document.getElementById("clock");
	
	// "serverSideImage.getImage" Action mapped to "api/clock" URL in MoleculerApplication.java
	clock.src = "api/clock?stamp=" + new Date().getTime();
}

window.addEventListener("load", function(event) {
	setInterval(reloadClock, 1000);
});