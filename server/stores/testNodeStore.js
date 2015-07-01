import NodeStore from './NodeStore'
// var nodeStore = NodeStore();

NodeStore.logNodeMap();
NodeStore.addSynth(1001);

console.log('getting node 1001: ');
console.log(NodeStore.getNode(1001));

console.log('getting node 1002: ');
console.log(NodeStore.getNode(1002));


NodeStore.addGroup(1002);

NodeStore.getNode(1002).addSynth(1003);
NodeStore.getNode(1002).addParGroup(1004);
NodeStore.getNode(1004).addSynth(1005);

NodeStore.logNodeMap();
// NodeStore.deleteNode(0);
NodeStore.logNodeMap();

console.log(NodeStore.nodes[1].nodes[1]);
