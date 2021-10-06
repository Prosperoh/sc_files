s.boot;
s.quit;

z = NdefMixer(s);

l.value('buf'); // pianofx -> pianomood_128

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

(
Synth(\grains, [
    \buf, e.buffers['buf'].bufnum,
    \atk, 2.0,
    \rel, 3.0,
]);
)


Scale.lydian;

(
Pdefn(\amp_high, -5);
Pdefn(\rel_low, 2.0);
Pdefn(\rel_high, 8.0);
Pdefn(\posMove, 0.05); // 0 gives a "stillness" effect, change with midi
)

(
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
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
)

(
var relLow = 2.0, relHigh = 8.0;
~loadToMidi.value(\rel_low,
    relLow, relHigh, \lin, relLow,
    m.elAt('kn', '2', '1')
);
~loadToMidi.value(\rel_high,
    relLow, relHigh, \lin, relHigh,
    m.elAt('kn', '1', '1')
);
~loadToMidi.value(\posMove,
    0, 0.2, \lin, 0.1,
    m.elAt('kn', '3', '1')
);
~loadToMidi.value(\grains_amp,
    0, 1, \lin, 1,
    m.elAt('sl', '1')
);
)

s.meter;

m.elAt('sl', '1');

Pdef(\grains).clear;

-60.dbamp;
-20.dbamp;
-5.dbamp;

(
Pdef(\grains, Pbind(
    \instrument, \grains,
    \out, ~routerIn,
    \dur, Prand([0.25, 0.5, 1.0, 1.5], inf),
    \amp, Pexprand(-20, Pdefn(\amp_high), inf).dbamp * Pdefn(\grains_amp),
    \buf, e.buffers['buf'].bufnum,
    \scale, Scale.mixolydian,
    \octave, 5,
    \gain, 5.0,
    \atk, 0.001 * Pkey(\amp).reciprocal,
    \rel, Pkey(\dur) * Pwhite(Pdefn(\rel_low), Pdefn(\rel_high), inf),
    \delayScale, 5.0,
    \delayDryWet, 0.0,
    \delayStereo, 1.0,
    \degree, Pstutter(
        Pseq([5, 10], inf),  // repeats 
        Pseq([0, Pwrand([7, 5 /* start with 7, change to 5 */], [3, 1].normalizeSum), -7], inf)),
    \grainScale, Pexprand(5.0, 2.0, inf),// * Pwrand([1, 0.5, 0.1], [10, 2, 1].normalizeSum),
    \pos, #[0.2, 0.2],
    \posMove, Pdefn(\posMove),
    \freqGrains, Prand([2.5, 5.0, 10.0], inf),
));
ControlSpec.add(\delayDryWet, [0, 1, \lin]);
ControlSpec.add(\posMove, [0, 1, \lin]);
ControlSpec.add(\freqGrains, [1, 50, \exp]);
)

s.plotTree;




s.quit;
s.boot;

Pdef(\grains).play;
Pdef(\grains).stop;

Pdef.all.do({ |p| p.play });
Pdef.all.do({ |p| p.stop });



// another element, noise

l.value('noise');


Ndef(\noiseRouter).clear;
(
Ndef(\router).copy(\noiseRouter);
~noiseIn.free;
~noiseIn = Bus.audio(s, 2);
)
(
Ndef(\noiseRouter).set(\inBus, ~noiseIn);
Ndef(\noiseRouter).play(~routerIn,
    addAction: \addToHead,
);
)
(
m.elAt('sl', '3').action_({ |el|
    var spec = ControlSpec.specs['gain'];
    var value = spec.map(el.value);
    Ndef(\noiseRouter).set(\gain, value);
    ("noiseGain: " ++ value.asString).postln;
});
)

s.plotTree;


(
Pdef(\noise, Pbind(
    \instrument, \grains,
    \out, ~noiseIn,
    \dur, Pseq([10.0], inf),
    \amp, Pexprand(0.4, 0.6, inf),
    \buf, e.buffers['noise'].bufnum,
    \gain, 2.0,
    \note, Pbrown(0, 24, 2, inf),
    \atk, Pkey(\dur) * 0.8,
    \rel, Pkey(\dur) * 2.0,
    \delayScale, 3.0,
    \delayDryWet, 0.5,
    \delayStereo, Pbrown(0.1, 0.9, 0.1, inf),
    \freqGrains, 20.0 * Pwhite(0.9, 1.2, inf),//Prand([2.5, 5.0, 10.0], inf),
    \grainScale, 1.0,
    \pos, #[0.2, 0.8],
    \posMove, 0.05,
    \hpFreq, Pbrown(1000, 2000, 200),
));
)




Pdef(\noise).play;
Pdef(\noise).stop;

s.meter;



s.plotTree;
~gui = m.gui;
m;

~gui.labelView.items;
~gui.labelView.background_(Color.black.alpha_(0.8));



(
Ndef(\stereo, {
    arg stereo = 1, freqLimit = 220;

    var sig, add, sub, stereosub;
    sig = \in.ar([0, 0]);

    add = sig[0] + sig[1];
    sub = sig[0] - sig[1];
    sub = HPF.ar(sub, freqLimit);

    [add + (stereo * sub), add - (stereo * sub)]
});
ControlSpec.add(\stereo, [0, 1, \lin]);
ControlSpec.add(\freqLimit, [30, 18000, \exp]);
)

(
Ndef(\stereo) <<> Ndef(\conv);
)

Ndef(\stereo).gui;
NdefMixer(s);

