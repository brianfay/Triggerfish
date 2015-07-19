import React from 'react';

export default class Synth extends React.Component{
  render(){
    return (
      <span className='synth'> 
        Synth{this.props.id}
      </span>
    );
  }
}
