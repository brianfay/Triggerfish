[2,3].wrapAt(3)
.collect{|bus, i| bus;}
~fe = "tets".collect{|e| e;}
(
SynthDef(\externalIn, {arg inBus=[0,0], outBus=[0,1], mul;
	Out.ar(outBus,SoundIn.ar(inBus));
}).store;

SynthDef(\delay, {arg inBus, outBus, mul;
	var snd = In.ar(inBus);
	snd = CombL.ar(snd);
	Out.ar(outBus, snd);
}).store;
)

PathName.new(SynthDef.synthDefDir).files.collect{arg item; item.fileName}

do{|elem| elem.fileName.postln;}


s.boot;
Synth(\externalIn, [inBus: [0,1], outBus: [0,1]]);
Synth(\externalIn)
Synth(\default)
Synth
{SoundIn.ar(0!2)}.play;

{Out.ar([0,0])}.new;

{
	var outs = [0,1];
	var srcs = SinOsc.ar([200,800,399]) * 0.2;
	outs.do{|n, i| srcs.do{|e| Out.ar(n, e)};
	};
}.play;