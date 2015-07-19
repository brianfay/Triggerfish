import ServerDispatcher from '../dispatcher/ServerDispatcher';
import NodeStore from '../stores/NodeStore'; 
import SocketConstants from '../../shared/constants/SocketConstants';
import socket from 'socket.io';

class NodeActions{
  constructor(server){
    this.io = new socket(server);
    this.registerSocketListeners.bind(this)();
    this.onChange = this.onChange.bind(this);
    NodeStore.addChangeListener(this.onChange);
  }
  registerSocketListeners(){
    this.io.on('connection', (socket) => {
      console.log('client connected!');
      this.io.emit(SocketConstants.SET_NODE_MAP, NodeStore.getNodeMap());
      socket.on(SocketConstants.ADD_NODE, (req) => {
        console.log('adding node');
        NodeStore.addNode(req.nodeType, req.targetNodeID, req.addAction).then(() => {
          this.io.emit(SocketConstants.SET_NODE_MAP, NodeStore.getNodeMap());
        })
        .catch((err) => {
          console.log(err);
        });
      });
    });
  }
  onChange(){
    console.log('emitting change');
    this.io.emit(SocketConstants.SET_NODE_MAP, NodeStore.getNodeMap());
  }
}

export default NodeActions;
