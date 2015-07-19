import ClientDispatcher from '../dispatcher/ClientDispatcher';
import ClientConstants from '../constants/ClientConstants';

const ClientActions = {
  focusOnNode: function(nodeID){
    ClientDispatcher.dispatch({
      actionType: ClientConstants.SET_FOCUSED_NODE_ID,
      nodeID: nodeID
    });
  }
}

export default ClientActions;
