s.boot;
s.quit;

z = NdefMixer(s);
StageLimiter.activate;


(
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
)


l.value('dist'); // envelope modulated synth
l.value('chop'); // cleanchop
l.value('acoustic1'); // acoustic 11
l.value('acoustic2'); // acoustic 5
l.value('acoustic3'); // acoustic 9


(
Ndef(\router).clear;
~routerIn.free;
~routerIn = Bus.audio(s, 2);
Ndef(\router, {
    arg gain = 1, inBus;

    var sig;

    sig = In.ar(inBus, 2) * gain;
    sig
});
Ndef(\router).set(\inBus, ~routerIn);
ControlSpec.add(\gain, [0, 5, \lin]);
Ndef(\conv) <<> Ndef(\router);
Ndef(\conv).play;
)

(
SynthDef(\grains, {
    arg out = 0,
        amp = 0.5,
        gain = 1,
        atk = 0.1,
        rel = 0.5,
        rateSign = 1,
        freqGrains = 5,
        grainScale = 1.0,
        delayScale = 5.0,
        cAtk = 5,
        cRel = -5,
        freq = 60.midicps, // (<=> rate = 1 <=> note = 0)
        pos = #[0, 1],
        delayDryWet = 0.2,
        delayStereo = 0.0,
        posMove = 0.0,
        lpFreq = 19000.0,
        hpFreq = 30.0,
        buf;

    var sig, env, posSig, delayedSig, delaytime, decaytime;

    posSig = posMove * LFNoise1.kr(freqGrains).range(pos[0], pos[1]);
    posSig = posSig + ((1 - posMove) * Rand(pos[0], pos[1]));

    sig = GrainBuf.ar(
        numChannels: 1,
        trigger: Impulse.kr(freqGrains), 
        dur: freqGrains.reciprocal * grainScale *
            LFNoise1.kr(freqGrains).range(0.9, 1.15),
        sndbuf: buf,
        rate: rateSign.sign * freq / 60.midicps, 
        pos: posSig,
        envbufnum: -1);

    sig = Pan2.ar(sig) * gain;

    env = EnvGen.kr(
        Env([0, 1, 0], [atk, rel], [cAtk, cRel])
    );

    sig = sig * env * amp;
    delaytime = (freqGrains.reciprocal * delayScale).clip(1.0);
    decaytime = delaytime * 5.0;

    delayedSig = sig.collect({ |monosig|
        CombC.ar(monosig,
            delaytime: Rand(0.95, 1.05) * delaytime,
            maxdelaytime: 1.05 * delaytime,
            decaytime: decaytime,
        )
    });

    delayedSig = [
        ((1 - delayStereo) * (delayedSig[0] + delayedSig[1])) + (delayStereo * (delayedSig[0] - delayedSig[1])),
        ((1 - delayStereo) * (delayedSig[0] + delayedSig[1])) - (delayStereo * (delayedSig[0] - delayedSig[1])),
    ];

    sig = ((1 - delayDryWet) * sig) + (delayDryWet * delayedSig);
    sig = LPF.ar(sig, lpFreq);
    sig = HPF.ar(sig, hpFreq);

    sig = LeakDC.ar(sig);

    // just in case the DetectSilence fails
    EnvGen.kr(Env.perc(0.01, (atk + rel + decaytime) * 1.15), doneAction: 2);
    FreeSelf.kr(DetectSilence.ar(sig + Line.kr(1.0, 0.0, delaytime)).product);

    Out.ar(out, sig);
}).add;
)

(
~createBufData = {
    arg symbol, probability, button, pbind;

    ~buffers.put(symbol, Dictionary.newFrom(List[
        \symbol, symbol,
        \prob, probability,
        \probButton, button,
        \pattern, Pdefn(symbol, Pchain(
            Pbind(\buf, e.buffers[symbol].bufnum),
            pbind
        )),
        \buffer, e.buffers[symbol],
    ]));
    ~patterns.put(symbol, Pdefn(symbol));
};
)

(
~buffers = ();
~patterns = ();
)

(
~createBufData.value('dist', 5, m.elAt('kn', '3', '5'),
    Pbind(\rate, 5)
);
~createBufData.value('chop', 2, m.elAt('kn', '2', '5'),
    Pbind(\rate, 1)
);
~createBufData.value('acoustic1', 2, m.elAt('kn', '3', '6'),
    Pbind(\rate, 1)
);
~createBufData.value('acoustic2', 2, m.elAt('kn', '2', '6'),
    Pbind(\rate, 1)
);
~createBufData.value('acoustic3', 2, m.elAt('kn', '1', '6'),
    Pbind(\rate, 1)
);
)


