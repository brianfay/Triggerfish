(
SynthDef(\externalIn, {arg inBus=#[0,0], outBus=0, mul=1, numChannels=2;
	mul * Out.ar(outBus, SoundIn.ar(inBus));
},metadata:
(nexusUISettings: [
	(ctlName: "mul", ctlType: "slider", min: 0, max: 1)]
)).load;

SynthDef(\delay, {arg inBus=#[0,0], outBus=0, delTime=0.4, decayTime=2, mul=1;
	var snd = In.ar(inBus);
	snd = CombL.ar(snd, 1, delTime, decayTime, mul);
	Out.ar(outBus, snd);
},metadata:
(nexusUISettings: [
	(ctlName: "mul", ctlType: "dial", min: 0, max: 1),
	(ctlName: "delTime", ctlType: "slider", min: 0.01, max: 1),
	(ctlName: "decayTime", ctlType: "dial", min: 0, max: 8)]
)).load;
)