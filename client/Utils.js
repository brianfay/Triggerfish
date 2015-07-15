import React from 'react';
import NodeStore from './stores/NodeStore';
import { Group, Synth, ParGroup } from './components/Components';

export function renderNode(nodeID){
  const node = NodeStore.getNode(nodeID);
  if(node.get('type') == 'Group'){ 
    return <Group id={nodeID} nodes={node.get('nodes')} key={nodeID}></Group>
  }
  else if(node.get('type') == 'ParGroup'){ 
    return <ParGroup {...NodeStore.getNode(nodeID)} nodes={node.get('nodes')} key={nodeID} id={nodeID}></ParGroup>
  }
  else if(node.get('type') == 'Synth'){ 
    return <Synth {...NodeStore.getNode(nodeID)} key={nodeID} id={nodeID}></Synth>
  }
}