(
~buttonSpec = ControlSpec.new(0, 1, \lin);
// initialize midi
~buffers.keysValuesDo({ |symbol, b|
    b['probButton'].action_({ |el|
        var value = ~buttonSpec.map(el.value);
        b['prob'] = value;
        (symbol ++ ": " ++ value).postln;
    });
});
)

// TODO: define default Pbind and use Pbindf to combine

(
Pdefn(\common, Pbind(
    \instrument, \grains,
    \dur, Pwhite(0.5, 1.5, inf),
    \out, ~routerIn,
    \atk, Pexprand(0.1, 0.5, inf),
    \rel, Pwhite(7, 10, inf),
    \pos, #[0.2, 0.7],
    \amp, Pwhite(0.7, 1.0, inf),
    \root, 0,
    \octave, 4,
    \scale, Scale.minor,
    \degree, Pstutter(Prand([2, 3, 5], inf),
        Prand([0, 1], inf)),
    \posMove, Pbrown(0.02, 0.2, 0.03),
    \gain, 2.0,
    \delayStereo, 0.6,
    \delayDryWet, 0.2,
    \delayScale, 3.0,
    \freqGrains, Pbrown(2, 10, 1, inf) * 0.1,
    \hpFreq, 400,
));
)

~buffers.collect({ |d| [d['pattern'], d['prob']] }).values.asArray;
(
// TODO: put the probs in a Pdefn!
var symsAndProbs = ~buffers
.collect({ |d| [d['symbol'], d['prob']] })
.values.asArray.postln;
Pdef(\chords, Pchain(
    Psym1(
        Pwrand(
            symsAndProbs.collect({ |sypb| sypb[0] }),
            symsAndProbs.collect({ |sypb| sypb[1] }),
            inf
        ),
        ~patterns
    ),
    Pdefn(\common),
));
)

~buffers.values.collect({ |a| ;
~patterns.keys.asArray;
~buffers.values.collect({ |a| a['prob'] }).asArray;

~patterns;

Pdef(\chords).play;
Pdef(\chords).stop;

~buffers.keys.asArray;

(
Pdefn(\weights, Pfunc { ~probs.normalizeSum });
)

(
var bufsAndProbs = [
    ['dist', 'chop', 'acoustic1', 'acoustic2', 'acoustic3'],
    [5, 2, 1, 1, 1],
    /*
    [Pdefn(\dist_prob),
     Pdefn(\chop_prob),
     Pdefn(\acoustic1_prob), Pdefn(\acoustic2_prob), Pdefn(\acoustic3_prob)],
     */
];

Pdef(\chords, Pbind(
    \instrument, \grains,
    \buf, Pwrand(
        ~buffers.collect({ |symbol|
            e.buffers[symbol].bufnum
        }), Pdefn(\weights), inf),
    \dur, Pwhite(0.5, 1.5, inf),
    \out, ~routerIn,
    \atk, Pexprand(0.1, 0.5, inf),
    \rel, Pwhite(7, 10, inf),
    \pos, #[0.2, 0.7],
    \amp, Pwhite(0.7, 1.0, inf),
    \root, 0,
    \octave, 4,
    \scale, Scale.minor,
    \degree, Pstutter(Prand([2, 3, 5], inf),
        Prand([0, 1], inf)),
    \posMove, Pbrown(0.02, 0.2, 0.03),
    \gain, 2.0,
    \delayStereo, 0.6,
    \delayDryWet, 0.2,
    \delayScale, 3.0,
    \freqGrains, Pbrown(2, 10, 1, inf) * 0.1,
    \hpFreq, 400,
));
)

Pdef(\chords).play;
Pdef(\chords).stop;

s.plotTree;

(
~loadToMidi = {
    arg symbol, low, high, curve, defaultValue = low, button;

    Ndef(symbol).clear;
    Pdefn(symbol, defaultValue);
    button.action_({ |el|
        var spec = ControlSpec.new(low, high, curve);
        var value = spec.map(el.value);
        Pdefn(symbol, value);
        (symbol ++ ": " ++ value.asString).postln;
    });
    button.elemDesc.label = symbol;
};
~loadToMidiWithNdef = {
    arg symbol, low, high, curve, defaultValue = low, button;

    Ndef(symbol).clear;
    Pdefn(symbol, Ndef(symbol, { defaultValue }));
    button.action_({ |el|
        var spec = ControlSpec.new(low, high, curve);
        var value = spec.map(el.value);
        Pdefn(symbol, Ndef(symbol, { value }));
        (symbol ++ ": " ++ value.asString).postln;
    });
    button.elemDesc.label = symbol;
}
)



