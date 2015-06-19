import React from 'react';
import shouldPureComponentUpdate from 'react-pure-render/function';
const Draggable = require('react-draggable');

const io = require('socket.io-client');
const socket = io();
React.initializeTouchEvents(true);

//@Util
const renderNode = (node) => {
  if(node.type == 'group'){
    // console.log(node);
    return <Group {...node} id={node.key}></Group>
  }
  else if(node.type == 'parGroup'){
    // console.log(node);
    return <ParGroup {...node} id={node.key}></ParGroup>
  }
  else if(node.type == 'synth'){
    return <Synth {...node} id={node.key}></Synth>
  }
}

//@Component
class App extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  render(){
    return <Group nodes={this.props.nodes} />
  }
}

//@Component
class Group extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate;
  handleStart(e, ui){
    e.stopPropagation();
  }
  handleDragStop(e, ui){
    const req = {id: this.props.id};
    socket.emit('moveNode', req);
  }
  //reset the position of the node, if drag failed
  resetNodePosition(){
    console.log('resetting node position');
    this.refs['theDraggable'].setState({clientX: 0, clientY: 0});
  }
  componentDidMount(){
    console.log('resetNode'+this.props.id);
    socket.on('resetNode'+this.props.id, this.resetNodePosition.bind(this));
  }
  componentWillUnMount(){
    this.socket.removeListener('resetNode'+this.props.key, this.resetNodePosition);
  }
  render(){
    return (<Draggable onStart={this.handleStart} onStop={this.handleDragStop.bind(this)} ref='theDraggable' zIndex={10000}>
      <div className='group'>
        {this.props.nodes.map(renderNode)}
      </div>
    </Draggable>);
  }
}

//@Component
class ParGroup extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate.bind(this);
  handleStart(e, ui){
    e.stopPropagation();
  }
  handleDragStop(e, ui){
    const req = {id: this.props.id};
    socket.emit('moveNode', req);
  }
  //reset the position of the node, if drag failed
  resetNodePosition(){
    console.log('resetting node position');
    this.refs['theDraggable'].setState({clientX: 0, clientY: 0});
  }
  componentDidMount(){
    console.log('resetNode'+this.props.id);
    socket.on('resetNode'+this.props.id, this.resetNodePosition.bind(this));
  }
  componentWillUnMount(){
    this.socket.removeListener('resetNode'+this.props.key, this.resetNodePosition);
  }
  render(){
    return (<Draggable onStart={this.handleStart} onStop={this.handleDragStop.bind(this)} ref='theDraggable' bounds={'parent'} zIndex={10000}>
    <div className='par-group'>
      {this.props.nodes.map(renderNode)}
    </div>
    </Draggable>);
  }
}

//@Component
class Synth extends React.Component{
  shouldComponentUpdate = shouldPureComponentUpdate.bind(this);
  handleStart(e, ui){
    e.stopPropagation();
  }
  handleDragStop(e, ui){
    const req = {id: this.props.id};
    socket.emit('moveNode', req);
  }
  //reset the position of the node, if drag failed
  resetNodePosition(){
    console.log('resetting node position');
    this.refs['theDraggable'].setState({clientX: 0, clientY: 0});
  }
  componentDidMount(){
    console.log('resetNode'+this.props.id);
    socket.on('resetNode'+this.props.id, this.resetNodePosition.bind(this));
  }
  componentWillUnMount(){
    this.socket.removeListener('resetNode'+this.props.key, this.resetNodePosition);
  }
  render(){
    return (
      <Draggable onStart={this.handleStart} onStop={this.handleDragStop.bind(this)} ref='theDraggable' zIndex={10000} moveOnStartChange={true}>
        <span
          className='synth' 
          style={{
            color: '#FFF'
            }}
        >
          Synth{this.props.id}
        </span>
      </Draggable>
    );
  }
}

const renderApp = (data) =>{
  console.log(data);
  React.render(<App nodes={data.nodes} />, document.getElementById('app'));
}

socket.on('appState', renderApp);
