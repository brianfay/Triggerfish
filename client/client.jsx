React = require('react');
var io = require('socket.io-client');

var socket = io();

//parGroupList -> parGroup -> synthList -> synth
App = React.createClass({
  addGroup: function(){
    socket.emit('addGroup');
  },
  render: function() {
    return <div>
      <h1>app</h1>
      <ParGroupList groups={this.props.appProps.parGroupList} />
      <BusList buses={this.props.appProps.busList} />
      <button onClick={this.addGroup}>addGroup</button>
    </div>;
  }
});

ParGroupList = React.createClass({
  render: function() {
    var createParGroup = function(instance){
      return <ParGroup instance={instance} key={instance.index} />;
    }
    return <div className="column-grid">{this.props.groups.map(createParGroup)}</div>;
  }
});

ParGroup = React.createClass({
  addSynth: function(){
    socket.emit('addSynth', this.props.instance);
  },
  removeGroup: function(){
    socket.emit('removeGroup', this.props.instance);
  },
  render: function(){
    return <div className="cell">
        Group # {this.props.instance.index}<br/>
        NodeID: {this.props.instance.nodeId}<br/>
        <SynthList synths={this.props.instance.synthList} />
        <button onClick={this.addSynth}>addSynth</button> 
        <button onClick={this.removeGroup}>removeGroup</button>
      </div>
  }
});

SynthList = React.createClass({
  render: function(){
    var createSynth = function(instance){
      return <Synth instance={instance} key={instance.index} />;
    }
    return <div className="row-grid">{this.props.synths.map(createSynth)}</div>;
  }
});

Synth = React.createClass({
  render: function(){
    return <div className="cell">Synth index: {this.props.instance.index}<br/>
      NodeId: {this.props.instance.nodeId}
    </div>
  }
});


BusList = React.createClass({
  render: function(){
    var createBus = function(instance){
      return <Bus instance={instance} />;
    }
    return <div>{this.props.buses.map(createBus)}</div>
  }
});

Bus = React.createClass({
  render: function(){
    return <div>Bus: {this.props.instance}</div>;
  }
});
initProps = 
{
 parGroupList: [],
 busList: []
};

React.render(<App appProps={initProps} />, document.getElementById("app"));

function renderApp(data){
  React.render(<App appProps={data} />, document.getElementById("app"));
}

socket.on('appState', renderApp);
