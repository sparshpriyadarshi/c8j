"use strict";

console.log("index js loads");

const DISPLAY_W = 64
const DISPLAY_H = 32
const DISPLAY_SCALE = 10
const OFF_RGB = { R:0,G:0,B:0}
const ON_RGB = { R:200,G:220,B:255}
const EMU_STATUSES = {
	STARTED: "STARTED",
	STOPPED: "STOPPED"
};
function Initialize() {
	const canvas = document.getElementById("main-canvas");
	canvas.width = DISPLAY_W * DISPLAY_SCALE;
	canvas.height = DISPLAY_H * DISPLAY_SCALE;
    const ctx = canvas.getContext("2d");

	//BG
	ctx.fillStyle = `rgb(
        ${OFF_RGB.R},
        ${OFF_RGB.G},
        ${OFF_RGB.B}
        )`;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

	//FG
	ctx.fillStyle = `rgb(
        ${ON_RGB.R},
        ${ON_RGB.G},
        ${ON_RGB.B}
        )`;
    ctx.fillRect(12, 12, DISPLAY_SCALE, DISPLAY_SCALE);

	const status = document.getElementById("player-status-text");
	status.innerText = `STATUS: ${EMU_STATUSES.STOPPED}`;

	const startButton = document.getElementById("start-button");
	startButton.addEventListener("click", Start);
	const stopButton = document.getElementById("stop-button");
	stopButton.addEventListener("click", Stop);
	const clearButton = document.getElementById("clear-button");
	clearButton.addEventListener("click", ClearLogs);

	let currentDateTime = new Date();
	// DD-MM-YYYY HH:MM:SS
	let formattedDate = `${currentDateTime.getDate()}-${currentDateTime.getMonth() + 1}-${currentDateTime.getFullYear()}`;
	let formattedTime = `${currentDateTime.getHours()}:${currentDateTime.getMinutes()}:${currentDateTime.getSeconds()}`;
	let formattedDateTime = `${formattedDate} ${formattedTime}`;
	const clientDateTime = document.getElementById("client-datetime");
	clientDateTime.innerText = formattedDateTime;


}

function Start(){
	console.log("Emulator started...");

	const status = document.getElementById("player-status-text");
	status.innerText = `STATUS: ${EMU_STATUSES.STARTED}`;
}

function Stop(){
	console.log("Emulator stopped...");

	const status = document.getElementById("player-status-text");
	status.innerText = `STATUS: ${EMU_STATUSES.STOPPED}`;
}
function ClearLogs(){
	console.log("Logs cleared...");

	const logs = document.getElementById("c8j-messages");
	logs.innerHTML = "";

}
Initialize();




/* messaging */
//stomp 
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/c8j-websocket'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/c8j-messages', (c8jmessage) => {
        showC8JMessage(c8jmessage.body);
        //showC8JMessage(JSON.parse(c8jmessage.body).id);
        //showC8JMessage(JSON.parse(c8jmessage.body).type);
		//showC8JMessage(JSON.parse(c8jmessage.body).content);
    });
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    document.getElementById("start-button").disabled = connected;
    document.getElementById("stop-button").disabled = !connected;
    if (connected) {
        document.getElementById("c8j-messages-container").style.display = "block";
    }
    else {
        document.getElementById("c8j-messages-container").style.display = "none";
    }
    document.getElementById("c8j-messages").innerHTML = "";
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendClientMessage() {
    stompClient.publish({
        destination: "/app/c8j-server",
        body: JSON.stringify({'id':new Date().getTime().toString(),'type': 'base','content':'client message body'})
    });
}

function showC8JMessage(message) {
    //document.getElementById("c8j-messages").innerHTML += "<div>" + message + "</div>"; 
    document.getElementById("c8j-messages").innerHTML += message; 
}

document.querySelectorAll("form").forEach(form => form.addEventListener('submit', (e) => e.preventDefault()));
document.getElementById("start-button").addEventListener('click', () => connect());
document.getElementById("stop-button").addEventListener('click', () => disconnect());
document.getElementById("send-client-message").addEventListener('click', () => sendClientMessage());
/*
 (function () {
  "use strict";
  function greetMe(yourName) {
    alert(`Hello ${yourName}`);
  }

  greetMe("World");
})();
*/