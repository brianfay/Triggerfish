import React from 'react';
import { renderNode, handleFocus } from '../utils/ComponentFunctions';

export class ParGroup extends React.Component{
  render(){
    return (
      <div className='par-group' onClick={handleFocus.bind(this)}>
        {this.props.nodes.map(renderNode)}
      </div>
    )
  }
}
