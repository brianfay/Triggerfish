import NodeConstants from '../constants/NodeConstants';
import NodeDispatcher from '../dispatcher/NodeDispatcher';

const NodeActions = {
  addNode: (nodeType, targetNodeID, addAction) => {
    NodeDispatcher.dispatch({
      actionType: NodeConstants.ADD_NODE,
      nodeType: nodeType,
      targetNodeID: targetNodeID,
      addAction: addAction
    });
  }
}

export default NodeActions;
