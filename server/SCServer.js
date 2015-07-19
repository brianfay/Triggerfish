//Returns a Promise that resolves to a function that allows API calls to supercollider
var scjs = require('supercolliderjs');

const startSCLang = new Promise(function(resolve, reject){
  console.log('starting sclang');
  scjs.resolveOptions(null, {
    stdin: false,
    echo: false
  })
  .then(function(options){
    var sclang = new scjs.sclang(options);
    sclang.on('stdout', function(d){
      console.log('STDOUT: ' + d);
    });
    sclang.on('stderr', function(d){
      console.log('STDERR: ' + d);
      throw d;
    });
    sclang.boot().then(function(){
      console.log('booted sclang');
      var sc = new scjs.scapi(options.host, options.langPort);
      sc.log.dbug(options);
      sc.connect();
      resolve(sc);
    })
    .catch(function(err){
      console.log('There was a problem starting sclang.');
      console.log(err);
      process.exit(1);
    });
  });
});

const startSCSynth = new Promise(function(resolve, reject){
  startSCLang.then(function(sc){
    var count = 0;
    var callWrapper = function(url, param){
      return sc.call(count++,url,param);
    }
    callWrapper('server.boot').then(function(){
      console.log('server booted');
    })
    .catch(function(err){
      console.log('There was a problem starting scsynth.');
      console.log(err);
      process.exit(1);
    })
    .then(() => {
      callWrapper('triggerfish.loadSynthDefs');
      console.log('loaded SynthDefs');
      resolve(callWrapper);
    })
    .catch((err) =>{
      console.err('There was a problem loading synth defs');
      console.error(err);
      process.exit(1);
    })
  });
});

function callSC(url, param){
  return new Promise(function(resolve, reject){
    startSCSynth.then(function(cb){
      cb(url, param).then(function(data){
        resolve(data);
      });
    });
  });
}

module.exports = callSC;
