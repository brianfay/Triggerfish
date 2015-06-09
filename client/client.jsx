React = require('react');
var io = require('socket.io-client');
var socket = io();

//global objects that aren't going to change often (don't want to send from server constantly)
synthDefs = {};
synthDescs = {};
hardwareBuses = {};

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
      <button onClick={this.addGroup}>addGroup</button>
      <select id="synthSelect">
      {Object.keys(this.props.synthDefs).map(createOption)}
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
    var req = {
     instance: this.props.instance,
     synth: document.getElementById('synthSelect').value,
      //args: ['inBus', 1, 'outBus', 1]
      args: []
    }
    socket.emit('addSynth', req);
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
  componentDidMount: function(){
    // nx.add('dial',
    //   {parent: this.props.instance.nodeId});
  },
  render: function(){
    return <div className="par-group" id={this.props.instance.nodeId}>
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
    componentDidMount: function(){
        var that = this;
        var dial = nx.add('dial',
               {parent: String(this.props.instance.nodeId)});
        dial.transmit({value: dial.val, name: dial.name});
        dial.sendsTo(function(data){
            console.log(data.value);
            console.log(String(that.props.instance.nodeId))
            var req = {
                nodeId: that.props.instance.nodeId,
                paramName: 'delTime',
                value: (data.value * 2 + 0.02)
            };
            socket.emit('controlSynth', req);
        });
    // socket.emit('addSynth', req);
    },
  render: function(){
      return <div className="synth" id={this.props.instance.nodeId}>Synth index: {this.props.instance.index}<br/>
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
  React.render(<App appProps={data} synthDefs={synthDefs} synthDescs={synthDescs} hardwareBuses={hardwareBuses} />, document.getElementById("app"));
}

function setSynthDefs(data){
  synthDefs = data;
}

function setSynthDescs(data){
  synthDescs = data;
}

function setHardwareBuses(data){
  hardwareBuses = data;
}

socket.on('synthDefs', setSynthDefs);
socket.on('synthDescs', setSynthDescs);
socket.on('hardwareBuses', setHardwareBuses);
socket.on('appState', renderApp);
