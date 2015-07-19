import React from 'react';
import NodeStore from '../stores/NodeStore';
import NodeActions from '../actions/NodeActions';
import DefaultGroup from './DefaultGroup';
import Inspector from './Inspector';

export default class App extends React.Component{
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
  render(){
    if(NodeStore.isEmpty()){
      return <p>Waiting for server...</p>
    }
    return (
      <div className='app'>
        <DefaultGroup nodes={this.state.nodes} id={'1'} />
        <Inspector />
      </div>
    )
  }
}
