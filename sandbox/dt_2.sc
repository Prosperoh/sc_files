s.boot;



z = NdefMixer(s);


(
Ndef(\in).copy(\p_dt_in);

~p_dt_in = Bus.audio(s, 2);
Ndef(\p_dt_in).set(\in, ~p_dt_in);
Ndef(\master) <<>.in1 Ndef(\p_dt_in);
)

(
SynthDef.new(\dt_chords, {
    arg out = 0,
        gate = 1,
        vel = 0.5,
        freq = 220,
        beginffreq = 440,
        endffreq = 220,
        cffreq = -5,
        att = 0.01,
        susLevel = 1,
        sus = 0,
        rel = 4,
        rq = 0.7,
        curve = -4,
        cFreq = 10,
        pgain = 1;

    var sig, env, ffreq;

    env = EnvGen.kr(
        /*Env.new([0, 1, susLevel, 0],
                [att, sus, rel],
                curve: curve),*/
        Env.perc(att, rel, curve: curve),
        gate: gate,
        doneAction: 2,
    ) * vel;

    // freq asymetry for stereo effect
    freq = freq + LFNoise1.kr(0.5!2);

    sig = SawDPW.ar(freq) * env;

    ffreq = EnvGen.kr(
        Env.new(
            [beginffreq, endffreq],
            [att + rel],
            curve: cffreq)
    );

    sig = RLPF.ar(sig, 
        ffreq,
        rq);
    sig = BPF.ar(sig, ffreq, rq);

    Out.ar(out, sig * pgain);
}).add;
)

(
Ndef(\fx_chain, {
    arg in, bpm = 120,
        delayLeft = 0.25, delayRight = 0.25, feedback = 0.1,
        dist = 1;

    var sig,
        delay = [delayLeft, delayRight],
        delaytime = bpm * delay / 60.0,
        minFeedback = (1/20),
        decaytime = delaytime * 0.001.log() / feedback.log(),
        alpha = 0.05;

    sig = In.ar(in, 2);

    sig = sig.collect({ |msig, i|

        msig + CombL.ar(msig,
            maxdelaytime: decaytime[i],
            delaytime: delaytime[i],
            decaytime: decaytime[i],
        )
    });

    // distortion
    sig = sig.pow([
        (1 / dist) * rrand(1 - alpha, 1 + alpha),
        (1 / dist) * rrand(1 - alpha, 1 + alpha),
    ]).tanh();

    sig
});
ControlSpec.add(\dist, [0.75, 3, \lin]);
)

Ndef(\master).stop;

Ndef(\fx_chain).set(\in, Ndef(\master).bus);

(
Ndef(\fx_chain).set(\dist, Ndef(\dist_fx, {
    LFNoise1.kr(0.5).range(1.1, 1.2)
    + Dust.kr(0.5).range(0, 0.5).lag(3)
}));
)

~bpm = 120;

Ndef(\conv).play(0, addAction: 'addToTail');
Ndef(\conv).stop;

(
var alpha = 0.02;
Ndef(\fx_chain).set(
    \delayLeft, (4/16) * {rrand(1 - alpha, 1 + alpha)},
    \delayRight, (4/16) * {rrand(1 - alpha, 1 + alpha)},
    \bpm, ~bpm,
    \feedback, 0.5,
);
)

(
Pdef(\test, Pbind(
    \instrument, \dt_chords,
    \out, 0,
    \dur, 1,
)).stop();
)


// try again with simpler chords, but add a filter envelope
(
var durSeq;

durSeq = (2/6)!3 ++ [(4/6)];

~bpm = 120;
~quant = ~bpm / 60.0 * durSeq.sum;

Pdef(\p_dt, Pbind(
    \instrument, \dt_chords,
    \out, ~p_dt_in,
    \dur, Pseq(durSeq, inf),
    \stretch, ~bpm / 60.0,
    \vel, Pseq([
        Pfunc({[0.5, 0.8, 0.7].scramble}, inf),
    ], inf),
    \att, Pkey(\dur) * 0.1,
    \rel, Pkey(\dur) * 0.5,
    \beginffreq, Pwhite(500, 600, inf) * 1,
    \endffreq, Pwhite(110, 220, inf) * 1.2,
    \rq, 0.8,
    \degree, Pseq([
        [4, 6, 8],
    ], inf),
    \mtranspose, 2,
    \gtranspose, Pwhite(-0.1, 0.1, inf),
    \scale, Scale.minor,
    \octave, 3,
    \pgain, 1,
)).quant_(~quant);
)


// chord progression idea, to be explored later
/*
(
var durSeq;

durSeq = [1.25, 2, 1.75, 3];

~bpm = 120;
~quant = ~bpm / 60.0 * durSeq.sum;

Pdef(\p_dt, Pbind(
    \instrument, \dt_chords,
    \out, ~p_dt_in,
    \dur, Pseq(durSeq, inf),
    \stretch, ~bpm / 60.0,
    \vel, 0.5,
    \att, Pkey(\dur) * 0.3,
    \rel, Pkey(\dur) * 1.8,
    \beginffreq, Pwhite(220, 2200, inf),
    \endffreq, Pwhite(220, 2200, inf),
    \rq, 0.8,
    \degree, Pseq([
        [0, 2, 4+7, 6],      // i
        [5, 7, 9-7, 11],    // v
        [4+7, 6, 8-7, 10],    // iv
        [2, 4+7, 6, 8-7],      // III
    ], inf),
    \mtranspose, 0,
    \gtranspose, Pwhite(-0.1, 0.1, inf),
    \scale, Scale.minor,
    \octave, 3,
    \pgain, 1,
)).quant_(~quant);
)
*/

Pdef(\p_dt).play(quant: ~quant);
Pdef(\p_dt).stop;

Ndef(\eq).copy(\p_dt_eq);
Ndef(\p_dt_eq) <<> Ndef(\p_dt_in);
Ndef(\master) <<>.in1 Ndef(\p_dt_eq);


Ndef(\conv) <<> Ndef(\fx_chain);

s.plotTree;
s.makeGui;


