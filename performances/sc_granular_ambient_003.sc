Ndef(\synth).clear;

// sample: synths -> minor staccato
// rate: 7 sts, also -5 for calmer, other interesting values
l.value('synth');
n.value('synth');
g.value('synth');

// make some automation on the parameters, use patterns

// not too inspired by the guitar
l.value('other');
n.value('other');
g.value('other');

// use percussion sounds for transitions between chords
// the base sample is quite active, we may use a lot of
// random sound effects

(
var rates, rateSeq, weights, stdRate;

stdRate = e.semiTone ** (-5);
stdRate = [[stdRate, stdRate]];

weights = [10, 2].normalizeSum;
rates = [-7, 7].collect({|s, i| e.semiTone ** s});
rateSeq = rates.collect({|r, i| [[r, r]]});

Ndef(\synth)[1] = \set -> Pbind(
    \dur, Pseq([Pwhite(10, 15), Pwhite([3, 5])], inf),
    \args, #[rate],
    \rate, Pseq([stdRate, Prand(rateSeq, weights, inf)], inf)
);
)

Ndef(\synth).clear;

(
Ndef(\synthrev, {
    arg wetDry;
    var n, a, in;

    in = \in.ar([0, 0]);
    a = in;

    n = 8;
    n.collect({ |i|
        a = AllpassC.ar(a,
            delaytime: LFNoise1.kr(0.03!2).range(0.01, 0.02 * (i + 1)),
            decaytime: (n - i),
            //decaytime: n - i,
            //decaytime: (n / 2 - i).abs + 0.05
        ).tanh
    }).mean;

    Mix([
        in * (1 - wetDry),
        a * wetDry
    ]);
});
ControlSpec.add(\wetDry, [0, 1], \lin);
)

Ndef(\synthrev).gui;

//Ndef(\reverb).copy(\synthrev);
Ndef(\synthrev) <<>.in Ndef(\synth);
Ndef(\syntheq) <<>.in Ndef(\synthrev);

Ndef(\eq).copy(\syntheq)
Ndef(\eq).copy(\othereq)
Ndef(\syntheq) <<>.in Ndef(\synth);
Ndef(\othereq) <<>.in Ndef(\other);

Ndef(\master) <<>.in1 Ndef(\syntheq);
Ndef(\master) <<>.in2 Ndef(\othereq);

Ndef(\syntheq).gui;
Ndef(\master).gui;

s.meter;

Ndef(\noise, { Pan2.ar(PinkNoise.ar(0.3)) });
Ndef(\noise).gui;
Ndef(\noise).clear;

