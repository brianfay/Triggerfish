var nodeDict = Dictionary.new();

(
newParGroup: { arg reply;
    var g = ParGroup.new();
    nodeDict.put(g.nodeID, g);
    reply.value(g.nodeID);
  },

newSynth: { arg reply, targetGroup, defName ... args;    
    var synth = Synth(defName, target:nodeDict.at(targetGroup), addAction:'addToHead');
    nodeDict.put(synth.nodeID, synth);
    reply.value(synth.nodeID);
  },

removeNode: { arg reply, targetNode;
    nodeDict.at(targetNode).free;
    /* nodeDict.removeAt(g.nodeID); */
    /* //kinda hacky way to remove any synths belonging to a group from the dict */
    /* nodeDict.select{|item| item.group.nodeID == targetNode}.keys.do{|idx| */
    /* nodeDict.removeAt(idx)}; */
    reply.value();
  }
);
