import NodeDispatcher from '../dispatcher/NodeDispatcher';
import NodeConstants from '../constants/NodeConstants';
import SocketConstants from '../../shared/constants/SocketConstants';
import Socket from '../utils/Socket';

Socket.on(SocketConstants.SET_NODE_MAP, function(nodeMap){
  NodeDispatcher.dispatch({
    actionType: NodeConstants.SET_NODE_MAP,
    nodeMap: nodeMap
  }); 
});

const NodeActions = {
  addGroup: function(targetNodeID, position){
    Socket.emit(SocketConstants.ADD_NODE, {
      nodeType: NodeConstants.GROUP,
      targetNodeID: targetNodeID,
      addAction: NodeConstants.ADD_TO_HEAD
    });
  }
}

export default NodeActions;
