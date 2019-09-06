var timerID = null;

function startUploading() {
	document.getElementById("fileToUpload").style.display = "none";
	document.getElementById("status").innerHTML = "Upload has started. Please wait...";
	if (timerID == null) {
		timerID = setTimeout(displayUploadedBytes, 1000);
	}
	document.getElementById("uploadForm").submit();
}

function displayUploadedBytes() {
	var rest = new XMLHttpRequest();
	
	// "upload.getUploadCount" Action mapped to "api/count" URL in
	// FileUpload.java Service (using annotation)
	rest.open("GET", "api/count", true);
	rest.onload = function() {
		if (rest.readyState === rest.DONE) {
			if (rest.status === 200) {
				var response = JSON.parse(rest.responseText).toString();
				var status = document.getElementById("status");
				status.innerHTML = response
						+ " bytes uploaded to the server. Please wait...";
			}
			setTimeout(displayUploadedBytes, 1000);
		}
	};
	rest.send("");
}