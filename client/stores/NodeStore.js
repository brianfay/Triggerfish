import ClientDispatcher from '../dispatcher/ClientDispatcher';
import { EventEmitter } from 'events';
import NodeConstants from '../constants/NodeConstants';
import Immutable from 'immutable';

const CHANGE_EVENT = 'change';

class NodeStore extends EventEmitter{
  constructor(){
    super();
    this.NodeMap = Immutable.Map(); 
    this.getNodeMap = this.getNodeMap.bind(this);
    this.getNode = this.getNode.bind(this);
    this.getDefaultGroup = this.getDefaultGroup.bind(this);
    this.setNodeMap = this.setNodeMap.bind(this);
    this.emitChange = this.emitChange.bind(this);
    this.isEmpty = this.isEmpty.bind(this);
    this.addChangeListener = this.addChangeListener.bind(this);
    this.removeChangeListener = this.removeChangeListener.bind(this);
  }
  getNodeMap(){
    return this.NodeMap;
  }
  getNode(id){
    return this.NodeMap.get(id);
  }
  getDefaultGroup(){
    return this.getNode('1');
  }
  setNodeMap(mapFromJS){
    this.NodeMap = Immutable.fromJS(mapFromJS);
    this.emitChange();
  }
  isEmpty(){
    return this.NodeMap.size == 0;
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
    case NodeConstants.SET_NODE_MAP:
      NodeStoreSingleton.setNodeMap(action.nodeMap);
      break;
    default:
      console.log('actionType: ' + actionType + ' not recognized'); 
  }
});

const NodeStoreSingleton = new NodeStore();
export default NodeStoreSingleton;
