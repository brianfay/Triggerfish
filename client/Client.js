import React from 'react';
//the curly braces around { App } actually matter, es6 is so weird
import { App } from './Components';
const io = require('socket.io-client');
const socket = io();
React.initializeTouchEvents(true);

// socket.on('appState', renderApp);
console.log(App);

var appState = {};
appState.nodes = [];
appState.nodes.push({
  type: "synth",
  key: 1000
});
appState.nodes.push({
  type: "parGroup",
  key: 1003,
  nodes: [
    {type: "synth",
    key: 1004}
  ]
});
appState.nodes.push({
  type: "group",
  key: 1005,
  nodes: [
    {type: "synth",
    key: 1006}
  ]
});
appState.nodes.push({
  type: "synth",
  key: 1001
});
appState.nodes.push({
  type: "synth",
  key: 1002
});

function renderApp(data){
  React.render(<App nodes={data.nodes} />, document.getElementById('app'));
}
renderApp(appState);
