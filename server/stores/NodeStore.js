//NodeStore is a reflection of the supercollider server's node tree, and is the single source of truth for Triggerfish clients
//Clients will receive a copy of the NodeStore on any change, and will never mutate their own local copy

//nodeMap is a flattened version of the RootNode, which allows each node to be accessed by id.
//I don't know for sure that this is a good idea... but it does seem easier and possibly more efficient than deep searches
let NodeMap = new Map();
import callSC from '../SCServer.js'

class Node{
  constructor(nodeId, type){
    this.nodeId = nodeId;
    this.type = type;
  }
  addNode(){
    //default
    console.log('no method to add node to nodeId: ' + this.nodeId);
  }
}

class ContainerNode extends Node{
  constructor(nodeId, type, nodes){
    super(nodeId, type)
    if(this.nodes){
      this.nodes = nodes;
    }else{
      this.nodes = [];
    }
  }
  addNode(node){
    this.nodes.push(node);
    NodeMap.set(node.nodeId, node);
  }
  addGroup(nodeId, nodes){
    this.addNode(new Group(nodeId, nodes));
  }
  addParGroup(nodeId, nodes){
    this.addNode(new ParGroup(nodeId, nodes));
  }
  addSynth(nodeId){
    this.addNode(new Synth(nodeId));
  }
}

//we assume the supercollider server will have a root node at id 0 that holds other nodes and never goes away
class RootNode extends ContainerNode{
  constructor(){
    super(0, 'RootNode');
  }
  getNode(nodeId){
    return NodeMap.get(nodeId);
  }
  deleteNode(idx){
    NodeMap.delete(this.nodes[idx].nodeId);
    this.nodes.splice(idx, 1);
  }
  logNodeMap(){
    console.log(JSON.stringify(NodeMap));
  }
  getNodeMap(){
    return NodeMap;
  }
}

class Group extends ContainerNode{
  constructor(nodeId, nodes){
    super(nodeId, 'Group', nodes);
  }
}

class ParGroup extends ContainerNode{
  constructor(nodeId, nodes){
    super(nodeId, 'ParGroup', nodes);
  }
}

class Synth extends Node{
  constructor(nodeId, defName){
    super(nodeId, 'Synth');
    this.defName = defName;
  }
}

const NodeStoreSingleton = new RootNode();
export default NodeStoreSingleton;
