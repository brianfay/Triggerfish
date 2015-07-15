const callSC = require('./SCServer');

// setInterval(function(){
//   callSC('API.apis', [])
//     .then(function(data){console.log(data)})
//     .catch(function(err){console.log(err)});
// },1000);
callSC('triggerfish.getSynthDefNames')
  .then(function(data){console.log(data)})
  .catch(function(err){console.log(err)});
callSC('triggerfish.addNode', ['Synth', 0, 'addToHead', 'default', 'freq', 660])
  .then(function(data){console.log(data)})
  .catch(function(err){console.log(err)});
callSC('triggerfish.addNode', ['Synth', 0, 'addToHead', 'default', 'freq', 880])
  .then(function(data){console.log(data)})
  .catch(function(err){console.log(err)});
callSC('triggerfish.addNode', ['Synth', 0, 'addToHead', 'default', 'freq', 1110])
  .then(function(data){console.log(data)})
  .catch(function(err){console.log(err)});
