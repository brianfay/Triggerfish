var nodeDict = Dictionary.new();

(
newParGroup: { arg reply;
    var g = ParGroup.new();
    nodeDict.put(g.nodeID, g);
    reply.value(g.nodeID);
  },

newSynth: { arg reply, targetGroup, defName ...argsFromJs;
    /* var processedArgs = Array.new(argsFromJs.size); */
    /* argsFromJs.do{arg i, idx; */
    /*   if(idx.even, */
    /*     {processedArgs.add(i.asSymbol)}, */
    /*     {processedArgs.add(i)}) */
    /* }; */
    var synth = Synth(defName, target:nodeDict.at(targetGroup), args: argsFromJs, addAction:'addToHead');
    nodeDict.put(synth.nodeID, synth);
    reply.value(synth.nodeID);
  },

removeNode: { arg reply, targetNode;
    nodeDict.at(targetNode).free;
    nodeDict.removeAt(targetNode);
    //kinda hacky way to remove any synths belonging to a group from the dict
    nodeDict.select{|item| item.group.nodeID == targetNode}.keys.do{|idx|
    nodeDict.removeAt(idx)};
    reply.value();
  }
);
