(
SynthDef(\externalIn, {arg inBus=#[0,0], outBus=0, mul=1, numChannels=2;
	mul * Out.ar(outBus, SoundIn.ar(inBus));
},metadata: (specs: (inBus: \audiobus, outBus: \audiobus, mul: \amp))).store;
SynthDef(\delay, {arg inBus=#[0,0], outBus=0, mul;
	var snd = In.ar(inBus);
	snd = CombL.ar(snd, 1, 0.4, 2);
	Out.ar(outBus, snd);
}).store;
)