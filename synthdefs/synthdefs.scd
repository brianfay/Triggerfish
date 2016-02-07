SynthDef("saw",
	{|out, freq=440|
		ReplaceOut.ar(out,
			Saw.ar(freq));
}).store;

SynthDef("sine",
	{|out, freq=440|
		ReplaceOut.ar(out,
			SinOsc.ar(freq));
}).store;

SynthDef("lopass",
	{|in, out, cutoff=440, res=1|
		ReplaceOut.ar(out, RLPF.ar(In.ar(in), cutoff, res));
}).store;

SynthDef("stereo-adc",
	{|outL, outR|
		ReplaceOut.ar(outL, SoundIn.ar(0));
		ReplaceOut.ar(outR, SoundIn.ar(1));
}).store;

SynthDef("tremolo",
	{|in, out, freq=0.3|
		ReplaceOut.ar(out, In.ar(in) * SinOsc.kr(freq))
}).store;

SynthDef("stereo-dac",
	{|inL, inR, outL=0, outR=1|
		Out.ar(outL, In.ar(inL));
		Out.ar(outR, In.ar(inR));
}).store;