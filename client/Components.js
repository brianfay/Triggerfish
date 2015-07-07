import React from 'react';
import shouldPureComponentUpdate from 'react-pure-render/function';
import { renderNode } from './Utils';
const io = require('socket.io-client');
const socket = io();

export class App extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return (<RootNode nodes={this.props.nodes} id={'rootNode'} />)
  }
}

export class RootNode extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return (
      <div className='root-node'>
        {this.props.nodes.map(renderNode)}
      </div>
    );
  }
}

export class Group extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  //reset the position of the node, if drag failed 
  render(){
    return (
      <div className='group'>
        {this.props.nodes.map(renderNode)}
      </div>
    );
  }
}

export class ParGroup extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return (
      <div className='par-group'>
        {this.props.nodes.map(renderNode)}
      </div>
    )
  }
}

export class Synth extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return (
      <span className='synth'> 
        Synth{this.props.id}
      </span>
    );
  }
}
