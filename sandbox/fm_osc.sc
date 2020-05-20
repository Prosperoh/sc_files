(
SynthDef(\fm, {
	arg freq = 500, mRatio = 1, cRatio = 1, index = 1, iScale = 5,
	amp = 0.2, atk = 0.01, rel = 3, cAtk = 4, cRel = -4, pan = 0;

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

	car = SinOsc.ar(freq * cRatio + mod) * env * amp;
	car = Pan2.ar(car, pan);

	Out.ar(0, car);
}).add;
)

Synth(\fm, [\freq, 35.midicps, \rel, 4, \index, 1, \iScale, 8, \mRatio, 2]);

(
Pdef(\fm, Pbind(
	\instrument, \fm,
	\dur, 0.9 / Prand([
		Pshuf([4, 4, 8, 8, 2, 4]),
		Pseq([4, 8, 8, 2]),
		Pseq([16], 8),
	], inf),
	\atk, Pexprand(0.001, 0.1, inf),
	\note, 50 + Pseq(0!6 ++ [-1, 5], inf),
	\rel, Pkey(\dur) * 1.1 * Pexprand(0.1, 3),
	\index, Pexprand(500, 100, inf),
	\mRatio, 5,
	\iScale, 0.1 / Pkey(\index),
	\pan, Pwhite(-1, 1, inf) * 0.3,
)).play;
)

Pdef(\fm).stop;