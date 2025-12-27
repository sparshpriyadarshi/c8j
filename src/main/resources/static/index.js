"use strict";

console.log("index js loads");

const DISPLAY_W = 64
const DISPLAY_H = 32
const DISPLAY_SCALE = 2
const OFF_RGB = { R:0,G:0,B:0}
const ON_RGB = { R:200,G:220,B:255}
const EMU_STATUSES = {
    RUNNING: "RUNNING",
    PAUSED: "PAUSED",
	STOPPED: "STOPPED",
    ERROR: "ERROR"
};
const CLIENT_MESSAGE_TYPE = {
    CONTROL: "CONTROL",
    KEYPAD: "KEYPAD",
    CANARY: "CANARY"
};
const CLIENT_ID = crypto.randomUUID();
const CLIENT_CANARY_MESSAGE = {
    "clientId": CLIENT_ID,
    "timestamp": new Date().getTime(),
    "type":CLIENT_MESSAGE_TYPE.CANARY, 
    "content":"client message content"
};
function SetupCanvas(){
    const canvas = document.getElementById("main-canvas");
    canvas.width = DISPLAY_W * DISPLAY_SCALE;
	canvas.height = DISPLAY_H * DISPLAY_SCALE;
    const ctx = canvas.getContext("2d");
    const img = new Image();
    img.src = "notlogo.png";
    img.crossOrigin = "anonymous";
    img.onload = () => {
        ctx.drawImage(img, 0, 0);
    };
    ctx.scale(DISPLAY_SCALE,DISPLAY_SCALE);
    ctx.imageSmoothingEnabled = false;
    
    /*
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
    ctx.fillRect(1, 1, canvas.width -1, canvas.height-1);
    */
    
}

function Initialize() {
	SetupCanvas();
    
	const statusText = document.getElementById("player-status-text");
	statusText.innerText = `STATUS: ${EMU_STATUSES.STOPPED}`;

	const startButton = document.getElementById("start-button");
	const stopButton = document.getElementById("stop-button");
	const clearButton = document.getElementById("clear-button");
    const keypadButtons = document.getElementById("keypad-section").getElementsByClassName("keypad");
    
   
	startButton.addEventListener("click", SetStatusStart);
    startButton.addEventListener('click', StompConnect);
    stopButton.addEventListener('click', StompDisconnect);
	stopButton.addEventListener("click", SetStatusStop);
    for(const btn of keypadButtons){
        btn.addEventListener("click", (e) => SendKeypadEvent(e.target.id));
    }
	clearButton.addEventListener("click", ClearLogs);

    document.querySelectorAll("form").forEach(form => form.addEventListener('submit', (e) => e.preventDefault()));
	
    let currentDateTime = new Date();
	// DD-MM-YYYY HH:MM:SS
	let formattedDate = `${currentDateTime.getDate()}-${currentDateTime.getMonth() + 1}-${currentDateTime.getFullYear()}`;
	let formattedTime = `${currentDateTime.getHours()}:${currentDateTime.getMinutes()}:${currentDateTime.getSeconds()}`;
	let formattedDateTime = `${formattedDate} ${formattedTime}`;
	const clientDateTime = document.getElementById("client-datetime");
	clientDateTime.innerText = formattedDateTime;

}


function RandomizeDisplay(){
    const randRGB = { R: Math.floor(Math.random() * 255), G: Math.floor(Math.random() * 255), B: Math.floor(Math.random() * 255) }

    const canvas = document.getElementById("main-canvas");
    const ctx = canvas.getContext("2d");
    const dispImageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    //const dispImageData = ctx.createImageData(canvas.width, canvas.height);
    const data = dispImageData.data;
    for (let i = 0; i < data.length; i += 4) {
        data[i] = randRGB.R;
        data[i + 1] = randRGB.G;
        data[i + 2] = randRGB.B;
    }
    ctx.putImageData(dispImageData, 0, 0);
}

function SetStatusStart(){
	const status = document.getElementById("player-status-text");
	status.innerText = `STATUS: ${EMU_STATUSES.RUNNING}`;
}

function SetStatusStop(){
	const status = document.getElementById("player-status-text");
	status.innerText = `STATUS: ${EMU_STATUSES.STOPPED}`;
}

function ClearLogs(){
	console.log("Logs cleared...");

	const logs = document.getElementById("c8j-messages");
	logs.innerHTML = "";

}

function SendStartEvent(){
    console.log("start event ");
    const clientStartMessage = {
        "clientId": CLIENT_ID,
        "timestamp": new Date().getTime(),
        "type":CLIENT_MESSAGE_TYPE.CONTROL, 
        "content":"START"
    };
    sendClientMessage(clientStartMessage);
}
function SendKeypadEvent(idValue){
    console.log("keypad event = " + idValue);
    const clientKeypadMessage = {
        "clientId": CLIENT_ID,
        "timestamp": new Date().getTime(),
        "type":CLIENT_MESSAGE_TYPE.KEYPAD, 
        "content":`${idValue}`
    };
    sendClientMessage(clientKeypadMessage);
}

function SendStepEvent(){
    console.log("step event ");
    const clientStepMessage = {
        "clientId": CLIENT_ID,
        "timestamp": new Date().getTime(),
        "type":CLIENT_MESSAGE_TYPE.CONTROL, 
        "content":"STEP"
    };
    sendClientMessage(clientStepMessage);
}

function SendPauseEvent(){
    console.log("pause event TODO");
}

function SendResumeEvent(){
    console.log("resume event TODO");
}

function SendStopEvent(){
    console.log("stop event ");
    const clientStopMessage = {
        "clientId": CLIENT_ID,
        "timestamp": new Date().getTime(),
        "type":CLIENT_MESSAGE_TYPE.CONTROL, 
        "content":"STOP"
    };
    sendClientMessage(clientStopMessage);
    
}







/* messaging */
//stomp 
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/c8j-websocket'
});

stompClient.onConnect = (frame) => {
    setConnected(true);//TODO remove UI coupling
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/c8j-messages', (c8jmessage) => {
        showC8JMessage(c8jmessage.body);//TODO refactor, update ui state...
        //showC8JMessage(JSON.parse(c8jmessage.body).id);
        //showC8JMessage(JSON.parse(c8jmessage.body).type);
		//showC8JMessage(JSON.parse(c8jmessage.body).content);
    });
    SendStartEvent();
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    //TODO this funcs usages need to be decoupled from messaging to a better place...
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

function StompConnect() {
    stompClient.activate();
}

function StompDisconnect() {
    SendStopEvent();
    stompClient.deactivate();
    console.log("Disconnected");
    setConnected(false);//TODO: remove ui coupling

}

function sendClientMessage(msg) { //TODO: broken esp on server
    console.log("sending client msg = " + msg);
    stompClient.publish({
        destination: "/app/c8j-server",
        body: JSON.stringify(msg)
    });
}

function showC8JMessage(message) {
    //document.getElementById("c8j-messages").innerHTML += "<div>" + message + "</div>"; 
    document.getElementById("c8j-messages").innerHTML += message; 
}
// ...stomp 



Initialize();

/*
 (function () {
  "use strict";
  function greetMe(yourName) {
    alert(`Hello ${yourName}`);
  }

  greetMe("World");
})();
*/