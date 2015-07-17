import ClientDispatcher from '../dispatcher/ClientDispatcher';
import ClientStore from '../stores/ClientStore';
import ClientConstants from '../constants/ClientConstants';

const NodeActions = {
  focusOnNode: function(nodeID){
    ClientDispatcher.dispatch({
      actionType: ClientConstants.SET_FOCUSED_NODE_ID,
      nodeID: nodeID
    });
  }
}

export default NodeActions;
