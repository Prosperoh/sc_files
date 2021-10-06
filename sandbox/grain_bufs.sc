z = NdefMixer(s)
s.meter;



(
SynthDef.new(\grains, {
    arg out = 0,
        buf,
        amp = 0.2,
        atk = 3,
        rel = 3,
        cAtk = -2,
        cRel = -4,
        sus = 0,
        rate = 1,
        density = 10,
        sampleWidth = 1,
        ngrains = 4,
        pos = #[0, 1],
        stereoWidth = #[0, 1],
        envbuf = -1;

    var env, grains, sig, spreadSig;

    env = EnvGen.kr(
        Env.linen(atk, sus, rel, curve: [cAtk, 0, cRel]),
        doneAction: 2
    );

    grains = 4.collect({ |i|
        var trig = Dust.ar(density);
        var posSig = Dwhite(pos[0], pos[1]);
        var sig, chain, fftSize = 1024;

        Demand.ar(trig, 0, [posSig]);

        sig = GrainBuf.ar(
            numChannels: 1,
            trigger: trig,
            dur: sampleWidth / density,
            sndbuf: buf,
            rate: rate,
            pos: posSig,
            envbufnum: envbuf);

        sig
    });

    spreadSig = LFNoise1.kr(
        LFNoise1.kr(0.5).range(0.3, 0.7)
    ).range(stereoWidth[0], stereoWidth[1]);

    sig = Splay.ar(grains, spread: spreadSig);
    sig = sig * env * amp;

    Out.ar(out, sig);
}).add;
)

(
SynthDef.new(\bass, {
    arg out = 0, amp = 0.5, freq = 220,
        atk = 0.01, rel = 1, cAtk = 0, cRel = -4;

    var sig, env;

    env = EnvGen.kr(
        Env.perc(atk, rel, curve: [cAtk, cRel]),
        doneAction: 2
    );

    sig = SawDPW.ar(freq);
    sig = LPF.ar(sig, freq * 1.1);

    sig = Pan2.ar(sig) * env * amp;

    Out.ar(out, sig);
}).add;
)

(
Synth(\bass, [
    \out, 0,
    \freq, 220,
]);
)

(
Synth(\grains, [
    \buf, e.buffers['test'],
    \sampleWidth, 2,
    \density, 30,
    \pos, #[0, 0.6],
]);
)

Env.perc(8, 18, curve: [-2, -5]).plot;

e.events = ();

(

// maybe use more variation on all this, maybe more silences, and use the reverb to play with it

var chordSeq, bassSeq;
var dur;

// that was the wrong chord [-15, 8] but that's okay it's actually interesting, maybe better than [−15, -8]
//chordSeq = [[-12, -5], [-10, -5], [-15, 8]];
chordSeq = [[-12, -5], [-10, -5], [-17, 0], [-15, -8]];

bassSeq = chordSeq.collect({ |chord| chord[0] });

dur = Pwhite(8, 18, inf);

Pdef(\g, Pbind(
    \instrument, \grains,
    \dur, dur,
    \out, e.buses['g'],
    \atk, Pkey(\dur) * 0.5,
    \rel, Pkey(\dur) * 1.4,
    \buf, e.buffers['test'].bufnum,
    \rate, 2 ** (-7 + Pseq(chordSeq, inf) / 12),
    \density, 5,
    \pos, #[0.2, 0.8],
    \sampleWidth, 30,
    \amp, 0.8,
    \stereoWidth, #[0.4, 0.7],
).collect({ |event| y = event })
).play(quant: Quant(quant: 2, timingOffset: 0.02));
)


Pdef(\g).stop;
Pdef(\g).play(quant: 2);


e.buffers['test'].bufnum

e.buses['g'] = Bus.audio(s, 2);
Ndef(\fx).set(\in, e.buses['g']);

(
Ndef(\fx, {
    arg in, ffreq = 440, rq = 0.5, fmix = 0.5;

    var sig;

    sig = In.ar(in, 2);

    //sig = CombC.ar(sig);

    sig = (fmix * BRF.ar(sig, ffreq, rq)) + ((1 - fmix) * sig);

    sig
});
ControlSpec.add(\ffreq, ControlSpec.specs['freq']);
ControlSpec.add(\fmix, [0, 1, \lin]);
)

