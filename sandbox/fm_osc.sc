// With Ndef

//////////////////////////////////////////////////////////////
// Foreplay
//////////////////////////////////////////////////////////////
StageLimiter.activate;
t = 85 / 60;

//////////////////////////////////////////////////////////////
// Midi detect AKAI Midimix
//////////////////////////////////////////////////////////////
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
m.gui;

//////////////////////////////////////////////////////////////
// Buses & groups
//////////////////////////////////////////////////////////////
(
(1..8).do({ |i|
    e.buses[('in' ++ i).asSymbol] = Bus.audio(s, 2);
});
e.groups['synths'] = Group.new(s, \addToHead);
e.groups['effects'] = Group.new(e.groups['synths'], \addAfter);
e.groups['master'] = Group.new(s, \addToTail);
)

//////////////////////////////////////////////////////////////
// Master
//////////////////////////////////////////////////////////////
(
Ndef(\master, {
    arg in1, in2, in3, in4, in5, in6, in7, in8,
        amp1 = 1, amp2 = 1, amp3 = 1, amp4 = 1, amp5 = 1, amp6 = 1, amp7 = 1, amp8 = 1;
    var bufnums, amps, ins, sig;

    bufnums = [in1, in2, in3, in4, in5, in6, in7, in8];
    amps = [amp1, amp2, amp3, amp4, amp5, amp6, amp7, amp8];
    ins = (0..7).collect({ |i| In.ar(bufnums[i], 2) * amps[i] });

    sig = Mix.ar(ins);
    sig
});

(1..8).do({ |i|
    var in, amp;
    in = (\in ++ i).asSymbol;
    amp = (\amp ++ i).asSymbol;

    Ndef(\master).set(in, e.buses[in]);

    ControlSpec.add(amp, [0, 2, \lin]);

    m.elAt(\sl, i - 1).action_({ |el|
        var value;
        value = amp.asSpec.map(el.value);
        Ndef(\master).set(amp, value);
    });
});
)

Ndef(\master).play(group: e.groups['master']);
Ndef(\master).stop;
Ndef(\master).gui;


//////////////////////////////////////////////////////////////
// Compander (side-chain)
//////////////////////////////////////////////////////////////

// Setup (bus)
e.buses['compander'] = Bus.audio(s, 2);

(
Ndef(\compander, {
    arg tshold = 0.5, controlBus, ratio = 1.0, relaxTime = 0.3;
    Compander.ar(In.ar(e.buses['compander'], 2),
        control: In.ar(controlBus.round, 2),
        thresh: tshold,
        slopeBelow: 1,
        slopeAbove: 1 / ratio,
        clampTime: 0.01,
        relaxTime: relaxTime)
});
ControlSpec.add(\tshold, [0.001, 1, \exp]);
ControlSpec.add(\ratio, [1, 100, \exp]);
ControlSpec.add(\relaxTime, [0.01, 2, \exp]);

Ndef(\compander).set(\controlBus, e.buses['in1']);
)

Ndef(\compander).play(e.buses['in2'], 2, e.groups['effects']);
Ndef(\compander).stop;

Ndef(\compander).gui;

