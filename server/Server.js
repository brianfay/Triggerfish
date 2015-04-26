var koa = require('koa.io');
var app = koa();
var co = require('co');
var fs = require('fs');
var serve = require('koa-static');
var scjs = require('supercolliderjs');
var SCAPI = scjs.scapi;
var SCLang = scjs.sclang;

//state
var appState = {
  parGroupList: [],
  hardwareBuses: [],
  busList: [],
};

var synthDefs = {};
var synthDescs = {};
var hardwareBuses = [];

//helper
//increments every time we call api
var inc = (function(){
  var count = -1;
  function doInc(){
    count++;
    return count;
  }
  return doInc;
})();

//helper
function updateGroupIndexes(){
  for(var i=0; i < appState.parGroupList.length; i++){
    appState.parGroupList[i].index = i;
  }
}

//init:
callSC = {};
co(function *(next){
  var options = yield scjs.resolveOptions();
  var sclang = new SCLang(options);
  yield sclang.boot();
  console.log('booted sclang');
  var sc = new SCAPI(options.host, options.langPort);
  sc.log.dbug(options);
  sc.connect();
  callSC = function(uri, param){
    try{
      var res = sc.call(inc(), uri, param);
      return res;
    }catch(err){
      sc.log.err(err);
    }
  }
  yield callSC('server.boot');
  console.log('booted scsynth');

  var defs = yield callSC('triggerfish.getSynthDefs');
  synthDefs = defs.result;
  console.log('whoa synthDefs: ' + synthDefs);

  var specs = yield callSC('triggerfish.getSpecs');
  synthDescs = specs.result;
  var hwBusInfo = yield callSC('triggerfish.getHardwareBuses');
  hardwareBuses = hwBusInfo.result;
}).catch(function(err){console.error(err.stack)});


//middleware
app.use(serve('../res'));

//middleware
app.io.use(function* (next) {
  // on connect
  console.log('client connected to socket');
  this.socket.emit('synthDefs', synthDefs);
  this.broadcast.emit('synthDefs', synthDefs);
  this.socket.emit('synthDescs', synthDescs);
  this.broadcast.emit('synthDescs', synthDescs);
  this.socket.emit('hardwareBuses', hardwareBuses);
  this.broadcast.emit('hardwareBuses', hardwareBuses);
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  yield* next;
  // on disconnect
});

//middleware
app.io.route('addSynth', function* (next, req) {
  console.log('addSynth received by server');
  console.log('req: ' + JSON.stringify(req));
  var request = [req.instance.nodeId, req.synth];
  for(var i = 0; i < req.args.length; i++){
    request.push(req.args[i]);
  }
  console.log('request: ' + JSON.stringify(request));
  // var res = yield callSC('triggerfish.newSynth', [parseInt(req.nodeId.toString()), 'default', 'amp', Math.random()*0.4, 'freq', Math.floor(Math.random()*15)*59]);
  var res = yield callSC('triggerfish.newSynth',request);
  //var res = yield callSC('triggerfish.testThis', request);
  //var res = yield callSC('triggerfish.newSynth',[1000, "default", "inputBuses", [0, 1]]);
  console.log("response: " + JSON.stringify(res));

  appState.parGroupList[req.instance.index].synthList.push({index: appState.parGroupList[req.instance.index].synthList.length, nodeId: res.result});
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  console.log('addSynth done.');
  console.log(res);

});

//middleware
app.io.route('addGroup', function* (next) {
  console.log('addGroup received by server');
  var res = yield callSC('triggerfish.newParGroup');
  console.log(res);
  appState.parGroupList.push({index: appState.parGroupList.length, nodeId: res.result, synthList: []});
  // console.log("triggerfish.testReply response " + res);
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
});

//middleware
app.io.route('addGroupAfter', function* (next, req) {
  console.log('addGroupAfter received by server');
  var res = yield callSC('triggerfish.placeGroupAfter', req.nodeID);
  console.log(res);
  if(req.index >= 0){
    appState.parGroupList.splice(req.index + 1, 0, {index: req.index + 1, nodeId: res.result, synthList: []});
    updateGroupIndexes();
    this.socket.emit('appState', appState);
    this.broadcast.emit('appState', appState);
  }
});

//middleware
app.io.route('addGroupBefore', function* (next, req) {
  console.log('addGroupBefore received by server');
  var res = yield callSC('triggerfish.placeGroupBefore', req.nodeID);
  console.log(res);
  appState.parGroupList.splice(req.index, 0, {index: req.index, nodeId: res.result, synthList: []});
  updateGroupIndexes();
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
});

//middleware
app.io.route('removeGroup', function* (next, req) {
  console.log('removeGroup received by server');
  var res = yield callSC('triggerfish.removeNode', parseInt(req.nodeId.toString()));
  //remove parGroup from list
  appState.parGroupList.splice(req.index, 1);
  updateGroupIndexes();
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  console.log("finished removing group");
});

app.listen(3000);
