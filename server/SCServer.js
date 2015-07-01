//Returns a Promise that resolves to a function that allows API calls to supercollider
import scjs from 'supercolliderjs';

const startSC = new Promise(function(resolve, reject){
  console.log('starting supercollider');
  scjs.resolveOptions().then(function(options){
    console.log('resolved options!');
    let sclang = new scjs.sclang(options);
    sclang.boot().then(function(){
      console.log('booted sclang');
      let sc = new scjs.scapi(options.host, options.langPort);
      sc.log.dbug(options);
      sc.connect();
      let count = 0;
      let callSC = function(uri, param){
        return sc.call(count++, uri, param);
      }
      callSC('server.boot');
      resolve(callSC);
    }); 
  });
});

export default startSC;
