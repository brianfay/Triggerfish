var koa = require('koa.io'),
    app = koa(),
    co = require('co'),
    fs = require('fs'),
    serve = require('koa-static'),
    scjs = require('supercolliderjs'),
    SCAPI = scjs.scapi;
    SCLang = scjs.sclang;

var appState = {
  parGroupList: [],
  busList: [],
  synthDefNames: []
};

//increments every time we call api
var inc = (function(){
  var count = -1;
  function doInc(){
    count++;
    return count;
  } 
  return doInc;
})();

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

  var synthDefNames = yield callSC('triggerfish.getSynthDefNames'); 
  synthDefNames = synthDefNames.result
  for(var i = 0; i < synthDefNames.length; i++){
    synthDefNames[i] = synthDefNames[i].split('.')[0];
  }
  console.log('got synth names: ', synthDefNames);
  synthDefNames.push('default');
  appState.synthDefNames = synthDefNames;
}).catch(function(err){console.error(err.stack)});


app.use(serve('../res'));

app.io.use(function* (next) {
  // on connect
  console.log('client connected to socket');


  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  yield* next;
  // on disconnect
});

//socket event
app.io.route('addSynth', function* (next, req) {
  console.log('addSynth received by server');
  // var res = yield callSC('triggerfish.newSynth', [parseInt(req.nodeId.toString()), 'default', 'amp', Math.random()*0.4, 'freq', Math.floor(Math.random()*15)*59]);
  var request = [req[0].nodeId, req[1]];
  for(var i = 2; i < req.length; i++){
    request.push(req[i]);
  }
  var res = yield callSC('triggerfish.newSynth', request);
  appState.parGroupList[req[0].index].synthList.push({index: appState.parGroupList[req[0].index].synthList.length, nodeId: res.result});
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  console.log('addSynth done.');
  console.log(res);
});

app.io.route('addGroup', function* (next) {
  console.log('addGroup received by server');
  var res = yield callSC('triggerfish.newParGroup');
  console.log(res);
  appState.parGroupList.push({index: appState.parGroupList.length, nodeId: res.result, synthList: []});
  // console.log("triggerfish.testReply response " + res);
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
});

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

app.io.route('addGroupBefore', function* (next, req) {
  console.log('addGroupBefore received by server');
  var res = yield callSC('triggerfish.placeGroupBefore', req.nodeID);
  console.log(res);
  appState.parGroupList.splice(req.index, 0, {index: req.index, nodeId: res.result, synthList: []});
  updateGroupIndexes();
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
});

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

console.log('listening on port 3000')
