import React from 'react';
import { renderNode, handleFocus } from '../utils/ComponentFunctions';

export default class Group extends React.Component{
  render(){
    return (
      <div className='group' onClick={handleFocus.bind(this)}>
        <p>{this.props.id}</p>
        {this.props.nodes.map(renderNode)}
      </div>
    );
  }
}
