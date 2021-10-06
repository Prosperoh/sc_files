s.boot;
s.quit;


Buffer.freeAll;
(
var numSegs = 8;
~wt_buffer.do({ |buf| buf.free; });
~wt_buffer = 4.collect({ |i|
    var env, sig, buffer;
    
    env = Env.new(
        [0] ++ (({rrand(0.0, 1.0)}!(numSegs - 1)) * [1, -1]).scramble ++ [0],
        {exprand(1, 20)}!numSegs,
        {rrand(-20, 20)}!numSegs
    );

    sig = env.asSignal(1024);
    buffer = Buffer.loadCollection(s, sig.asWavetable);
    buffer
});
)


(
Ndef(\grainIn, {
    arg mix = 1.0, grainfreq = 8.0;

    var in;
    in = \in.ar([0, 0]);

    (in * (1 - mix)) + (mix * GrainIn.ar(
        numChannels: 1,
        trigger: Impulse.kr(grainfreq),
        dur: grainfreq.reciprocal * LFNoise1.kr(0.5).range(0.9, 1.5),
        in: in,
        envbufnum: -1
    ))
});
ControlSpec.add(\mix, [0, 1, \lin]);
ControlSpec.add(\grainfreq, [0.125, 64.0, \exp]);
)  

Ndef(\grainIn) <<> Ndef(\router);
Ndef(\conv) <<> Ndef(\grainIn);


s.plotTree;

~routerIn = Bus.audio(s, 2);
(
Ndef(\router, {
    In.ar(~routerIn, 2)
});
Ndef(\conv) <<> Ndef(\router);
)

Ndef(\conv).gui


z = NdefMixer(s);

// Synthdef version
(
SynthDef(\sounds, {
    arg out=0, freq=440, amp=0.2, 
        atk=2, rel=3, cAtk=0, cRel=(-2),
        ffreq=880, rq=1.0,
        harmonicMovAmp=0.05,
        harmonicMov=0.2,
        harmonicAttenuation=2.0;

    // starting with a collection of sinusoids
    var sig, nSines=16, harmonicDepth=16;

    sig = nSines.collect({
        var amp_, sine, env, bufpos;
        var harmonic, harmonicAmp, harmonicEnvScale;

        harmonic = ExpRand(1, harmonicDepth).round;
        harmonic = harmonic + LFNoise1.kr(harmonicMov).range(harmonicMovAmp.neg, harmonicMovAmp);
        harmonicAmp = harmonic.reciprocal.pow(harmonicAttenuation);

        bufpos = LFNoise1.kr(2.0).range(~wt_buffer[0].bufnum,
            ~wt_buffer[3].bufnum);
        sine = VOsc.ar(bufpos, freq * harmonic);
        sine = LPF.ar(sine, (freq * harmonic * 3.0).clip(19000));
        //sine = SinOsc.ar(freq * harmonic);
        sine = sine.pow(LFNoise1.kr(0.5).range(0.8, 0.95));

        harmonicEnvScale = (harmonic.neg / 32).exp;
        env = EnvGen.kr(
            Env([0, 1, 0], [atk, rel], [cAtk, cRel]),
            timeScale: harmonicEnvScale 
        );

        amp_ = LFNoise1.kr(0.05).range(0.5, 0.9) * harmonicAmp;

        amp_ * sine * env * nSines.log.reciprocal;
    });

    sig = Splay.ar(sig);

    sig = BPF.ar(sig,
        ffreq * LFNoise1.kr(0.4).range(0.95, 0.95.reciprocal),
        rq);

    DetectSilence.ar(Mix(sig), doneAction: 2);

    Out.ar(out, LeakDC.ar(sig * amp));
}).add;
)

s.plotTree;

s.meter;

Env([0, 1, 0], [1, 1], [-5, 5]).plot;



