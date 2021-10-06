z = NdefMixer(s);




// NEXT STEP
// design a nice grainy bass
// for inspiration look to Sa Pa - In A Landscape



(
SynthDef.new(\dt_chords, {
    arg out = 0,
        gate = 1,
        vel = 0.5,
        freq = 220,
        ffreq = 440,
        att = 0.01,
        susLevel = 1,
        sus = 0,
        rel = 4,
        rq = 0.7,
        curve = -4,
        cFreq = 10,
        pgain = 1;

    var sig, env;

    env = EnvGen.kr(
        Env.new([0, 1, susLevel, 0],
                [att, sus, rel],
                curve: curve),
        //Env.perc(att, rel, curve: curve),
        gate: gate,
        doneAction: 2,
    ) * vel;

    // freq asymetry for stereo effect
    freq = freq + LFNoise1.kr(0.5!2);

    sig = SawDPW.ar(freq) * env;

    sig = RLPF.ar(sig, 
        env.range(
            (ffreq * ((cFreq + 3) * vel).max(1)).min(19000),
            (ffreq * (3 * vel).max(1)).min(19000)
        ),
        rq);
    sig = BPF.ar(sig, ffreq, rq);

    Out.ar(out, sig * pgain);
}).add;
)

(
Ndef(\fx_chain, {
    arg in, bpm = 120,
        delayLeft = 0.25, delayRight = 0.25, feedback = 0.1;

    var sig,
        delay = [delayLeft, delayRight],
        delaytime = bpm * delay / 60.0,
        minFeedback = (1/20),
        decaytime = delaytime * 0.001.log() / feedback.log();

    sig = In.ar(in, 2);

    sig = sig.collect({ |msig, i|

        msig + CombL.ar(msig,
            maxdelaytime: decaytime[i],
            delaytime: delaytime[i],
            decaytime: decaytime[i],
        )
    });

    sig = sig.pow([0.79, 0.81]).tanh();

    sig
});
)

Ndef(\master).bus;

Ndef(\fx_chain).set(\in, Ndef(\master).bus);
Ndef(\conv) <<>.in Ndef(\fx_chain);

(
Ndef(\in, {
    arg in;

    In.ar(in, 2)
});

Ndef(\in).copy(\p_dt_in);
Ndef(\in).copy(\p_dt_mid_in);

~p_dt_in = Bus.audio(s, 2);
~p_dt_mid_in = Bus.audio(s, 2);

Ndef(\p_dt_in).set(\in, ~p_dt_in);
Ndef(\p_dt_mid_in).set(\in, ~p_dt_mid_in);
)

Ndef(\master) <<>.in1 Ndef(\p_dt_in);
Ndef(\master) <<>.in2 Ndef(\p_dt_mid_in);
~bpm = 85;

(
var stretch = 1;
var durSeq = [0.75, 1.75, 4.5];
~quant = ~bpm / 60.0 * durSeq.sum;

Pdef(\p_dt, Pbind(
    \instrument, \dt_chords,
    //\out, 0,
    \out, ~p_dt_in,
    \dur, Pseq(durSeq, inf) * stretch,
    \stretch, ~bpm / 60.0,
    \vel, Pseq([0.8, 0.9, 0.2].collect({|i|
        [1, 1, 0.8, 0.5] * i
    }), inf),
    \att, Pseq([1, 1, 5], inf) * Pexprand(0.03, 0.1, inf) * stretch,
    \rel, Pexprand(0.3, 0.6, inf) * stretch,
    \degree, Pstutter(3, Pseq([
        [0, 2, 4, 8],
    ], inf), inf),
    \ffreq, Pbrown(0.5, 2, 0.3, inf)
        * Pseq([
            200, // increase throughout the track
            300, // increase throughout the track
            150,
        ], inf),
    \rq, Pbrown(0.7, 0.8, 0.1, inf),
    \mtranspose, 0,
    \cFreq, 0,
    \gtranspose, 1 + Pwhite(-0.1, 0.1, inf),
    \scale, Scale.minor,
    \legato, 0.01,
    \octave, 3,
    \pgain, 1,
)).quant_(~quant);
)

Pdef(\p_dt).play(quant: ~quant);
Pdef(\p_dt).stop;

