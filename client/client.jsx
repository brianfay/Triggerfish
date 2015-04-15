React = require('react');
var io = require('socket.io-client');

var socket = io();

globalAppState = {};

//parGroupList -> parGroup -> synthList -> synth
App = React.createClass({
  addGroup: function(){
    socket.emit('addGroup');
  },
  render: function() {
    var createOption = function(val) {
      return <option value={val}>{val}</option>;
    }
    return <div>
      <p>SynthDefNames: {globalAppState.synthDefNames}</p>
      <button onClick={this.addGroup}>addGroup</button>
      <select id="synthSelect">
        {globalAppState.synthDefNames.map(createOption)}
      </select>
      <ParGroupList groups={this.props.appProps.parGroupList} />
      <BusList buses={this.props.appProps.busList} />
    </div>;
  }
});

ParGroupList = React.createClass({
  render: function() {
    var createParGroup = function(instance){
      return <ParGroup instance={instance} key={instance.nodeID} />;
    }
    return <div className="row-grid">{this.props.groups.map(createParGroup)}</div>;
  }
});

ParGroup = React.createClass({
  addSynth: function(){
    socket.emit('addSynth', [this.props.instance, document.getElementById('synthSelect').value, 'inBus', 0, 'outBus', 1]);
  },
  removeGroup: function(){
    socket.emit('removeGroup', this.props.instance);
  },
  addGroupAfter: function(){
    socket.emit('addGroupAfter', this.props.instance);
  },
  addGroupBefore: function(){
    socket.emit('addGroupBefore', this.props.instance);
  },
  render: function(){
    return <div className="par-group">
        Group # {this.props.instance.index}<br/>
        NodeID: {this.props.instance.nodeId}<br/>
        <button onClick={this.addGroupBefore}>addGroupBefore</button>
        <button onClick={this.addSynth}>addSynth</button> 
        <button onClick={this.removeGroup}>removeGroup</button>
        <button onClick={this.addGroupAfter}>addGroupAfter</button>
        <SynthList synths={this.props.instance.synthList} />
      </div>
  }
});

SynthList = React.createClass({
  render: function(){
    var createSynth = function(instance){
      return <Synth instance={instance} key={instance.nodeID} />;
    }
    return <div className="column-grid">{this.props.synths.map(createSynth)}</div>;
  }
});

Synth = React.createClass({
  render: function(){
    return <div className="synth">Synth index: {this.props.instance.index}<br/>
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

function renderApp(data){
  globalAppState = data; //for stuff I don't want to explicitly pass down to children, like synthDefNames (this is probably a terrible idea but #yoloswagwhatever)
  React.render(<App appProps={data} />, document.getElementById("app"));
}

socket.on('appState', renderApp);
