import React from 'react';
//the curly braces around { App } actually matter, es6 is so weird
import { App } from './Components';
const io = require('socket.io-client');
const socket = io();
React.initializeTouchEvents(true);

console.log(App);

socket.on('nodeState', function(data){
  React.render(<App nodes={data.nodes} />, document.getElementById('app'));
});
