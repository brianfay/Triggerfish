import React from 'react';
import shouldPureComponentUpdate from 'react-pure-render/function';
import Draggable from 'react-draggable';
import { renderNode, setZIndexRecursively } from './Utils';
// const Dispatcher = require('flux').Dispatcher;
// const dispatcher = new Dispatcher();
// const io = require('socket.io-client');
// const socket = io();

export class App extends React.Component{
  // shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return (<RootNode nodes={this.props.nodes} id={'rootNode'} />)
  }
}

export class RootNode extends React.Component{
  // shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return (
      <div className='root-node'>
        {this.props.nodes.map(renderNode.bind(this))}
      </div>
    );
  }
}

export class Group extends React.Component{
  constructor(props){
    super(props);
    //restingZIndex is the zIndex used when not dragging
    this.state = {restingZIndex: 0};
  }
  // shouldComponentUpdate = shouldPureComponentUpdate;
  handleStart(e, ui){
    this.setZIndexRecursively(1);
    e.stopPropagation();
  }
  handleDragStop(e, ui){
    this.setZIndexRecursively(0);
    e.stopPropagation();
    const req = {id: this.props.id};
    // socket.emit('moveNode', req);
  }
  //reset the position of the node, if drag failed 
  resetNodePosition(){
    this.refs['theDraggable'].setState({clientX: 0, clientY: 0});
  }
  componentDidMount(){
    // socket.on('resetNode'+this.props.id, this.resetNodePosition.bind(this));
    // this.setState({dropHandler: dispatcher.register(this.handleDrop.bind(this))});
  }
  componentWillUnMount(){
    // this.socket.removeListener('resetNode'+this.props.key, this.resetNodePosition);
    // dispatcher.unregister(this.state.dropHandler);
  }
  handleDrop(payload){
    if(payload.actionType == 'drop'){
      //shouldn't handle a drop on own component
      if(payload.id == this.props.id) return;

      return;
      // console.log('x: ' + payload.x);
      // console.log('y: ' + payload.y);
    }
  }
  setZIndexRecursively = setZIndexRecursively;
  getStyle  = () => {
    return {zIndex: this.state.restingZIndex}
  }
  render(){
    return (<Draggable onStart={this.handleStart.bind(this)} onStop={this.handleDragStop.bind(this)} ref='theDraggable'>
      <div className='group' style={this.getStyle()}>
        {this.props.nodes.map(renderNode.bind(this))}
      </div>
    </Draggable>);
  }
}

export class ParGroup extends React.Component{
  constructor(props){
    super(props);
    this.state = {restingZIndex: 0};
  }
  // shouldComponentUpdate = shouldPureComponentUpdate;
  handleStart(e, ui){
    this.setZIndexRecursively(1);
    e.stopPropagation();
  }
  handleDragStop(e, ui){
    this.setZIndexRecursively(0);
    const req = {id: this.props.id};
    // socket.emit('moveNode', req);
  }
  //reset the position of the node, if drag failed
  resetNodePosition(){
    this.refs['theDraggable'].setState({clientX: 0, clientY: 0});
  }
  handleDrop(payload){
    if(payload.actionType == 'drop'){
      //shouldn't handle a drop on own component
      if(payload.id == this.props.id) return;

      // console.log('drop event on a pargroup...');
      return;
      // console.log('x: ' + payload.x);
      // console.log('y: ' + payload.y);
    }
  }
  setZIndexRecursively = setZIndexRecursively;
  componentDidMount(){
    // socket.on('resetNode'+this.props.id, this.resetNodePosition.bind(this));
    // this.setState({dropHandler: dispatcher.register(this.handleDrop.bind(this))});
    this.setState({zIndex: 100});
  }
  componentWillUnMount(){
    // this.socket.removeListener('resetNode'+this.props.key, this.resetNodePosition);
    // dispatcher.unregister(this.state.dropHandler);
  }
  getStyle  = () => {
    return {zIndex: this.state.restingZIndex}
  }
  render(){
    return (<Draggable onStart={this.handleStart.bind(this)} onStop={this.handleDragStop.bind(this)} ref='theDraggable'>
    <div className='par-group' style={this.getStyle()}>
      {this.props.nodes.map(renderNode.bind(this))}
    </div>
    </Draggable>)
  }
}

export class Synth extends React.Component{
  constructor(props){
    super(props);
    this.state = {restingZIndex: 0};
  }
  // shouldComponentUpdate = shouldPureComponentUpdate;
  handleStart(e, ui){
    this.setZIndexRecursively(1);
    e.stopPropagation();
  }
  handleDragStop(e, ui){
    this.setZIndexRecursively(0);
    const req = {id: this.props.id};
    // socket.emit('moveNode', req);
    let x = e.clientX;
    let y = e.clientY;
    //throw drop event:
    // dispatcher.dispatch({actionType: 'drop', x: x, y: y, id: this.props.id});
  }
  handleDrop(payload){
    if(payload.actionType == 'drop'){
      //shouldn't handle a drop on own component
      if(payload.id == this.props.id) return;

      // console.log('drop event on a synth, ignore...');
      return;
    }
  }
  //reset the position of the node, if drag failed
  resetNodePosition(){
    //I think this is getting called more often than it should
    this.refs['theDraggable'].setState({clientX: 0, clientY: 0});
  }
  componentDidMount(){
    // socket.on('resetNode'+this.props.id, this.resetNodePosition.bind(this));
    // this.setState({dropHandler: dispatcher.register(this.handleDrop.bind(this))});
  }
  componentWillUnMount(){
    // this.socket.removeListener('resetNode'+this.props.key, this.resetNodePosition);
    // dispatcher.unregister(this.state.dropHandler);
  }
  setZIndexRecursively = setZIndexRecursively;
  getStyle  = () => {
    return {zIndex: this.state.restingZIndex}
  }
  render(){
    return (
      //zIndex is related to its current stacking context - the component with zIndex 1 will be higher than other components 1 in same scope
      <Draggable style={this.getStyle()} onStart={this.handleStart.bind(this)} onStop={this.handleDragStop.bind(this)} ref='theDraggable' zIndex={1}>
        <span className='synth'> 
          Synth{this.props.id}
        </span>
      </Draggable>
    );
  }
}
