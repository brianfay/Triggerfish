import React from 'react';
import { Group, Synth, ParGroup } from './Components';

// var Group = components.Group;
// const ParGroup = require('./components.jsx').ParGroup;
// const Synth = require('./components.jsx').Synth;
//@Util
//tried to make this an arrow function, but then it's auto-binded so that was a bad idea I guess
export function renderNode(node){
  console.log('attempting to renderNode.');
  console.log('Group: ' + Group);
  //passing parent down to the nodes so they can update parent's z index... probably should just pass a callback
  if(node.type == 'group'){
    return <Group {...node} id={node.key} parent={this}></Group>
  }
  else if(node.type == 'parGroup'){
    return <ParGroup {...node} id={node.key} parent={this}></ParGroup>
  }
  else if(node.type == 'synth'){
    return <Synth {...node} id={node.key} parent={this}></Synth>
  }
}

//set zIndex of self and parents (recursively)
export const setZIndexRecursively = function(z){
  this.setState({restingZIndex: z});
  if(!this.props.parent){
    return;
  }
  if(this.props.parent.setZIndexRecursively == undefined){
    return;
  }
  this.props.parent.setZIndexRecursively(z);
}
