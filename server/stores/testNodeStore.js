'use strict';
const NodeStore= require('./NodeStore');

// NodeStore.logNodeMap();
// NodeStore.addSynth(1001);
console.log('NodeMap size: ' + NodeStore.getNodeMap().size);

NodeStore.addNode('Synth', 0, 'addToHead', 'default', ['freq', 660])
  .then(function(){
    console.log('NodeMap size: ' + NodeStore.getNodeMap().size);
    console.log('Node 1000: ' + NodeStore.getNodeMap().get('1000'));
    console.log('Node 1001: ' + NodeStore.getNodeMap().get('1001'));
    console.log('Node 1002: ' + NodeStore.getNodeMap().get('1002'));
  });
NodeStore.addNode('Synth', 0, 'addToHead', 'default', ['freq', 880])
  .then(function(){
    console.log('NodeMap size: ' + NodeStore.getNodeMap().size);
    console.log('Node 1000: ' + NodeStore.getNodeMap().get('1000'));
    console.log('Node 1001: ' + NodeStore.getNodeMap().get('1001'));
    console.log('Node 1002: ' + NodeStore.getNodeMap().get('1002'));
  });
NodeStore.addNode('Synth', 0, 'addToHead', 'default', ['freq', 1110])
  .then(function(){
    console.log('NodeMap size: ' + NodeStore.getNodeMap().size);
    console.log('Node 1000: ' + NodeStore.getNodeMap().get('1000'));
    console.log('Node 1001: ' + NodeStore.getNodeMap().get('1001'));
    console.log('Node 1002: ' + NodeStore.getNodeMap().get('1002'));
  });
