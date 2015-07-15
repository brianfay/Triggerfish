'use strict';
import http from 'http';
import NodeActions from './actions/NodeActions';
import NodeStatic from 'node-static';

const file = new NodeStatic.Server('../client');
const server = http.createServer(function(req,res){
  file.serve(req,res);
});

const nodeActions = new NodeActions(server);

server.listen(3000);