(
// this works!!!
// you can use this to create various synchronized patterns,
Pdef(\env, Penvir((), Ptpar([
    0.0, Pbind(
        \instrument, \sounds,
        \dur, Pwhite(5, 30, inf),
        \atk, Pkey(\dur) * Pwhite(0.05, 0.3),
        \rel, Pkey(\dur) * Pwhite(0.9, 1.2),
        \scale, Scale.dorian,
        \degree, Pseq([
            [0, 2, 4, 9],
            [-1, 3, 5, 7],
            [0, 3, 5, 8],
        ], inf),
        \root, -4,
        \harmonicAttenuation, Pdefn(\sounds_harmonicAttenuation),
        \amp, Pdefn(\sounds_amp),
        \ffreq, Pdefn(\sounds_ffreq),
        \rq, Pdefn(\sounds_rq),
        \harmonicMovAmp, Pbrown(0.005, 0.03, 0.02, inf),
        \harmonicMov, 5.0,
        \octave, 4,
        \out, ~routerIn,
        \timingOffset, 0.1,
    ).collect({ |event| ~lastEvent = event }),
    0.1, Pbind(
        \instrument, \sounds,
        \dur, Pfunc { ~lastEvent[\dur] },
        \timingOffset, Pkey(\dur) * Pwhite(0.02, 0.1),
        \atk, Pkey(\dur) * Pwhite(0.5, 0.7) - (Pkey(\timingOffset) / 4.0),
        \rel, Pkey(\dur) * Pwhite(0.5, 0.7),
        \scale, Pfunc { ~lastEvent[\scale] },
        \degree, Pfunc { ~lastEvent[\degree] },
        \root, Pfunc { ~lastEvent[\root] },
        \harmonicAttenuation, Pdefn(\sounds_harmonicAttenuation) * 1.5,
        \amp, Pdefn(\sounds_amp) * Pseq([0.7, 0.5], inf),
        \ffreq, Pdefn(\sounds_ffreq) * Pseq([0.5, 0.25], inf),
        \rq, Pdefn(\sounds_rq) / 2.0,
        \harmonicMovAmp, 0.1 * Pexprand(3.0, 0.5, inf) * Pbrown(0.02, 0.04, 0.01, inf),
        \harmonicMov, Pfunc { ~lastEvent[\harmonicMov] },
        \octave, Pfunc { ~lastEvent[\octave] } - Pseq([1, 2], inf),
        \out, Pfunc { ~lastEvent[\out] },
    )]
)));
)

Pdef(\env).play;
Pdef(\env).stop;
Pdef(\env).clear;


(
Pdefn(\sounds_harmonicAttenuation, 2.0);
Pdefn(\sounds_ffreq, 1500);
Pdefn(\sounds_rq, 1.2);
Pdefn(\sounds_amp, 5.0);
)


(
// for midi usage
Pdefn(\sounds_harmonicAttenuation, Ndef(\sounds_harmonicAttenuation, { \val.kr(1.0) }));
Pdefn(\sounds_amp, Ndef(\sounds_amp, { \val.kr(5.0) }));
Pdefn(\sounds_ffreq, Ndef(\sounds_ffreq, { \val.kr(1000) }));
Pdefn(\sounds_rq, Ndef(\sounds_rq, { \val.kr(1.0) }));
)

// just midi for amplitude
(
Pdefn(\sounds_amp, Ndef(\sounds_amp, { \val.kr(5.0) }));
Pdefn(\sounds_amp2, Ndef(\sounds_amp2, { Ndef(\sounds_amp) * 0.5 }));
)

// midi
(
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
//m.gui;
)

(
m.elAt('kn', '1', '1').action_({ |el|
    var spec = ControlSpec.new(0.1, 10.0, \exp);
    Ndef(\sounds_harmonicAttenuation).set(\val, spec.map(1 - el.value).postln);
});
m.elAt('kn', '2', '1').action_({ |el|
    var spec = ControlSpec.new(0.1, 2.0, \exp);
    Ndef(\sounds_rq).set(\val, spec.map(1 - el.value).postln);
});
m.elAt('kn', '3', '1').action_({ |el|
    var spec = ControlSpec.specs['freq'];
    Ndef(\sounds_ffreq).set(\val, spec.map(el.value).postln);
});
)
// amp
(
m.elAt('sl', '1').action_({ |el|
    var spec = ControlSpec.new(0.0, 8.0, \lin);
    Ndef(\sounds_amp).set(\val, spec.map(el.value).postln);
});
)
Pdef(\pat).play;
Pdef(\pat).stop;

m.elAt('sl', '1');




// noise, synthdef version (to use with an envelope)
(
SynthDef(\noise, {
    arg out = 0,
        atk=2, rel=3, cAtk=0, cRel=(-2),
        dustFreq = 20,
        ffreq = 10000,
        rq = 1.0,
        gain = 1.0,
        scarceness = 1.0,
        stereoWidth = 1.0;

    var nDusts = 16;

    var sig, mSig, sSig, env;

    env = EnvGen.kr(
        Env([0, 1, 0], [atk, rel], [cAtk, cRel]),
        doneAction: 2
    );
    
    sig = nDusts.collect({
        var baseAmp, varAmp, pan;

        baseAmp = ExpRand(0.2, 0.8);
        varAmp = LFNoise1.kr(0.5).pow(scarceness);
        pan = LFNoise1.kr(0.2).range(-1, 1);
        Dust.ar(dustFreq) * baseAmp * varAmp
    });

    sig = Splay.ar(sig);

    //sig = sig + (SinOsc.ar(440!2) * 0.2);

    ffreq = ffreq * LFNoise0.kr(15).range(0.95, 1.05);
    rq = rq * LFNoise0.kr(20).range(0.95, 1.05);
    sig = BPF.ar(sig, ffreq, rq);

    mSig = sig[0] + sig[1];
    sSig = sig[0] - sig[1];

    sig[0] = mSig + (stereoWidth * sSig);
    sig[1] = mSig - (stereoWidth * sSig);

    Out.ar(out, LeakDC.ar(sig * env * gain));
}).add;
)



