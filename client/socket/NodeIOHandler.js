import NodeActions from '../actions/NodeActions';
import NodeConstants from '../constants/NodeConstants';
import SocketConstants from '../../shared/constants/SocketConstants';

export default function(IO){
  IO.on(SocketConstants.SET_NODE_MAP, function(nodeMap){
    NodeActions.setNodeMap(nodeMap);
  });
}
