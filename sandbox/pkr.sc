(
SynthDef(\test, {
    arg out = 0, freq = 440, amp = 1, atk = 0.01, rel = 1.0, curve = -4;
    var env, sig;

    env = EnvGen.kr(
        Env.perc(atk, rel, curve: curve),
        doneAction: Done.freeSelf
    );

    sig = SinOsc.ar(freq.dup) * env * amp * 0.3;
    Out.ar(out, sig);
}).add;
)

(
SynthDef(\perc, {
    arg out = 0, amp = 1, freq = 440, fm_ratio = 2, fm_max = 20, pan = 0, atk = 0.01, rel = 0.2;

    var env, sig, freqMod, fmAmp;

    env = EnvGen.kr(Env.perc(atk, rel), doneAction: Done.freeSelf);

    fmAmp = freq * fm_ratio;
    freqMod = SinOsc.kr(fmAmp) * fmAmp;

    sig = SinOsc.ar(freq + freqMod) * env * amp * 0.1;
    Out.ar(out, Splay.ar(sig, center: pan));
}).add;
)

Synth(\perc);
s.freeAll;

(
var bpm = 130;

Pdef(\test, Pbind(
    \instrument, \perc,
    \out, 0,
    \amp, 0.5 * Pwhite(0.2, 1),
    \dur, (60 / bpm) / Pxrand([Pseq([1/2, 8, 4]), 2, 4, Pser([4, 8], 3)], inf),
    \atk, 0.00001,
    \rel, Pkey(\dur) * Pwhite(2, 4),
    \note, 10,
    \fm_ratio, 3,
    \pan, Pwhite(-0.5, 0.5),
)).play;
)

Pdef(\test).stop;
