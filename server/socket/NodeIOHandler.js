import SocketConstants from '../../shared/constants/SocketConstants';
import NodeActions from '../actions/NodeActions';
import NodeStore from '../stores/NodeStore'

export default (IO) => {
  function emitNodeState(socket){
    socket.emit(SocketConstants.SET_NODE_MAP, NodeStore.getNodeMap());
  }

  IO.on('connection', (socket) => {
    console.log('client connected!');
    emitNodeState(socket);
    socket.on(SocketConstants.ADD_NODE, (req) => {
      NodeActions.addNode(req.nodeType, req.targetNodeID, req.addAction);
    })
  });

  NodeStore.addChangeListener(() => {
    emitNodeState(IO);
  });
};
