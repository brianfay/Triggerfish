// import koa from 'koa.io'
// import serve from 'koa-static'
var koa = require('koa.io');
var app = koa();
var serve = require('koa-static');

//state
var nodeState= {};
nodeState.nodes = [];
nodeState.nodes.push({
  type: "synth",
  key: 1000
});
nodeState.nodes.push({
  type: "parGroup",
  key: 1003,
  nodes: [
    {type: "synth",
    key: 1004}
  ]
});
nodeState.nodes.push({
  type: "group",
  key: 1005,
  nodes: [
    {type: "synth",
    key: 1006}
  ]
});
nodeState.nodes.push({
  type: "synth",
  key: 1001
});
nodeState.nodes.push({
  type: "synth",
  key: 1002
});

//middleware
app.use(serve('client'));

//middleware
app.io.use(function* (next) {
  // on connect
  console.log('client connected to socket');
  this.socket.emit('nodeState',nodeState);
  this.broadcast.emit('nodeState',nodeState);
  yield* next;
  // on disconnect
});

app.io.route('moveNode', function* (next, req){
  //on move node
  console.log('node: ' + req.id + ' has moved.');
  //should call supercollider here to move the node
  //
  //on error, should return the node to its original state
  console.log('resetNode' + req.id);
  this.socket.emit('resetNode' + req.id);
  // yield* next;
});

app.listen(3000);
