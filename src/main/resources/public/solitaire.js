var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/solitaire/");

// construct the board on connect
webSocket.onmessage = function(board) {
    var boardData = JSON.parse(board.data);
    id("board").innerHTML = boardData.table;
    this.onmessage = discard;
//    this.onmessage = highlight;
};

// discard the first peg
var discard = function(peg) {
    var selection = peg.data;
    id(selection).style.backgroundColor = "#CCCCCC";
    this.onmessage = highlight;
}

// highlight the cell on valid selection and prepare for jump
var highlight = function(peg) {
    var selection = peg.data;
    if (selection != "error") {
        id(selection).style.backgroundColor = "green";
        this.onmessage = jump;
    }
}

var jump = function(jumpData) {
    var pegs = JSON.parse(jumpData.data);
    if (pegs.error == null) {
        id(pegs.source).style.backgroundColor = "#CCCCCC";
        id(pegs.jumped).style.backgroundColor = "#CCCCCC";
        id(pegs.destination).style.backgroundColor = "black";
    } else {
        id(pegs.error).style.backgroundColor = "black";
    }
    this.onmessage = highlight;
}

function select(peg) {
    webSocket.send(peg.id);
}

function suggest() {
    var previousMethod = webSocket.onmessage;
    webSocket.send("suggestion");
    webSocket.onmessage = function(suggestion) {
        try {
            var data = JSON.parse(suggestion.data);
            var i;
            for (i in data.suggestions) {
                id("peg" + data.suggestions[i].start).style.backgroundColor = "blue";
                id("peg" + data.suggestions[i].end).style.backgroundColor = "red";
            }
        } finally {
            this.onmessage = previousMethod;
        }
    }
}

// Helper function to get an element by id
function id(id) {
    return document.getElementById(id);
}

