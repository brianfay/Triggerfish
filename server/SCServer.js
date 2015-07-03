//Returns a Promise that resolves to a function that allows API calls to supercollider
import scjs from 'supercolliderjs';

const startSCLang = new Promise(function(resolve, reject){
  console.log('starting sclang');
  scjs.resolveOptions(null, {
    stdin: false,
    echo: false
  })
  .then(function(options){
    let sclang = new scjs.sclang(options);
    sclang.on('stdout', function(d){
      console.log('STDOUT: ' + d);
    });
    sclang.on('stderr', function(d){
      console.log('STDERR: ' + d);
    });
    sclang.boot().then(function(){
      console.log('booted sclang');
      let sc = new scjs.scapi(options.host, options.langPort);
      sc.log.dbug(options);
      sc.connect();
      resolve(sc);
    });
  });
});

const startSCSynth = new Promise(function(resolve, reject){
  startSCLang.then(function(sc){
    let count = 0;
    let callWrapper = function(url, param){
      return sc.call(count++,url,param);
    }
    callWrapper('server.boot').then(function(){
      console.log('server booted');
      resolve(callWrapper);
    });
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

export default callSC;
