import React from 'react';
import ClientActions from '../actions/ClientActions';
import NodeStore from '../stores/NodeStore';
import Group from '../components/Group';
import ParGroup from '../components/ParGroup';
import Synth from '../components/Synth';

/*
 * These functions are used by multiple components.
 * Maybe it would be cleaner to create higher-order components
 * I think this approach is pretty straightforward, though
 */

//Populates children of container nodes (DefaultGroup, Group, and ParGroup)
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

//Whenever a specific node is "focused", we should update app state
export function handleFocus(e){
  e.stopPropagation();
  ClientActions.focusOnNode(this.props.id);
}
