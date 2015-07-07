import React from 'react';
import { Group, Synth, ParGroup } from './Components';

export function renderNode(node){
  if(node.type == 'group'){ return <Group {...node} id={node.key}></Group>
  }
  else if(node.type == 'parGroup'){
    return <ParGroup {...node} id={node.key}></ParGroup>
  }
  else if(node.type == 'synth'){
    return <Synth {...node} id={node.key}></Synth>
  }
}
