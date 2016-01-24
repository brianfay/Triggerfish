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

SynthDef("adc",
	{|outL, outR|
		ReplaceOut.ar(outL, SoundIn.ar(0));
		ReplaceOut.ar(outR, SoundIn.ar(0));
}).store;

SynthDef("tremolo",
	{|in, out, freq=0.3|
		ReplaceOut.ar(out, In.ar(in) * SinOsc.kr(freq))
}).store;

SynthDef("dac",
	{|inL, inR|
		Out.ar(0, In.ar(inL));thethe j
		Out.ar(1, In.ar(inR));
}).store;

daij