(
Ndef(\fx).set(\ffreq, Ndef(\fx_ffreq,
    { arg freq = #[440, 4400];
        LFNoise1.kr(0.5).range(freq[0], freq[1])
    }
));
Ndef(\fx).set(\rq, Ndef(\fx_rq,
    { arg rq = #[0.1, 0.2];
        LFNoise1.kr(0.5).range(rq[0], rq[1])
    }
));
)

ControlSpec.specs['freq'];

s.plotTree

// use dubsynth03 (or another one, don't remember,
// but it will be recognizable in how it sounds)
l.value('test');
n.value('test');
g.value('test');



// use one from pianofx or pad, don't remember
l.value('test2');
n.value('test2');
g.value('test2');


(
Pdef(\gg, Pbind(
    \instrument, \grains,
    \dur, Pwhite(8, 15, inf),
    \out, e.buses['g'],
    \atk, Pkey(\dur) * (0.5 + Pwhite(0, 0.3, inf)),
    \sus, Pkey(\dur) * 0.7,
    \rel, Pkey(\dur) * (0.5 + Pwhite(0, 0.3, inf)),
    \buf, e.buffers['test3'].bufnum,
    \rate, 2 ** (Pseq([[-11, 1], [-14, 6]], inf) / 12),
    \density, 5,
    \pos, #[0.2, 0.8],
    \sampleWidth, 30,
    \amp, 0.5,
    \stereoWidth, #[0.4, 0.6],
));
)

Pdef(\gg).play
Pdef(\gg).stop

(
Ndef(\lower, {
    arg in, freq = 440;

    var sig;

    sig = In.ar(in, 1);
    sig = LPF.ar(sig, freq);

    Pan2.ar(sig)
});
)

(
Ndef(\higher, {
    arg in, freq = 440;

    var sig;

    sig = In.ar(in, 2);
    sig = HPF.ar(sig, freq);

    sig
});
)

(
Ndef(\lower).set(\in, e.buses['g']);
Ndef(\higher).set(\in, e.buses['g']);
Ndef(\master) <<>.in1 Ndef(\lower);
Ndef(\master) <<>.in2 Ndef(\higher);
)


(
var pospat = Pseries(0, 0.33355, inf).mod(1);
var pospat2 = pospat + 0.1;
Pdef(\reg, Pbind(
    \instrument, \grains,
    \dur, Pseq([1.2, 1], inf) * Pwhite(13, 15, inf),
    \out, e.buses['g'],
    \atk, Pkey(\dur) * Prand([[0.5, 0.6], [0.4, 0.7]], inf),
    \sus, Pkey(\dur) * 0.1,
    \rel, Pkey(\dur) * 1.5,
    \cAtk, 0,
    \cRel, -5,
    \buf, e.buffers['test3'].bufnum,
    // \rate, 2 ** (Pseq([[-11, 1]], inf) / 12),
    \rate, 2 ** (Pseq([[-11, 1], [-16, 4]], inf) / 12),
    \pos, #[pospat, pospat2],
    \sampleWidth, 20,
    \density, 40,
    \amp, Pseq([0.4, 0.5], inf),
    \stereoWidth, #[0.4, 0.6]
));
)

t = (Pseries(0, 0.05, inf).mod(1)).asStream;
t.next;

Pdef(\reg).play
Pdef(\reg).stop

l.value('test3');






// beginning again
(
var atkRelDur = 0.01;
var grainEnv = Env([0, 1, 1, 0], [atkRelDur, 1, atkRelDur], [0, 0, 0]);
//grainEnv.plot;
e.buffers['g_env'] = Buffer.sendCollection(s, grainEnv.discretize, 1);
)

