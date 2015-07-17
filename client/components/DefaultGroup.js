import React from 'react';
import { renderNode, handleFocus } from '../utils/ComponentFunctions';

export default class DefaultGroup extends React.Component{
  render(){
    return (
      <div className='default-group' onClick={handleFocus.bind(this)}>
        {this.props.nodes.map(renderNode)}
      </div>
    );
  }
}
