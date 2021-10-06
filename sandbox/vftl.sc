s.boot;
s.quit;


(
SynthDef(\noise, {
    arg out = 0,
        amp = 0.2,
        atk = 0.01,
        rel = 0.2,
        stereo = 1.0,
        ffreqLow = 500,
        ffreqHigh = 15000,
        ffreqSpeed = 5,
        rq = 0.2;

    var nNoises = 8, sig, freeEnv;

    // DetectSilence does not seem to be very reliable :/
    freeEnv = EnvGen.kr(Env.perc(atk * 3, rel * 3), doneAction: 2);

    sig = nNoises.collect({
        var env_, sig_, ffreq_, atk_, rel_, rq_;

        atk_ = ExpRand(0.5 * atk, 2 * atk);
        rel_ = ExpRand(0.5 * rel, 2 * rel);

        env_ = EnvGen.kr(Env.perc(atk, rel));
        sig_ = WhiteNoise.ar();

        ffreq_ = LFNoise1.kr(ffreqSpeed * (atk_ + rel_).reciprocal);
        ffreq_ = ffreq_.exprange(ffreqLow, ffreqHigh);

        rq_ = rq * ExpRand(0.5, 2.0);

        sig_ = BPF.ar(sig_,
            ffreq_,
            rq_,
        );
        sig_ * env_ * amp / (nNoises.pow(0.05));
    });

    sig = Splay.ar(sig, spread: stereo);
    sig = LeakDC.ar(sig);
    Out.ar(out, sig);
}).add;
)

(
~noiseOut = Bus.audio(s, 2);
~tomsOut = Bus.audio(s, 2);
)

(
Pdef(\noise, Pbind(
    \instrument, \noise,
    \out, ~noiseOut,
    \stretch, (120 / 134.0), 
    //\dur, // Pseq([0.0625], inf),//Pwhite(0.01, 0.1, inf),
    \dur, Pbrown(0.05, 0.25, 0.01, inf),
    \atk, Pbrown(0.001, 0.05, 0.01, inf),
    \amp, Pbrown(0.2, 0.2, 0.01, inf) * Pwrand([0.0, 1.0], [1, 10].normalizeSum, inf),
    \rel, Pkey(\dur) * Pbrown(0.5, 2.0, 0.1, inf),
    \stereo, 0.5,
    \rq, 0.1,
    \ffreqSpeed, Pbrown(5.0, 100.0, 2.0, inf),
    \ffreqLow, Pexprand(1500, 2000, inf),
    \ffreqHight, Pexprand(3000, 15000, inf),
));
)
Pdef(\noise).play;
Pdef(\noise).stop;  



(
SynthDef.new(\tom, {
    arg out = 0,
        freq = 110,
        amp = 0.2,
        atk = 0.001,
        rel = 0.3;

    var sig, env;

    env = EnvGen.kr(Env.perc(atk, rel), doneAction: 2);

    sig = SinOsc.ar(
        XLine.kr(freq * 1.3, freq, 0.08),
        pi / 2.0,
    ).dup;

    sig = sig.pow(XLine.kr(0.8, 1, 0.05));

    sig = sig * env * amp;

    Out.ar(out, sig);
}).add;
)

Synth(\tom);

Ndef(\test, { SinOsc.ar(440).dup * 0.1 }).play;
Ndef(\test, { SinOsc.ar(440).dup * 0.1 }).stop;
s.meter;
s.quit;
s.boot;

(
var p, a;
p = Pstutter(10, Pbrown(0.01, 0.1, 0.02));
a = p.asStream;
20.do({ a.next.postln; });
)

(
Pdef(\toms, Pbind(
    \instrument, \tom,
    \out, ~tomsOut,
    \dur, Pstutter(10, Pwhite(0.02, 0.2, inf))
        * Pseq([Pgeom(1.0, 1.2, 10)], inf),
    \amp, 0.3,
    \atk, Pkey(\dur) * 0.01,
    \rel, Pkey(\dur) * 0.99,
    \octave, 3,
    \root, 5,
    \scale, Scale.lydian,
    \degree, Pstutter(
        Prand([1, 3, 5], inf),
        Pseq([2, -3, -4, 0], inf).round
    ),
));
)

Pdef(\toms).play
Pdef(\toms).stop
Pdef(\toms).clear;

s.plotTree;

(
Ndef(\mixer, {
    arg bus1, bus2, bus3, bus4;
    var master, buses;

    buses = [bus1, bus2, bus3, bus4];

    master = Mix(4.collect({ |i|
        In.ar(buses[i], 2) * ('amp' ++ (i+1).asSymbol).asSymbol.kr(1)
    }));
    
    master
});
)

(
Ndef(\mixer).set(\bus1, ~noiseOut);
Ndef(\mixer).set(\bus2, ~tomsOut);
)

Ndef(\mixer) <<>.amp1 Ndef(\amptest);
Ndef(\mixer) <<>.amp2 Ndef(\amptest);


Ndef(\mixer).clear;
Ndef(\mixer).gui;

(
Ndef(\amptest, {
    EnvGen.kr(
        Env.perc(0.00001, 2.0),
        gate: Dust.kr(0.3)
    )
});
)


l.value('grain');
n.value('grain');
g.value('grain');


s.freeAll;
s.meter;

s.boot;
