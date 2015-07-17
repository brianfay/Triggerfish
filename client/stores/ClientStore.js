import ClientDispatcher from '../dispatcher/ClientDispatcher';
import { EventEmitter } from 'events';
import NodeStore from './NodeStore';
import NodeConstants from '../constants/NodeConstants';
import ClientConstants from '../constants/ClientConstants';
import Immutable from 'immutable';

const CHANGE_EVENT = 'change';

//this class should store client application state (not to be shared with server)
class ClientStore extends EventEmitter{
  constructor(){
    super();
    this.focusedNodeID = NodeConstants.DEFAULT_GROUP_ID;
  }
  setFocusedNodeID(id){
    this.focusedNodeID = id;
    this.emitChange(CHANGE_EVENT);
  }
  getFocusedNodeID(){
    return this.focusedNodeID;
  }
  emitChange(){
    this.emit(CHANGE_EVENT);
  }
  addChangeListener(callback){
    this.on(CHANGE_EVENT, callback);
  }
  removeChangeListener(callback){
    this.removeListener(CHANGE_EVENT, callback);
  }
}

ClientDispatcher.register(function(action) {
  switch(action.actionType){
    case ClientConstants.SET_FOCUSED_NODE_ID:
      ClientStoreSingleton.setFocusedNodeID(action.nodeID);
      break;
    default:
      console.error('actionType: ' + action.actionType + ' not recognized');
  }
});

const ClientStoreSingleton = new ClientStore();
export default ClientStoreSingleton;
