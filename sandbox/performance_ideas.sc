
// use distorted piano loop, rate 1.95 (or 1.42), low trigger
// eq: lower 590 and 1085, increase 5145 
// also works well at lower rates (0.45, 0.61)
// try to adapt the EQ to cut out the frequencies based on the rate too
l.value('piano');
n.value('piano');
g.value('piano');

(
SynthDef(\revclap_notuseful, {
    arg out = 0, sdur = 3, amp = 1, atk = 0.01, rel = 0.3, nclaps = 1, freq = 1500, rq = 0.1;

    var sig, ampEnv, doneEnv, single;

    ampEnv = 0.5 * amp * EnvGen.kr(
        Env.perc(atk, rel)
    );

    doneEnv = EnvGen.kr(Env.perc(atk, rel * 3), doneAction: Done.freeSelf);

    single = WhiteNoise.ar()!2 * ampEnv;
    sig = BPF.ar(single, freq, rq);

    nclaps.do({
        var delay, amp, delaySig;

        delay = LFNoise0.kr(1).range(0.01, 0.03);
        amp = (0.3 - delay) / 0.3;

        delaySig = CombC.ar(single, delay, delay, delay);
        delaySig = BPF.ar(delaySig, freq * Rand(0.8, 1.2), rq);

        sig = sig + delaySig;
    });

    sig = LeakDC.ar(sig);
    Out.ar(out, sig);
}).add;

SynthDef(\kick, {
    arg out = 0,
        freqStart = 300, freqEnd = 50, freqDur = 2, freqCurve = -6,
        amp = 1, ampCurve = -3,
        startPhase = 0;
    var freqEnv, ampEnv, sig;

    freqEnv = EnvGen.kr(
        Env.new([freqStart, freqEnd], [freqDur], freqCurve)
    );

    ampEnv = 0.5 * amp * EnvGen.kr(
        Env.new([1, 0], [freqDur * 1.1], ampCurve),
        doneAction: Done.freeSelf
    );

    sig = SinOsc.ar(freqEnv, startPhase).dup * ampEnv;
    Out.ar(out, sig);
}).add;

SynthDef(\reverb, {
    arg in, out = 0, wetDry = 0.2;

    var inSig, sig, nrev;

    inSig = In.ar(in, 2);
    sig = inSig;
    nrev = 8;
    nrev.collect({ |i|
        sig = AllpassC.ar(inSig,
            delaytime: LFNoise1.kr(0.03!2).range(0.01, 0.02 * (i + 1)),
            //decaytime: (nrev - i),
            //decaytime: nrev - i,
            decaytime: (nrev / 2 - i).abs + 0.05
        ).tanh
    }).mean;

    sig = Mix([
        sig * (1 - wetDry),
        inSig * wetDry
    ]);

    sig = LeakDC.ar(sig);

    Out.ar(out, sig);
}).add;

SynthDef(\revclap, {
    arg out = 0, amp = 1, nclaps = 4, rel = 0.2, freq = 1200, rq = 0.5, curve = -5;//, wetDry = 0, nrev = 4;

    var doneEnv, sig, revSig;

    doneEnv = XLine.kr(dur: 5, doneAction: Done.freeSelf);

    sig = 0;
    
    nclaps.do({
        var env, single, singleFreq;
        env = EnvGen.kr(
            Env.new(
                [0.0001, 1, 0.0001, 1, 0.0001, 1, 0],
                [0.01, 0.0001, 0.01, 0.0001, 0.01, rel],
                ['lin', 'lin', 'lin', 'lin', 'lin', curve]
            )
        );

        singleFreq = freq * Rand(0.9, 1.1);

        single = WhiteNoise.ar()!2 * env;
        single = BPF.ar(single, singleFreq, rq) / (2 + nclaps).log; 
        sig = sig + single;
    });

    sig = sig * 0.5 * amp;
    sig = LeakDC.ar(sig);

    Out.ar(out, sig);
}).add;

SynthDef(\insound, {
    arg out = 0, sdur = 5, amp = 1, ampCurve = -5, freqStart = 100, freqMin = 50, freqMax = 5000, rq = 1.0;
    var sig, ampEnv, freqSig, freqMod, freqAmp, rqSig;
    
    // amplitude
    ampEnv = 0.5 * amp * EnvGen.kr(
        Env.new([0.001, 1], [sdur], ampCurve),
        doneAction: 2
    );

    // filter freq modulation
    freqMod = XLine.kr(1, 50.0, sdur * 0.95);
    freqAmp = [
        XLine.kr(freqStart, freqMin, sdur),
        XLine.kr(freqStart, freqMax, sdur)
    ];

    freqSig = LFNoise1.kr(freqMod).range(freqAmp[0], freqAmp[1]);

    // signal (filtered noise)
    sig = PinkNoise.ar();
    sig = BPF.ar(sig, freqSig, rq).dup;
    sig = sig * ampEnv;

    Out.ar(out, sig);
}).add;
)



