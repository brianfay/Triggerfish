/*
 * Returns a function that calls supercollider after Promise to start sclang and scsynth is resolved
 */
import scjs from 'supercolliderjs';

const startSCLang = new Promise((resolve, reject) => {
  console.log('starting sclang');
  scjs.resolveOptions(null, {
    stdin: false,
    echo: false
  })
  .then((options) => {
    var sclang = new scjs.sclang(options);
    sclang.on('stdout', (d) => {
      console.log('STDOUT: ' + d);
    });
    sclang.on('stderr', (d) => {
      console.error('STDERR: ' + d);
      throw d;
    });
    sclang.boot().then(() => {
      console.log('booted sclang');
      var sc = new scjs.scapi(options.host, options.langPort);
      sc.log.dbug(options);
      sc.connect();
      resolve(sc);
    })
    .catch((err) => {
      console.log('There was a problem starting sclang.');
      console.log(err);
      process.exit(1);
    });
  });
});

const startSCSynth = new Promise((resolve, reject) => {
  startSCLang.then((sc) => {
    var count = 0;
    var callWrapper = (url, param) => {
      return sc.call(count++,url,param);
    }
    callWrapper('server.boot').then(() => {
      console.log('server booted');
    })
    .catch((err) => {
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
      console.error('There was a problem loading SynthDefs');
      console.error(err);
      process.exit(1);
    })
  });
});

function callSC(url, param){
  return new Promise((resolve, reject) => {
    startSCSynth.then((cb) => {
      cb(url, param).then((data) => {
        resolve(data);
      });
    });
  });
}

export default callSC;