(
Pdef(\ntest, Pbind(
    \instrument, \grains,
    //\dur, Pexprand(1.5, 0.1, inf),
    //\dur, Pbrown(0.25, 0.35, 0.05, inf),
    //\dur, Pbrown(0.5, 0.75, 0.05, inf),
    //\dur, 2 * Pbrown(1, 1.25, 0.05, inf),
    \dur, 2 * Pbrown(1, 1.25, 0.05, inf) * Pseg([1, 0.2], 180, \exp),
    \out, Pseq([[
        e.buses['ntest'],
        e.buses['ntest_pad'],
    ]], inf),
    //\atk, Pseq([[0, 0.1]], inf) * Pkey(\dur),
    \atk, 0,
    \sus, Pkey(\dur) * 0.8,
    \rel, Pkey(\dur) * 0.2,
    \cAtk, 0,
    \cRel, 0,
    \buf, Pseq([[
        e.buffers['ntest'].bufnum,
        e.buffers['ntest_pad'].bufnum,
    ]], inf),
    //\rate, 1,
    \rate, 2 ** (Pseq([[-12, 0], [-24, 0]], inf) / 12),
    \pos, #[0, 1],
    \sampleWidth, Pseq([10, 2], inf),
    \density, 40,
    \amp, Pseq([[0.2, 0.5], [0.2, 0.4]], inf),
    \stereoWidth, #[0.5, 0.5],
    \envbuf, e.buffers['g_env'].bufnum,
));
)

(
Ndef(\eq_in, {
    arg in, lpfreq = 18000, hpfreq = 30,
        freq1 = 100, rq1 = 1.0, db1 = 0.0,
        freq2 = 1000, rq2 = 1.0, db2 = 0.0,
        freq3 = 10000, rq3 = 1.0, db3 = 0.0;

    var sig;

    sig = In.ar(in, 2);

    // eq1
    sig = BPeakEQ.ar(sig, freq1, rq1, db1);

    // eq2
    sig = BPeakEQ.ar(sig, freq2, rq2, db2);

    // eq3
    sig = BPeakEQ.ar(sig, freq3, rq3, db3);

    // low pass
    sig = BLowPass.ar(sig, lpfreq, 1.5);

    // high pass
    sig = BHiPass.ar(sig, hpfreq, 1.5);

});
ControlSpec.add(\freq1, [30, 18000, \exp]);
ControlSpec.add(\rq1, [0.1, 10, \exp]);
ControlSpec.add(\db1, [-30, 20, \lin]);
ControlSpec.add(\freq2, ControlSpec.specs[\freq1]);
ControlSpec.add(\rq2, ControlSpec.specs[\rq1]);
ControlSpec.add(\db2, ControlSpec.specs[\db1]);
ControlSpec.add(\freq3, ControlSpec.specs[\freq1]);
ControlSpec.add(\rq3, ControlSpec.specs[\rq1]);
ControlSpec.add(\db3, ControlSpec.specs[\db1]);
)

Ndef(\eq_in).copy(\ntest_eq);
Ndef(\eq_in).copy(\ntest_pad_eq);
Ndef(\ntest_eq).set(\in, e.buses['ntest']);
Ndef(\ntest_pad_eq).set(\in, e.buses['ntest_pad']);


(
Ndef(\fx, {
    arg revmix = 0.5, revdur = 1;

    var sig, n = 8, revSig;

    sig = Mix([
        \in1.ar([0, 0]),
        \in2.ar([0, 0]),
        \in3.ar([0, 0]),
        \in4.ar([0, 0]),
    ]);
    revSig = sig;

    n.collect({ |i|
        revSig = AllpassC.ar(
            revSig,
            delaytime: LFNoise1.kr(0.03!2).range(0.01, 0.02 * (i+1)),
            //decaytime: (n - i) * revdur
            //decaytime: (i + 1) * revdur
            decaytime: ((n / 2 - i).abs + 0.1) * revdur
        ).tanh
    }).mean;

    Mix([
        (1 - revmix) * sig * LFNoise1.kr(3).range(0, 1),
        revmix * revSig
    ])
});
ControlSpec.add(\revmix, [0, 1, \lin]);
ControlSpec.add(\revdur, [0.5, 2, \exp]);
)


e.buses['ntest'] = Bus.audio(s, 2);
e.buses['ntest_pad'] = Bus.audio(s, 2);

Ndef(\fx) <<>.in1 Ndef(\ntest_eq);
Ndef(\fx) <<>.in2 Ndef(\ntest_pad_eq);

Pdef(\ntest).play;
Pdef(\ntest).stop;

// use 398926 glitch2
l.value('ntest');
n.value('ntest');
g.value('ntest');

// use 484634 carbnpad
l.value('ntest_pad');

e.buffers['g_env'].plot;