(
var rel = 0.3;
Env.new(
    [0.0001, 1, 0.0001, 1, 0.0001, 1, 0],
    [0.01, 0.0001, 0.01, 0.0001, 0.01, rel],
    ['lin', 'lin', 'lin', 'lin', 'lin', -5]
).plot;
)

(
Synth(\revclap, [
    \amp, 0.5,
    \rel, 0.1,
    \freq, 1200,
    \rq, 0.4,
    \nclaps, 3,
]);
)

e.reverbBus = Bus.audio(s, 2);
(
r = Synth(\reverb, [
    \in, e.reverbBus,
    \out, 0
]);
)
r.set(\wetDry, 0.8);

(
Pdef(\p_revclap, Pbind(
    \instrument, \revclap,
    \dur, Pseq([1], 1),
    \amp, 0.5,
    \rel, 0.1,
    \freq, 1200,
    \rq, 0.4,
    \nclaps, 3,
    \nrev, 10,
    \wetDry, 0.1,
)).play;
)

(
Pdef(\p_insound, Pbind(
    \instrument, \insound,
    \dur, Pseq([5], 1),
    \freqStart, 100,
    \freqMin, 400,
    \freqMax, 1000,
    \amp, 0.5,
    \sdur, Pkey(\dur),
    \rq, 0.25,
));
)

(
var start = 0.05;

Pdef(\explosion, Pseq([
    Pdef(\p_insound),
    Ppar([
        Pdef(\p_kick),
        Pbind(
            \instrument, \revclap,
            \out, e.reverbBus,
            \dur, Pgeom(start, 1.1, 30),
            \rel, Pkey(\dur) * 0.95,
            \amp, 0.5 + Pwhite(0.05, 0.1),
            \rel, Pkey(\dur) * 0.9,
            \freq, 1200 * Pwhite(0.95, 1.05),
            \rq, 0.4,
            \nclaps, Pxrand([3, 4, 5, 6], inf),
            \curve, Pgeom(-5, 0.95, inf),
        ),
    ])
], inf));
)
Pdef(\explosion).play;
Pdef(\explosion).stop;

(
var a;
a = Pgeom(1.0, 1.1, inf);
a.asStream.nextN(100).plot;
)

Synth(\kick);
(
Pdef(\p_kick, Pbind(
    \instrument, \kick,
    \dur, Pseq([2], 1),
    \freqStart, 130,
    \freqEnd, 45,
    \freqDur, 2,
    \freqCurve, -10,
    \ampCurve, -3,
    \startPhase, pi / 10,
)).play;
)

Pdef(\patKick).gui;


// XLine experiments
l.value('perc');
(
SynthDef(\xline_play, {
    arg out = 0, start, end, xdur, bufnum;
    var env, sig, trig;

    // trigger
    trig = XLine.kr(start, end, xdur, doneAction: 2);
    trig = Impulse.kr(trig);

    // the sound
    sig = Pan2.ar(PlayBuf.ar(1, bufnum, rate: Rand(0.5, 1.5), trigger: trig)) * 0.2;
    Out.ar(out, sig);
}).add;
)

e.xlineBus = Bus.audio(s, 2);

(
Ndef(\xline, {
    In.ar(e.xlineBus, 2)
});
)

Ndef(\xline).gui;

Ndef(\xline).stop;

Ndef(\synthrev).gui;
Ndef(\synthrev) <<>.in Ndef(\xline);

(
Pdef(\xline_play, Pbind(
    \instrument, \xline_play,
    \out, e.xlineBus,
    \dur, 4,
    \bufnum, e.buffers['perc'].bufnum,
    \start, 30,
    \end, 1,
    \xdur, 2
)).play;
)

(
Synth(\xline_play, [
    \out, e.xlineBus,
    \bufnum, e.buffers['perc'].bufnum,
    \start, 30,
    \end, 1,
    \xdur, 10
]);
)

s.meter;
s.makeGui;



s.stop;
