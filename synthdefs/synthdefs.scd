SynthDef("saw",
	{|out, freq=440|
		ReplaceOut.ar(out,
			Saw.ar(freq, mul: 0.3));
}).store;

SynthDef("sine",
	{|out, freq=440|
		ReplaceOut.ar(out,
			SinOsc.ar(freq, mul: 0.03));
}).store;

SynthDef("lfsine",
	{|out, freq=1, mul=100, add=200|
		ReplaceOut.kr(out, SinOsc.kr(freq, mul: mul, add: add))
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

SynthDef("delay",
	{|in, out, delaytime = 0.2, decaytime = 1|
		ReplaceOut.ar(out, CombL.ar(in: In.ar(in),maxdelaytime: 2,delaytime: delaytime,decaytime: decaytime))
}).store;

SynthDef("stereo-dac",
	{|inL, inR, outL=0, outR=1|
		Out.ar(outL, In.ar(inL));
		Out.ar(outR, In.ar(inR));
}).store;

SynthDef(\loop, {arg in, bufnum, trigger= -1.0, rate=1, out=0;
	var sr, input, recordHead, loopEnd, output, env, envgen, existingSig;
	sr = SampleRate.ir;
	env = Env.asr(attackTime: 0.15, releaseTime: 0.5, curve: -4);
	envgen = EnvGen.ar(env, trigger); //fade in/out when starting/stopping recording
	input = In.ar(in) * (trigger > 0) * envgen;
	recordHead = Phasor.ar(trigger, BufRateScale.kr(bufnum), 0, BufFrames.kr(bufnum));
	existingSig = BufRd.ar(1,bufnum,recordHead) * (trigger < 0); //existing sig will keep rewriting original signal when not recording (thanks http://jeremah.co.uk/blog/permalink/a_synced_audio_looper_in_supercollider)
	loopEnd = Latch.ar(recordHead, trigger.neg); //where the loop stops in samples
	BufWr.ar(input + existingSig, bufnum, recordHead, 0);
	output = BufRd.ar(1,bufnum,Phasor.ar(trigger.neg, BufRateScale.kr(bufnum) * rate, 0, loopEnd)) * (trigger < 0);
	ReplaceOut.ar(out, output);
}).store;
