var cb = { arg reply, msg, result;
	var m = MixedBundle.new;
    m.addFunction({ reply.value(result) });
    m.add(msg);
    m.send( Server.default );
  };

var nodeDict = Dictionary.new();

(
newParGroup: { arg reply;
    var g = ParGroup.new();
    nodeDict.put(g.nodeID, g);
    reply.value(g.nodeID);
  },
newSynth: { arg reply, targetGroup, defName ... args;    
    var synth = Synth(defName, target:nodeDict.at(targetGroup), addAction:'addToHead');
    cb.value(reply, synth.nodeID);
  }
);
