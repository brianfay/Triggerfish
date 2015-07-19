import socket from 'socket.io';
import NodeIOHandler from './NodeIOHandler';

export default function(server){
  const IO = new socket(server);
  NodeIOHandler(IO);
}