(
var stretch = 1;
var pat_dur = Pwhite(16, 32, inf);
Pdef(\p_dt_mid, Pbind(
    \instrument, \dt_chords,
    //\out, 0,
    \out, ~p_dt_mid_in,
    \dur, pat_dur,
    \stretch, ~bpm / 60.0,
    \vel, 1,
    \att, pat_dur / 8,
    \rel, pat_dur / 32,
    \degree, Pstutter(1, Pseq([
        [4, 8, 14],
    ], inf), inf),
    \ffreq, Pbrown(500, 1000, 100, inf),
    \rq, Pbrown(0.7, 0.8, 0.1, inf),
    \mtranspose, 0,
    \cFreq, 0,
    \gtranspose, 1 + Pwhite(-0.1, 0.1, inf),
    \scale, Scale.minor,
    \octave, 7,
    \pgain, 0.03,
));
)
Pdef(\p_dt_mid).play(quant: ~bpm / 60.0 * 4);
Pdef(\p_dt_mid).stop;


(
var alpha = 0.02;
Ndef(\fx_chain).set(
    \delayLeft, (6/16) * {rrand(1 - alpha, 1 + alpha)},
    \delayRight, (10/16) * {rrand(1 - alpha, 1 + alpha)},
    \bpm, ~bpm,
    \feedback, 0.6,
);
)

Ndef(\master).play(~fx_in);


// amp1: noise granular
// 202972 noise zoom
l.value('noise');
e.sb['pstretch'].value('noise');
h.value('noise');

Ndef(\eq).copy(\noise_eq);
Ndef(\noise_eq) <<>.in Ndef(\noise);


// amp2: pinknoise
(
Ndef(\pinknoise, {
    var sig;
    
    sig = PinkNoise.ar();
    sig = sig.pow(0.7).tanh();
    sig = sig * LFNoise1.kr(0.5!2).range(0.5, 1) * 0.2;
    sig = sig.pow(0.9).tanh();

    sig = BRF.ar(sig,
        freq: LFNoise0.kr(0.5).range(400, 8000).lag(2),
        rq: LFNoise0.kr(0.5!2).range(0.1, 0.5).lag(2)
    );

    LeakDC.ar(sig)
});
)

Ndef(\eq).copy(\pn_eq);
Ndef(\pn_eq) <<> Ndef(\pinknoise);


// amp3: asmr tapping
// tidal_longersounds -> asmr -> tapping
l.value('asmr');
n.value('asmr');
g.value('asmr');


// amp4: sing shivaya 402772
l.value('sing');
//e.sb['pstretch'].value('sing');
//h.value('sing');

(
// simple looped playbuf within section
Ndef(\sing, {
    arg bufnum, pos = #[0, 1], sgate = 0, rate = 1;

    var bufFrames, startLoop, endLoop;

    bufFrames = BufFrames.kr(bufnum);
    startLoop = (bufFrames * pos[0]).asInteger;
    endLoop = (bufFrames * pos[1]).asInteger;

    LoopBuf.ar(
        numChannels: 1,
        bufnum: bufnum, 
        rate: rate * BufRateScale.kr(bufnum), 
        gate: sgate, 
        startPos: startLoop,
        startLoop: startLoop,
        endLoop: endLoop
    ).dup
});
ControlSpec.add(\sgate, [-1, 2, \lin]);
)
Ndef(\sing).set(\bufnum, e.buffers['sing'].bufnum);

Ndef(\sing).gui


(
Ndef(\noise_balancer, {
    var ins, sigs, period;
    
    period = \period.kr(10);

    ins = [
        \in1.ar([0, 0]) * \amp1.kr(1),
        \in2.ar([0, 0]) * \amp2.kr(1),
        \in3.ar([0, 0]) * \amp3.kr(1),
        \in4.ar([0, 0]) * \amp4.kr(1),
    ];

    sigs = ins.collect({ |item|
        EnvGen.kr(
            Env.perc(
                period * Rand(0.3, 0.5),
                period * Rand(0.5, 0.7),
            ),
            gate: Dust.kr(Rand(0.8, 1.2) / period),
        ).range(\low.kr(0), 1) * item
    });

    LeakDC.ar(Mix(sigs))
});
ControlSpec.add(\low, [0, 1, \lin]);
(0..7).do({ |i|
    var symbol = ('amp' ++ i.asSymbol).asSymbol;
    ControlSpec.add(symbol, [0, 4, \lin]);
});
)

Ndef(\noise_balancer) <<>.in1 Ndef(\noise_eq);
Ndef(\noise_balancer) <<>.in2 Ndef(\pn_eq);
Ndef(\noise_balancer) <<>.in3 Ndef(\asmr);
Ndef(\noise_balancer) <<>.in4 Ndef(\sing);

Ndef(\noise_balancer).clear;






s.boot;
StageLimiter.activate;
StageLimiter.deactivate;
s.plotTree;
s.makeGui;
