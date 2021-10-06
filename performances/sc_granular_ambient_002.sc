
StageLimiter.activate();
StageLimiter.deactivate();


Ndef(\master).gui;
Ndef(\master).end;

s.meter;

///////////////////////////////////////////////////
// Instruments
///////////////////////////////////////////////////

// use pianofx/piano roll ambiance
// eq: remove high resonance at 1500 + low resonance at 350
// lower rates (< 1)
l.value('pgran');
m.value('pgran');
g.value('pgran');

// use noise/Zoom0057.wav
// eq: high pass 1500 Hz & freq boost at 1050 Hz
// centerPos: interesting sound at 0.43, make it 0.43 - 0.55 in order
// to make it appear not too frequently
// medium-short duration to avoid too much noise
// rate 2.5, maybe width > 0 to avoid defined pitch
l.value('factory');
n.value('factory');
g.value('factory');

l.value('lowconga');
n.value('lowconga');
g.value('lowconga');

(
Ndef(\pgran_amp, {
    arg amp = 1;
    
    \in.ar([0, 0]) * amp;
});
)

(
Tdef(\pgran_pos, {
    var calmPos, melancholicPos, alternativePos, posSequence, shiftProb, baseShift = -5;

    calmPos = [
        [0.46, 0.47],
        [0.46, 0.54],
        [0.51, 0.6],
        [0.61, 0.71],
        [0.7, 0.71],
        [0.77, 0.83],
        [0.97, 0.98]
    ];

    melancholicPos = [
        [0.05, 0.17],
        [0.16, 0.29],
        [0.3, 0.31]
    ];

    alternativePos = [
        [0.11, 0.12]
        [0.33, 0.34],
        [0.8, 0.85],
        [0.87, 0.88]
    ];

    posSequence = [
        [melancholicPos, { exprand(20, 10) }, 'melancholic'],
        [alternativePos, { exprand(20, 10) }, 'alternative'],
        [calmPos, { exprand(40, 20) }, 'calm']
    ];
    
    baseShift = -8;
    shiftProb = 1;

    loop {
        posSequence.do({ |posData|
            var pos, delta, posArray, deltaFunc, posName, randTry, shift, val, times;

            posArray = posData[0];
            deltaFunc = posData[1];
            posName = posData[2];
            posName.postln;

            pos = posArray.choose();
            format("position: %", pos).postln;
            Ndef(\pgran).set(\centerPos, pos);

            randTry = 1.0.rand;
            if ( randTry <= shiftProb, {
                'Shifting pitch'.postln;
                shift = [0].choose + baseShift;
                format("semi tones: %", shift).postln;
                val = e.semiTone ** shift;
                Ndef(\pgran).set(\rate, [val, val]);
            });

            delta = deltaFunc.value();
            format("next in % seconds", delta).postln;

            times = [5, 10, 5].normalizeSum;

            Ndef(\pgran_amp).map(\amp, Ndef(\pgran_amp_kr, { 
                EnvGen.kr(
                    Env.linen(
                        attackTime: delta * times[0],
                        sustainTime: delta * times[1],
                        releaseTime: delta * times[2],
                        curve: -2
                    ).range(0.6, 1)
                )
            }));

            delta.wait;
        });
    }
}).play;
)

(
var delta = 15;
Env.linen(
    attackTime: delta * 0.1,
    sustainTime: delta * 0.75,
    releaseTime: delta * 0.15,
    curve: -2
).range(0.5, 1).plot;
)

Tdef(\pgran_pos).clear;
Tdef(\pgran_pos).stop;
Ndef(\pgran).clear;
Ndef(\pgran).clear;

(
Ndef(\pgran_pan, {
    arg amp = 1, ampFreq = 1;
    var sig;
    sig = SinOsc.kr(ampFreq).range(0, amp);
    [sig * (-1), sig]
});
ControlSpec.add(\ampFreq, [0.05, 2, \exp]);
)

Ndef(\pgran_pan).gui;

Ndef(\pgran).map(\pan, Ndef(\pgran_pan));

(
Ndef(\pgran_bpf, {
    arg ffreq_min = 440, bw = 1, ffreq_max = 880, ffreq_freq = 0.5;
    var sig;

    sig = \in.ar([0, 0]);
    sig = BBandStop.ar(
        sig,
        LFNoise1.kr(ffreq_freq).range(ffreq_min, ffreq_max),
        bw);
    sig
});
ControlSpec.add(\ffreq_min, [30, 18000, \exp]);
ControlSpec.add(\ffreq_max, [30, 18000, \exp]);
ControlSpec.add(\bw, [1.0 / 12, 8, \exp]);
ControlSpec.add(\ffreq_freq, [0.1, 10, \exp]);
)


Ndef(\eq).copy(\pgraneq);
Ndef(\eq).copy(\factoryeq);
Ndef(\eq).copy(\lowcongaeq);


Ndef(\pgran_amp) <<>.in Ndef(\pgran);
Ndef(\pgraneq) <<>.in Ndef(\pgran_amp);
Ndef(\pgran_bpf) <<>.in Ndef(\pgraneq);
Ndef(\factoryeq) <<>.in Ndef(\factory);
Ndef(\lowcongaeq) <<>.in Ndef(\lowconga);

Ndef(\master) <<>.in1 Ndef(\pgran_bpf);
Ndef(\master) <<>.in2 Ndef(\factoryeq);
Ndef(\master) <<>.in3 Ndef(\lowcongaeq);

Ndef(\pgran_bpf).gui;
Ndef(\pgraneq).gui;
Ndef(\factoryeq).gui;
Ndef(\lowcongaeq).gui;
Ndef(\master).gui;

// trying to associate MIDI with \fgran_freq
MIDIClient.init;
MIDIIn.connectAll;

e.fgranFreq = Param(Ndef(\fgran_freq), \varFreq, \varFreq.asSpec);
e.fgranRange = Param(Ndef(\fgran_freq), \freqRange, \freqRange.asSpec);

MIDIMap([e.knobs[0][0]], e.fgranFreq);
MIDIMap([e.knobs[0][1]], e.fgranRange);

s.meter;

