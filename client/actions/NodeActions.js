import ClientDispatcher from '../dispatcher/ClientDispatcher';
import NodeConstants from '../constants/NodeConstants';
import SocketConstants from '../../shared/constants/SocketConstants';
const io = require('socket.io-client');
const socket = io();

socket.on(SocketConstants.SET_NODE_MAP, function(nodeMap){
  ClientDispatcher.dispatch({
    actionType: NodeConstants.SET_NODE_MAP,
    nodeMap: nodeMap
  }); 
});

const NodeActions = {
  addGroup: function(targetNodeID, position){
    socket.emit(SocketConstants.ADD_NODE, {
      nodeType: NodeConstants.GROUP,
      targetNodeID: targetNodeID,
      addAction: NodeConstants.ADD_TO_HEAD
    });
  }
}

export default NodeActions;
