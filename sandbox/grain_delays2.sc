s.boot;
s.quit;

z = NdefMixer(s);
StageLimiter.activate;

l.value('chords00'); // chords_yoshimi_00.wav
l.value('chords00b'); // chords_yoshimi_00b.wav, a bit loud transients maybe */

~routerIn.free;
~routerIn = Bus.audio(s, 2);
(
Ndef(\router, {
    arg gain = 1, inBus;

    var sig;

    sig = In.ar(inBus, 2) * gain;
    sig
});
Ndef(\router).set(\inBus, ~routerIn);
ControlSpec.add(\gain, [0, 5, \lin]);
Ndef(\conv) <<> Ndef(\router);
)

(
SynthDef(\grains, {
    arg out = 0,
        amp = 0.5,
        gain = 1,
        atk = 0.1,
        rel = 0.5,
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
        rate: freq / 60.midicps, 
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

Pdefn(\chords_invdensity, 1);

(
var bufsAndProbs = [
    ['chords00', 'chords00b'],
    [5, 1]
];

Pdef(\chords, Pbind(
    \instrument, \grains,
    \buf, Pwrand(
        bufsAndProbs[0].collect({ |symbol|
            e.buffers[symbol].bufnum
        }), bufsAndProbs[1].normalizeSum, inf),
    \dur, Pwhite(0.5, 1.5, inf) * Pdefn(\chords_invdensity),
    \out, ~routerIn,
    \atk, Pexprand(0.1, 0.5, inf),
    \rel, Pwhite(7, 10, inf),
    \pos, #[0.2, 0.7],
    \amp, Pwhite(0.7, 1.0, inf),
    \root, 0,
    \octave, 4 + Pwrand([0, 1], [5, 3].normalizeSum, inf),
    \scale, Scale.minor,
    \degree, Pstutter(Prand([2, 3, 5], inf),
        Prand([0, 4], inf)),
    \posMove, Pbrown(0.02, 0.2, 0.03),
    \gain, 5.0,
    \delayStereo, 0.6,
    \delayDryWet, 0.2,
    \delayScale, 3.0,
    \freqGrains, Pbrown(2, 10, 1, inf) * Pdefn(\chords_freqGrainsMult),
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

(
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
~loadToMidi.value(\chords_invdensity,
    2.0, 0.1, \exp, 1.0,
    m.elAt('kn', '3', '1'));
~loadToMidi.value(\chords_freqGrainsMult,
    0.1, 3.0, \exp, 1.0,
    m.elAt('kn', '2', '1'));
)