(
Pdef(\noise_pat, Pbind(
    \instrument, \noise,
    \dur, Pwhite(10, 20, inf),
    \atk, Pkey(\dur) * Pwhite(0.2, 0.4),
    \rel, Pkey(\dur) * Pwhite(0.8, 1.2),
    \gain, Pdefn(\noise_gain), 
    \rq, 1,
    \ffreq, Pdefn(\noise_ffreq),
    \dustFreq, 100,
    \stereoWidth, Ndef(\noise_stereoWidth, { LFNoise1.kr(0.2).range(0.2, 0.8) }),
    \out, ~routerIn,
));
)

(
Pdefn(\noise_gain, 5);
//Pdefn(\noise_ffreq, 3500);
Pdefn(\noise_ffreq, Ndef(\noise_ffreq, { LFNoise1.kr(0.5).range(1500, 5000) }));
)

(
// midi
Pdefn(\noise_gain, Ndef(\noise_gain, { \val.kr(5.0) }));
Pdefn(\noise_ffreq, Ndef(\noise_ffreq, { \val.kr(3500) }));
)


(
m.elAt('sl', '2').action_({ |el|
    var spec = ControlSpec.new(0.0, 8.0, \lin);
    Ndef(\noise_gain).set(\val, spec.map(el.value).postln);
});
m.elAt('kn', '3', '2').action_({ |el|
    var spec = ControlSpec.new(500.0, 18000, \exp);
    Ndef(\noise_ffreq).set(\val, spec.map(el.value).postln);
});
)

Pdef(\noise_pat).play;
Pdef(\noise_pat).stop;

(
Pdef(\pat2, Pbind(
    \instrument, \sounds,
    \dur, 
        //Pseq([0.5, 0.5], inf),
        Pseq([0.25, 0.75], inf),
    \atk, 0.001,
    \rel, 0.2,
    \scale, Scale.minor,
    \degree, Pseq([
        [0, 8],
    ], inf),
    \root, 0,
    \octave, 3,
    \harmonicMovAmp, Pbrown(0.05, 0.08, 0.01, inf) / Pkey(\octave),
    \harmonicAttenuation, 0.8,
    \amp, 3.0 * Pseq([1, 0.2], inf),
    \ffreq, Pwhite(1000, 2000, inf),
    \rq, Pbrown(0.5, 1.0, 0.2, inf),
    \out, ~routerIn,
));
)

Pdef(\pat2).play;
Pdef(\pat2).stop;




// let's play with noise
(
Ndef(\dust, {
    arg dustFreq = 20,
        ffreq = 10000,
        rq = 1.0,
        gain = 1.0,
        scarceness = 1.0,
        stereoWidth = 1.0;

    var nDusts = 16;

    var sig, mSig, sSig;
    
    sig = nDusts.collect({
        var baseAmp, varAmp, pan;

        baseAmp = ExpRand(0.2, 0.8);
        varAmp = LFNoise1.kr(0.5).pow(scarceness);
        pan = LFNoise1.kr(0.2).range(-1, 1);
        Dust.ar(dustFreq) * baseAmp * varAmp
    });

    sig = Splay.ar(sig);

    //sig = sig + (SinOsc.ar(440!2) * 0.2);

    ffreq = ffreq * LFNoise0.kr(15).range(0.95, 1.05);
    rq = rq * LFNoise0.kr(20).range(0.95, 1.05);
    sig = BPF.ar(sig, ffreq, rq);

    mSig = sig[0] + sig[1];
    sSig = sig[0] - sig[1];

    sig[0] = mSig + (stereoWidth * sSig);
    sig[1] = mSig - (stereoWidth * sSig);

    LeakDC.ar(sig * gain);
});
ControlSpec.add(\stereoWidth, [0, 1, \lin]);
)

Ndef(\dust).clear;

Ndef(\dust).play(~routerIn.index, 2, addAction: \addToHead);
Ndef(\dust).stop;










// Ndef version
(
Ndef(\sounds, {
    arg freq=440;

    // starting with a collection of sinusoids
    var sig,
        nSines = 32,
        chordFreqs;

    var midiroot = freq.cpsmidi;
    chordFreqs = [0, 2, 4].collect({ |n|
        (midiroot + n).midicps
    });

    sig = chordFreqs.collect({ |chordFreq|
        nSines.collect({
            var amp, sine;
            var harmonic, harmonicAmp;

            harmonic = exprand(1, 15).round;
            harmonic = harmonic * LFNoise1.kr(0.2).range(0.95, 1);
            harmonicAmp = harmonic.reciprocal ** 2;

            sine = SinOsc.ar(chordFreq * harmonic);
            sine = sine.pow(LFNoise1.kr(0.5).range(0.8, 0.95));

            amp = LFNoise1.kr(0.05).range(0.5, 0.9) * harmonicAmp;

            amp * sine * nSines.log.reciprocal;
        });
    });

    sig = sig.collect({ |signal| Splay.ar(signal); }).sum;

    sig = BPF.ar(sig, 1500, 5) * 8;

    sig
});
)

Ndef(\sounds).gui;

Ndef(\sounds).clear;

Ndef(\conv).gui;

s.boot;
