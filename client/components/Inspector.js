import React from 'react';
import ClientStore from '../stores/ClientStore';
import NodeStore from '../stores/NodeStore';
import NodeActions from '../actions/NodeActions';

export default class Inspector extends React.Component{
  constructor(props){
    super(props);
    this.onChange = this.onChange.bind(this);
    ClientStore.addChangeListener(this.onChange);
    this.state = {focusedNodeID: ClientStore.getFocusedNodeID()};
  }
  componentWillUnmount(){
    ClientStore.removeChangeListener(this.onChange);
  }
  handleAddGroupClick(){
    NodeActions.addGroup(this.state.focusedNodeID, 0);
  }
  onChange(){
    this.setState({focusedNodeID: ClientStore.getFocusedNodeID()});  
  }
  renderButtons(){
    if(NodeStore.getNode(this.state.focusedNodeID).get('type') != 'Synth'){
      return(
        <div>
          <button onClick={this.handleAddGroupClick.bind(this)}>ADD GROUP</button> 
          <button>ADD PARGROUP</button> 
          <button>ADD SYNTH</button> 
        </div>
      )
    }
    return <h1>Synths have no child nodes</h1>
  }
  render(){
   return (
    <div className='right-pane'>
      <h1>{this.state.focusedNodeID}</h1>
      <h1>{NodeStore.getNode(this.state.focusedNodeID).get('type')}</h1>
      {this.renderButtons.call(this)}
    </div>
   );
  } 
}
