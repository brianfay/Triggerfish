import NodeIOHandler from './NodeIOHandler';

const io = require('socket.io-client');
const Socket = io();

NodeIOHandler(Socket);

export default Socket;
