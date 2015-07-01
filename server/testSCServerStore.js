import startSC from './SCServer'

startSC.then(function(callSC){
  setInterval(function(){
    callSC('API.apis', [])
      .then(function(data){console.log(data)})
      .catch(function(err){console.log(err)});
  },1000);
});

