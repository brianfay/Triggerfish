import http from 'http';
import IO from './socket/IO';
import NodeStatic from 'node-static';

const file = new NodeStatic.Server('../client');
const Server = http.createServer(function(req,res){
  file.serve(req,res);
});

IO(Server);

Server.listen(3000);

export default Server;
