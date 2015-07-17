import React from 'react';

export default class Synth extends React.Component{
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
