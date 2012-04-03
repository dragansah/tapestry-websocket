Tapestry.Initializer.initChat = function(spec) {

	this.websocket = new WebSocket(spec.url);

	var websocketSupportEl = document.getElementById('websocket-support')
	if (typeof (WebSocket) != "function") {
		websocketSupportEl.setAttribute('class', "does-not-support-web-sockets");
		websocketSupportEl.innerHTML = "Your browser does not support web sockets";
	} else{
		websocketSupportEl.setAttribute('class', "supports-web-sockets");
		websocketSupportEl.innerHTML = "Your browser supports web sockets";
	}

	var usernameTextField = document.getElementById('username-textfield');
	var chatTextField = document.getElementById('chat-textfield');
	usernameTextField.focus();

	chatTextField.onkeypress = function(event) {
		if (event.keyCode == 13) { // enter pressed
			if (this.value != "")
				websocket.send("message/" + this.value + "/" + window.username);
			this.value = "";
			return false; // stop form submission
		}
	}

	usernameTextField.onkeypress = function(event) {
		if (event.keyCode == 13) { // enter pressed
			if (this.value != "") {
				window.username = this.value; // needed for logout
				websocket.send("login/" + username);
				document.getElementById('chat-area').style.visibility = 'visible';
				chatTextField.focus();
				this.style.visibility = 'hidden';
			}
			this.value = "";
			return false; // stop form submission
		}
	}

	websocket.onopen = function(evt) {
		console.log("open:" + evt.data);
	};

	websocket.onclose = function(evt) {
		console.log("close:" + evt.data);
	};

	websocket.onmessage = function(evt) {
		console.log("message:" + evt.data);

		json = JSON.parse(evt.data);

		// add a user to the users list
		if (json.loginUser != undefined) {
			var user = document.createElement('div');
			user.setAttribute('class', 'user');
			user.innerHTML = json.loginUser;
			document.getElementById('users').appendChild(user);
		}

		// remove a user from the users list
		if (json.logoutUser != undefined) {
			var users = document.getElementById('users');
			var children = users.childNodes;
			for (i = 0; i < children.length; i++) {
				var child = children[i];
				if (child.innerHTML == json.logoutUser)
					users.removeChild(child);
			}
		}

		// render the message received from some user
		if (json.message != undefined) {
			var messages = document.getElementById('messages');
			var message = JSON.parse(json.message);

			var timestamp = document.createElement('div');
			timestamp.setAttribute('class', 'msg msg-timestamp');
			timestamp.innerHTML = '[' + message.timestamp + ']';
			var username = document.createElement('div');
			username.setAttribute('class', 'msg msg-username');
			username.innerHTML = '[' + message.username + ']:';
			var content = document.createElement('div');
			content.setAttribute('class', 'msg msg-content');
			content.innerHTML = message.content;

			var msg = document.createElement('div');
			msg.setAttribute('class', 'msg msg-holder');
			msg.appendChild(timestamp);
			msg.appendChild(username);
			msg.appendChild(content);
			messages.appendChild(msg);

			// scroll to bottom in the case of overflow
			messages.scrollTop = messages.scrollHeight;
		}
	};

	window.onbeforeunload = function() {
		if (window.username != undefined)
			websocket.send("logout/" + username);
	}

	websocket.onerror = function(evt) {
		console.log("error:" + evt.data);
	};
}