(
SynthDef(\fm, {
	arg freq = 500, mRatio = 1, cRatio = 1, index = 1, iScale = 5,
	amp = 0.2, atk = 0.01, rel = 3, cAtk = 4, cRel = -4, pan = 0, phase = 0;

	var car, mod,modAmp, env, iEnv;

	iEnv = EnvGen.kr(
		Env.new(
			[index, index * iScale, index],
			[atk, rel],
			[cAtk, cRel]
		)
	);

	env = EnvGen.kr(
		Env.perc(atk, rel, curve: [cAtk, cRel]),
		doneAction: Done.freeSelf
	);

	modAmp = freq * mRatio; // normalized amplitude, use index to change amplitude
	mod = SinOsc.ar(freq * mRatio, mul: freq * mRatio * iEnv);

	car = SinOsc.ar(freq * cRatio + mod, phase) * env * amp;
	car = Pan2.ar(car, pan);

	Out.ar(0, car);
}).add;
)

Synth(\fm, [\freq, 35.midicps, \rel, 4, \index, 1, \iScale, 8, \mRatio, 2]);

t = 85 / 60;

[10, 1].normalizeSum;

(
Pdef(\fm1, Pbind(
	\instrument, \fm,
	\dur, 1 / Pwrand([
		Pshuf(4!!4 ++ 2!!2 ++ 1!!1),
		Pseq(8!!32),
	],
	[10, 10].normalizeSum(), inf),
	\stretch, t,
	\amp, Pwhite(0.5, 1) * 0.3,
	\atk, Pexprand(0.001, 0.1, inf),
	\note, 20 + Pseq(0!6 ++ [-1, 5], inf) + Pif(Pkey(\dur) <= 0.2, 24, 0, 0),
	\rel, Pkey(\dur) * 1.1 * Pexprand(0.7, 1),
	\index, Pexprand(1000, 100, inf).round,
	\mRatio, 5,
	\iScale, 0.1 / Pkey(\index),
	\pan, Pwhite(-1, 1, inf) * 0.3,
//)).play(quant: t);
)).quant_(t);
)

Pslide(#[1, 2, 3, 4, 5, 6, 7, 8], 10, 3, 1, 0, false).asStream.all;

Pslide(#[1, 2, 3, 4, 5, 6, 7, 8], 10, 3, 1, 0, false).clump(3).asStream.all;


(
Pdef(\fm2, Pbind(
	\instrument, \fm,
	\dur, 1,
	\amp, 0.5,
	\rel, 0.5,
	\phase, 0.3,
	\note, -40,
	\index, 1,
	\iScale, 15,
	\cAtk, \lin,
	\cRel, -10,
	\stretch, t
//)).play(quant: t);
)).quant_(t);
)

Pdef(\track, Ppar([Pdef(\fm1), Pdef(\fm2)], inf)).play;
Pdef(\track).stop;

Pdef(\fm1).stop;
Pdef(\fm2).stop;


ProxySpace.push(s.boot);


// With Ndef
(
Ndef(\fmlong, {
	arg freq = 500, mRatio = 1, cRatio = 1, index = 1,
	amp = 0.2, pan = 0, phase = 0;

	var car, mod, modAmp;

	modAmp = freq * mRatio; // normalized amplitude, use index to change amplitude
	mod = SinOsc.ar(freq * mRatio, mul: freq * mRatio * index);

	car = SinOsc.ar(freq * cRatio + mod, phase) * amp;
	car = Pan2.ar(car, pan);

	car
});
)

Ndef(\fmlong).clear;

(
Ndef(\fmlong)[1] = \set -> Pdef(\fmlong);
)

(
Pdef(\fmlong, Pbind(
	\dur, 1/16,
	\stretch, t,
	\index, Pstutter(4, Prand([1, 3, 6, 10], inf)),
	\amp, 0.2,
));
)

{ SinOsc.ar([440, 442], mul: 0.1) }.play;

Ndef(\fmlong).play;
Ndef(\fmlong).stop;
