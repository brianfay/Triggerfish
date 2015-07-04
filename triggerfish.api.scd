var nodeDict = Dictionary.new();

(
addNode: {arg reply, nodeType, targetNodeID, addAction, defName ...args;
	var g;
	switch(nodeType,
		'Group', {
			g = Group.new(target:nodeDict.at(targetNodeID), addAction: addAction.asSymbol);
		},
		'ParGroup', {
			g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction: addAction.asSymbol);
		},
		'Synth', {
			g = Synth.new(defName: defName, args: args, target:nodeDict.at(targetNodeID), addAction: addAction.asSymbol)
		},
		{Error('addNode: nodeType unrecognized').throw}
	);
	nodeDict.put(g.nodeID, g);
	reply.value(g.nodeID);
},
// addGroupToHead: {arg reply, targetNodeID;
// 	var g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction:'addToHead');
// 	nodeDict.put(g.nodeID, g);
// 	reply.value(g.nodeID);aj
// },
//
// addGroupToTail: {arg reply, targetNodeID;
// 	var g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction:'addToTail');
// 	nodeDict.put(g.nodeID, g);
// 	reply.value(g.nodeID);
// },
//
// addGroupBefore: { arg reply, targetNodeID;
// 	var g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction:'addBefore');
// 	nodeDict.put(g.nodeID, g);
// 	reply.value(g.nodeID);
// },
//
// addGroupAfter: { arg reply, targetNodeID;
// 	var g = ParGroup.new(target:nodeDict.at(targetNodeID), addAction:'addAfter');
// 	nodeDict.put(g.nodeID, g);
// 	reply.value(g.nodeID);
// },
//
// addSynthToHead: { arg reply, targetNodeID, defName ...argsFromJs;
// 	var synth = Synth(defName, target:nodeDict.at(targetNodeID), args: argsFromJs, addAction:'addToHead');
// 	nodeDict.put(synth.nodeID, synth);
// 	reply.value(synth.nodeID);
// },
//
// addSynthToTail: { arg reply, targetNodeID, defName ...argsFromJs;
// 	var synth = Synth(defName, target:nodeDict.at(targetNodeID), args: argsFromJs, addAction:'addToTail');
// 	nodeDict.put(synth.nodeID, synth);
// 	reply.value(synth.nodeID);
// },
//
// addSynthBefore: { arg reply, targetNodeID, defName ...argsFromJs;
// 	var synth = Synth(defName, target:nodeDict.at(targetNodeID), args: argsFromJs, addAction:'addBefore');
// 	nodeDict.put(synth.nodeID, synth);
// 	reply.value(synth.nodeID);
// },
//
// addSynthAfter: { arg reply, targetNodeID, defName ...argsFromJs;
// 	var synth = Synth(defName, target:nodeDict.at(targetNodeID), args: argsFromJs, addAction:'addAfter');
// 	nodeDict.put(synth.nodeID, synth);
// 	reply.value(synth.nodeID);
// },

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
    reply.value((PathName.new(SynthDef.synthDefDir).files.collect{arg item; item.fileName.split(46.asAscii).at(0)}.asSet).asArray);
},

getSynthDefs: { arg reply;
    var synthDict = Dictionary.new;
    var synthDefNames = (PathName.new(SynthDef.synthDefDir).files.collect{arg item; item.fileName.split(46.asAscii).at(0)}.asSet).asArray;
    synthDefNames.do{arg item, index;
	      synthDict.put(item.asSymbol, SynthDescLib.global.read.at(item.asSymbol).metadata)};
    reply.value(synthDict);
},

getSpecs: { arg reply;
	var specDict = Dictionary.new;
	ControlSpec.specs.keys.do{arg key;
		specDict.put(key, Spec.specs.at(key).asJSON);
	};
	reply.value(specDict);
},

getHardwareBuses: { arg reply;
	var hardwareBuses = Dictionary.new();
	hardwareBuses.put('numInputBusChannels', Server.local.options.numInputBusChannels);
	hardwareBuses.put('numOutputBusChannels', Server.local.options.numOutputBusChannels);
	reply.value(JSON.stringify(hardwareBuses));
},

controlSynth: { arg reply, targetNodeID, param, val;
	nodeDict.at(targetNodeID).set(param, val);
	reply.value(val);
}
);