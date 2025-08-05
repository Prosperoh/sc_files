s.boot;
s.quit;

/*
(
var prob = 0.2; // chance of being above zero
var pat, stream;

var left_val = prob.neg.reciprocal - 1;

pat = Pbrown(0, 0, 1.0, inf);
stream = pat.asStream;

100.do({ stream.next.postln; });

~pat = pat;
)
*/

(
~busFx = Bus.audio(s, numChannels: 2);
~prob = {
	arg lo = 0.0, hi = 1.0, pow = 4, step = 0.125;

	Pbrown(0.0, 1.0, step, inf).pow(pow) * (hi - lo) + lo
};
~expprob = {
	arg lo = 0.0, hi = 1.0, pow = 4, step = 0.125;

	Pbrown(0.0, 1.0, step, inf).pow(pow) * (hi - lo) + lo
};
)

(
Ndef(\fx, {
	var sig;
	sig = In.ar(~busFx, 2);

	sig = CombL.ar(sig);
	sig = Mix.ar([sig, 0.5 * GVerb.ar(sig)]);
	sig;
}).play;
)

t = TempoClock();
t.tempo_(120/60);

(
SynthDef("fm", {
	arg out = 0, amp = 0.5, freq = 440, pan = 0.0, spread = 1.0, rq = 1.0,
	mRatio = 1, cRatio = 1, index = 1, iScale = 5,
	atk = 0.01, rel = 3.0, cAtk = 4, cRel = (-4);
	var sig, env, iEnv;

	iEnv = EnvGen.kr(Env.new(
		[index, index * iScale, index],
		[atk, rel],
		[cAtk, cRel])
	);

	env = EnvGen.kr(Env.perc(atk, rel, curve: [cAtk, cRel]),
		doneAction: Done.freeSelf);

	// possible to use PMOsc too (don't forget to .mod(2pi) on the phase)
	sig = 2.collect({
		var mod, car;

		mod = SinOsc.ar(freq * mRatio,
			LFNoise1.kr(Rand(0.1, 0.2)).range(0, 2pi),
			mul: freq * mRatio * iEnv);
		car = SinOsc.ar(freq * cRatio + mod) * env * amp;

		car
	});

	sig = Splay.ar(sig, spread: spread, center: pan);

	sig = BPF.ar(sig, freq, rq * (1 + SinOsc.kr(0.1, mul: 0.1)));

	Out.ar(out, sig);
}).add;
)

Synth("fm")

(
Pdef(\base, Pbind(
	\instrument, \fm,
	\dur, 1,
	\out, ~busFx,

	\octave, 4,
	\note, 0,

	\atk, Pkey(\dur) * 0.05,
	\rel, Pkey(\dur) * 0.9,

	\mRatio, 5,
	\cRatio, 20,
));
)

Pdef(\base).stop;

Pdef(\base).play(t);

Pdef.clear;

