import NodeDispatcher from '../dispatcher/NodeDispatcher';
import NodeConstants from '../constants/NodeConstants';
import SocketConstants from '../../shared/constants/SocketConstants';
import Socket from '../socket/Socket';

const NodeActions = {
  addGroup: (targetNodeID, position) => {
    Socket.emit(SocketConstants.ADD_NODE, {
      nodeType: NodeConstants.GROUP,
      targetNodeID: targetNodeID,
      addAction: NodeConstants.ADD_TO_HEAD
    });
  },
  setNodeMap: (nodeMap) => {
    NodeDispatcher.dispatch({
      actionType: NodeConstants.SET_NODE_MAP,
      nodeMap: nodeMap
    }); 
  }
}

export default NodeActions;
