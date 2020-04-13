Ndef(\gran).end;
Ndef(\gran).play;
Ndef(\master).gui;
Ndef(\master).end;

Ndef(\gran).asCode;

Tdef(\gran).stop;

(
    Ndef(\gran).set(\rate, [0.43, 0.43]);
    Ndef(\gran).set(\centerPos, [0.97, 0.97]);
)

(
    var pos = 0.40;
    Ndef(\gran).set(\centerPos, [pos, pos]);
)

(
Ndef(\gran_pos, {
    LFNoise2.kr(0.1).range(0.42, 0.47)
});

Ndef(\gran).map(\centerPos, [Ndef(\gran_pos), Ndef(\gran_pos)]);
)

Ndef(\gran).gui;


// TODO: use Pdef instead of Tdef?
(
Tdef(\gran, {
    var centerWidth, centerPos, rate;


    loop {
        centerPos = 0.965;
        rate = e.semiTone**(-1);
        Ndef('gran').set(\rate, [rate, rate]);
        Ndef('gran').set(\centerPos, [centerPos, centerPos]);
        exprand(10, 2).wait;

        centerPos = 0.5;
        centerWidth = rrand(0.01, 0.1);

        Ndef(\gran).unmap(\centerPos);
        Ndef('gran').set(\centerPos, [max(0.0, centerPos - centerWidth), min(1.0, centerPos + centerWidth)]);
        rate = e.semiTone**(-7);
        Ndef('gran').set(\rate, [rate, rate]);

        exprand(0.1, 0.4).wait;
    };
}).play;
)

Ndef(\master).play;

(
Ndef(\master, {
    arg mix = 0.33, room = 0.5, damp = 0.5, ffreq = 440, rq = 0.7, lpfreq = 17000, hpfreq = 30,
        in1_amp = 1, in2_amp = 1, in3_amp = 1, in4_amp = 1,
        in5_amp = 1, in6_amp = 1, in7_amp = 1, in8_amp = 1;

    var master;

    // mix input
    master = Mix([
        \in1.ar([0, 0]) * in1_amp,
        \in2.ar([0, 0]) * in2_amp,
        \in3.ar([0, 0]) * in3_amp,
        \in4.ar([0, 0]) * in4_amp,
        \in5.ar([0, 0]) * in5_amp,
        \in6.ar([0, 0]) * in6_amp,
        \in7.ar([0, 0]) * in7_amp,
        \in8.ar([0, 0]) * in8_amp
    ]);

    // band reject
    master = BRF.ar(master, ffreq, rq);

    // reverb
    master = FreeVerb.ar(master, mix, room, damp);

    // low pass
    master = BLowPass.ar(master, lpfreq, 1.5);

    // high pass
    master = BHiPass.ar(master, hpfreq, 1.5);

    master
});

ControlSpec.add(\mix, [0, 1, \lin]);
ControlSpec.add(\room, [0, 1, \lin]);
ControlSpec.add(\damp, [0, 1, \lin]);
ControlSpec.add(\ffreq, [30, 18000, \exp]);
ControlSpec.add(\lpfreq, [30, 18000, \exp]);
ControlSpec.add(\hpfreq, [30, 18000, \exp]);
ControlSpec.add(\in1_amp, ControlSpec.specs[\amp]);
ControlSpec.add(\in2_amp, ControlSpec.specs[\amp]);
)

ControlSpec.specs[\amp];

(
Ndef(\ff, {
    arg lpfreq = 18000, hpfreq = 30;

    var sig;

    sig = \in.ar([0, 0]);

    // low pass
    sig = BLowPass.ar(sig, lpfreq, 1.5);

    // high pass
    sig = BHiPass.ar(sig, hpfreq, 1.5);

    sig
});
)

Ndef(\ff).copy(\granff);
Ndef(\granff) <<>.in Ndef(\gran);

Ndef(\master) <<>.in1 Ndef(\granff);
Ndef(\master) <<>.in2 Ndef(\ornament);
Ndef(\master) <<>.in3 Ndef(\bottle2);
Ndef(\master) <<>.in4 Ndef(\toms);
Ndef(\master) <<>.in5 Ndef(\piano);

Ndef(\master).gui;

Ndef(\gran).gui;
Ndef(\granff).gui;

(
Ndef(\ffreq, {
    arg width = 10, mult = 1, lofreq = 200, hifreq = 2000;
    LFNoise1.kr(
        LFNoise0.kr(2).exprange(mult / width, mult * width)
    ).exprange(lofreq, hifreq)
});
ControlSpec.add(\width, [1, 20, \exp]);
ControlSpec.add(\mult, [0.1, 100, \exp]);
ControlSpec.add(\lofreq, [200, 17000, \exp]);
ControlSpec.add(\hifreq, [200, 17000, \exp]);
)

Ndef(\ffreq).gui;

Ndef(\master).set(\ffreq, Ndef(\ffreq));
Ndef(\master).play;


Ndef(\master).gui;
Ndef(\gran).gui;
Ndef(\granff).gui;


(
Ndef(\gran).stop;
Ndef(\master) <<>.in1 Ndef(\gran);
Ndef(\master).play;
)
Ndef(\master).end;

Ndef(\master).gui;

// trying to associate MIDI with \fgran_freq
MIDIClient.init;
MIDIIn.connectAll;

e.fgranFreq = Param(Ndef(\fgran_freq), \varFreq, \varFreq.asSpec);
e.fgranRange = Param(Ndef(\fgran_freq), \freqRange, \freqRange.asSpec);

MIDIMap([e.knobs[0][0]], e.fgranFreq);
MIDIMap([e.knobs[0][1]], e.fgranRange);

// Load samples here
l.value('gran');
n.value('gran');
g.value('gran');

// use bottle with low rate
l.value('ornament');
n.value('ornament');
g.value('ornament');

// use to cdshuf with high trigger and low duration
// keep volume low, otherwise too much
l.value('bottle2');
n.value('bottle2');
g.value('bottle2');

// use timbale with low rate
l.value('toms');
n.value('toms');
g.value('toms');

l.value('piano');
n.value('piano');
g.value('piano');

StageLimiter.activate();
StageLimiter.deactivate();
