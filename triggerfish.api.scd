var nodeDict = Dictionary.new();

(
newParGroup: { arg reply;
    var g = ParGroup.new();
    nodeDict.put(g.nodeID, g);
    reply.value(g.nodeID);
  },

placeGroupAfter: { arg reply, targetNodeID;
    var g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction:'addAfter');
    nodeDict.put(g.nodeID, g);
    reply.value(g.nodeID);
    },

placeGroupBefore: { arg reply, targetNodeID;
    var g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction:'addBefore');
    nodeDict.put(g.nodeID, g);
    reply.value(g.nodeID);
    },

newSynth: { arg reply, targetGroup, defName ...argsFromJs;
    var synth = Synth(defName, target:nodeDict.at(targetGroup), args: argsFromJs, addAction:'addToHead');
    nodeDict.put(synth.nodeID, synth);
    reply.value(synth.nodeID);
  },

removeNode: { arg reply, targetNode;
    nodeDict.at(targetNode).free;
    nodeDict.removeAt(targetNode);
    //remove any synths that belonged to a group that is being removed (to avoid overflowing the dictionary)
    nodeDict.keys.do{|idx| 
    if(nodeDict.at(idx).isKindOf(Synth),
        {if(nodeDict.at(idx).group.nodeID == targetNode, 
          {nodeDict.removeAt(idx)})
        }
    )};
    reply.value();
  },

getSynthDefNames: { arg reply;
    reply.value(PathName.new(SynthDef.synthDefDir).files.collect{arg item; item.fileName}); 
  }
);
