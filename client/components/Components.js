import React from 'react';
import { renderNode } from '../Utils';
import NodeStore from '../stores/NodeStore';
import NodeActions from '../actions/NodeActions';

export class App extends React.Component{
  constructor(){
    super();
    this.onChange = this.onChange.bind(this);
    if(NodeStore.isEmpty()){
      this.state = {nodes: []}
    }else{
      this.state = {nodes: NodeStore.getDefaultGroup().get('nodes')};
    }
    NodeStore.addChangeListener(this.onChange);
  }
  componentWillUnmount(){
    NodeStore.removeChangeListener(this.onChange);
  }
  onChange(){
    console.log(NodeStore.getNodeMap());
    this.setState({nodes: NodeStore.getDefaultGroup().get('nodes')});
  }
  // shouldComponentUpdate() {
  //  return React.addons.PureRenderMixin.shouldComponentUpdate.apply(this, arguments);
  // }
  render(){
    if(NodeStore.isEmpty()){
      return <p>Waiting for server...</p>
    }
    return (
      <div className='app'>
        <DefaultGroup nodes={this.state.nodes} id={'1'} />
        <RightPane />
      </div>
    )
  }
}

export class DefaultGroup extends React.Component{
  // shouldComponentUpdate() {
  //  return React.addons.PureRenderMixin.shouldComponentUpdate.apply(this, arguments);
  // }
  render(){
    return (
      <div className='default-group'>
        {this.props.nodes.map(renderNode)}
      </div>
    );
  }
}

export class RightPane extends React.Component{
  // shouldComponentUpdate() {
  //   return React.addons.PureRenderMixin.shouldComponentUpdate.apply(this, arguments);
  // }
  constructor(props){
    super(props);
  }
  // displayOverlay(){
  //   return(
  //     <div className='overlay'>
  //     <button onClick={this.closeOverlay()}>close</button>
  //       raaawwwr I am overlaying the xscsrefreeen
  //     </div>
  //   )
  // }
  // closeOverlay(){
  //   dispatcher.dispatch({actionType: 'closeOverlay'});
  // }
  // dispatchOverlayFunction(){
  //   console.log('dispatching action');
  //   dispatcher.dispatch({actionType: 'displayOverlay', renderFunc: this.displayOverlay.bind(this)});
  // }
  handleAddGroupClick(){
    NodeActions.addGroup(1, 0);
  }
  
  render(){
   return (
    <div className='right-pane'>
      <button onClick={this.handleAddGroupClick.bind(this)}>ADD GROUP</button> 
      <button>ADD PARGROUP</button> 
      <button>ADD SYNTH</button> 
    </div>
   );
  } 
}

export class Group extends React.Component{
  // shouldComponentUpdate() {
  //   return React.addons.PureRenderMixin.shouldComponentUpdate.apply(this, arguments);
  // }
  //reset the position of the node, if drag failed 
  render(){
    return (
      <div className='group'>
        <h1>{this.props.id}</h1>
        {this.props.nodes.map(renderNode)}
      </div>
    );
  }
}

export class ParGroup extends React.Component{
  // shouldComponentUpdate() {
  //   return React.addons.PureRenderMixin.shouldComponentUpdate.apply(this, arguments);
  // }
  render(){
    return (
      <div className='par-group'>
        {this.props.nodes.map(renderNode)}
      </div>
    )
  }
}

export class Synth extends React.Component{
  // shouldComponentUpdate() {
  //   return React.addons.PureRenderMixin.shouldComponentUpdate.apply(this, arguments);
  // }
  render(){
    return (
      <span className='synth'> 
        Synth{this.props.id}
      </span>
    );
  }
}
