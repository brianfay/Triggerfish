/*
 * NodeStore is a reflection of the supercollider server's node tree, and is the single source of truth for Triggerfish clients
 * Clients will receive a copy of the NodeStore on any change, and will never mutate their own local copy
 */

import callSC from '../SCServer';
import Immutable from 'immutable';
import NodeConstants from '../constants/NodeConstants';
import NodeDispatcher from '../dispatcher/NodeDispatcher';
import { EventEmitter } from 'events';

const CHANGE_EVENT = 'change';

class NodeStore extends EventEmitter{
  constructor(){
    super();
    this.NodeMap = Immutable.Map({1: {type: 'DefaultGroup', nodes: []}});
    this.getNodeMap = this.getNodeMap.bind(this);
    this.addNode = this.addNode.bind(this);
    this.emitChange = this.emitChange.bind(this);
    // setInterval(() =>{callSC('triggerfish.queryAllNodes').then((resp) => {console.log(resp);})}, 1000);
  }
  _setNodeMap(nodeMap){
    console.log('setting node map');
    this.NodeMap = nodeMap;
    this.emitChange();
  }
  getNodeMap(){
    return this.NodeMap;
  }
  addNode(nodeType, targetNodeID, addAction, defName, args){
    //supercollider does not like undefined arguments
    defName = defName || null;
    args = args || null;
 
    //keys will be converted to strings when we serialize to json, should adhere to that here
    const targetKey = targetNodeID.toString();
    const targetNode = this.NodeMap.get(targetKey);
    if(!targetNode){
      throw Error(`there was no node at ID ${targetKey}`);
    }

    return(
      callSC.apply(this, ['triggerfish.addNode', [nodeType, targetNodeID, addAction, defName].concat(args)])
      .then((resp) => {
        const nodeID = resp.result;
        this._setNodeMap(
          this.NodeMap
          .set(nodeID.toString(), {type: nodeType, nodes: []})
          .set(targetKey,
                {type: targetNode.type, nodes: [nodeID.toString()].concat(targetNode.nodes)}
          )
        );
        return;
      })
      .catch((err) => {
        console.log(err);
      })
    )
  }
  addChangeListener(callback){
    this.on(CHANGE_EVENT, callback);
  }
  removeChangeListener(callback){
    this.removeListener(CHANGE_EVENT, callback);
  }
  emitChange(){
    this.emit(CHANGE_EVENT);
  }
}

NodeDispatcher.register(function(action) {
  switch(action.actionType){
    case NodeConstants.ADD_NODE:
      NodeStoreSingleton.addNode(
        action.nodeType,
        action.targetNodeID,
        action.addAction 
      );
      break;
    default: 
      console.error(`actionType: ${action.actionType} not recognized`);
  }
});

const NodeStoreSingleton = new NodeStore();
export default NodeStoreSingleton;