//////////////////////////////////////////////////////////////
// FM Ndef & \set pattern
//////////////////////////////////////////////////////////////
(
Ndef(\fmlong)[0] = {
	arg freq = 500, mRatio = 1, cRatio = 1, index = 1,
	amp = 0.2, pan = 0, phase = 0, spread = 1, freqRatio = 1.01,
    hiShelfFreq = 2000, hiShelf = 1;

	var sig, car1, car2, mod, modAmp;

	modAmp = freq * mRatio; // normalized amplitude, use index to change amplitude
	mod = SinOsc.ar(freq * mRatio, mul: freq * mRatio * index);

	car1 = SinOsc.ar(freq * cRatio + mod, phase) * amp * 0.2;
	car2 = SinOsc.ar(freq * cRatio * freqRatio + mod, phase) * amp * 0.2;

	sig = Splay.ar([car1, car2], center: pan, spread: spread);
    sig = BHiShelf.ar(sig, hiShelfFreq, 1 / hiShelf, -10);
    sig
};
ControlSpec.add(\hiShelfFreq, ControlSpec.specs[\freq]);
ControlSpec.add(\hiShelf, [1, 20, \exp]);

// Control rate proxies for pattern
~fmlong_spread = { LFNoise1.kr(3).range(0, 1) };
~fmlong_index = {
    arg width = 10, center = 10, freqVar = 0.5;
    LFNoise2.kr(
        LFNoise0.kr(freqVar).exprange(0.1, 10)
    ).range(center / width, center * width)
};
ControlSpec.add(\freqVar, [0.1, 5, \exp]);
ControlSpec.add(\width, [1, 100, \exp]);
ControlSpec.add(\center, [0.01, 100, \exp]);

~fmlong_mRatio = {
    arg mRatio = 20;
    mRatio
};

ControlSpec.add(\mRatio, [0.1, 50, \exp]);

// Midi setup for control rate proxies
m.elAt(\kn, 0, 0).action_({ |el| ~fmlong_index.set(\width, \width.asSpec.map(el.value)); });
m.elAt(\kn, 1, 0).action_({ |el| ~fmlong_index.set(\center, \center.asSpec.map(el.value)); });
m.elAt(\kn, 2, 0).action_({ |el| ~fmlong_index.set(\freqVar, \freqVar.asSpec.map(el.value)); });
m.elAt(\kn, 2, 1).action_({ |el|
    var floatVal, roundedVal;
    floatVal = \mRatio.asSpec.map(el.value);
    if (floatVal <= 1, 
        { roundedVal = 1 / (1 / floatVal).round; },
        { roundedVal = floatVal.round; }
    );
    roundedVal.postln;
    ~fmlong_mRatio.set(\mRatio, roundedVal);
});

// Set pattern
Pdef(\fmlong, Pbind(
    \dur, 1/16,
    \stretch, t,
    \note, -24,
    \index, ~fmlong_index,//Pkr(Ndef(\index).index),
    /*0.1 * Pseq([
        300 * Pstep(Pwhite(0.7, 1.3, 1), 0.1 / t, 10),
    ], inf),*/
    \spread, ~fmlong_spread,
    \pan, 0.2 * Pseq([
        Pseg([-1, 1], 10 / t),
        Pseg([1, -1], 10 / t)
    ], inf),
    \freqRatio, Pstutter(8,
        Pwhite([0.97, 1.03])
    ),
    \cRatio, 1,// Pstutter(unit / 0.2, Pwhite(1, 10)),
    \mRatio, ~fmlong_mRatio,/*10 * Pstutter(
        unit / Pseq([2, 8, 8, 2], inf),
        Prand([10, 20, 7], inf)
    ),*/
    \amp, Pbrown(0.2, 1, 0.1) * 0.3,
)).quant_(t);

Ndef(\fmlong)[1] = \set -> Pdef(\fmlong);

Ndef(\fmlong).play(e.buses['compander'].index, 2, group: e.groups['synths']);
)

Ndef(\fmlong).clear;
Ndef(\fmlong).gui;
Ndef(\fmlong).stop;

s.freqscope;

~fmlong_index.gui;
~fmlong_mRatio.gui;

Pdef(\fmlong).clear;


//////////////////////////////////////////////////////////////
// Playbuf (kick & other samples)
//////////////////////////////////////////////////////////////

// Synthdef
(
SynthDef(\playbuf, {
    arg out = 0, bufnum, pan = 0, rate = 1, amp = 1;
    var sig;
    sig = PlayBuf.ar(1, bufnum, rate, doneAction: 2);
    sig = sig * amp * 0.5;
    sig = Pan2.ar(sig, pan);
    Out.ar(out, sig);
}).add;
)

// Loading kick sample
l.value('kick');

e.buffers['kick'].free;

// Kick pattern
(
Pdef(\kick, Pbind(
    \instrument, \playbuf,
    \group, e.groups['synths'],
    \out, e.buses['in1'].index,
    \dur, 1 / Prand(
        [1, 2, 4, 8],
        inf),
    \stretch, t,
    \bufnum, e.buffers['kick'],
    \rate, e.semiTone ** (
        Pwhite(-5, 0, inf).round +
        Pwrand(
            [0, 12, 24], 
            [12, 3, 1].normalizeSum,
            inf)
        ),
    \amp, Pwhite(0.5, 1, inf) * Pwrand(
        [1, 0],
        [5, 1].normalizeSum,
        inf) /
        Pkey(\rate),
)).quant_(t);
)
Pdef(\kick).play;


//////////////////////////////////////////////////////////////
// Why In.ar does not work with Ndef: ORDER OF EXECUTION
// Put outputs of Ndefs containing the synths in a group that
// will always be at the beginning
//////////////////////////////////////////////////////////////

Ndef(\fmlong).gui;

Ndef(\master).stop;

s.freqscope;









// Test zone

(
SynthDef(\test, {
    arg amp = 1, freq = 440, out = 0;
    var sig;
    sig = SinOsc.ar([freq, freq * 1.01]) * 0.1 * amp;
    sig = sig * EnvGen.kr(Env.perc(), doneAction: 2);

    Out.ar(out, sig);
}).add;
)

(
Pdef(\test, Pbind(
    \instrument, \test,
    \dur, 1,
));
)

Pdef(\test).stop;
(
Ndef(\testIn, {
    \in.ar(0!2) * 0.1
});
)

Ndef(\testIn).clear;

Ndef(\testIn) <<>.in Ndef(\testPattern, Pdef(\test));
Ndef(\testIn).play;
Ndef(\testIn) <<> Ndef(\test);
Ndef(\testIn).gui;

Ndef(\test, { SinOsc.ar([440, 441]) * 0.05 });
Ndef(\test).play();
Ndef(\test).stop;
Ndef(\master).set(\in2, Ndef(\test));

Ndef(\master).gui;

s.plotTree;




