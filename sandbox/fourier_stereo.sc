s.boot;

~profile = Env([0, 1], [1]).plot;

(
t.free;
a = 1 ! 2048;
a[1] = 48000;
t = Buffer.loadCollection(s, a);
)


(
Ndef(\stereo, {
    arg stereo = 1, freqLimit = 220;

    var sig, add, sub, stereosub;

    /*
    sig = [100, 440 * 3.9].collect({ |freq|
        SinOsc.ar(freq) * 0.2
    });
    */

    /*sig = SinOsc.ar([110, 115]) * 0.2;
    sig = sig + Pan2.ar(
        SinOsc.ar(440) * 0.2,
        SinOsc.kr(0.333).range(-1, 1)
    );*/
    
    sig = \in.ar([0, 0]);

    add = sig[0] + sig[1];
    sub = sig[0] - sig[1];
    sub = HPF.ar(sub, freqLimit);

    [add + (stereo * sub), add - (stereo * sub)]
});
ControlSpec.add(\stereo, [0, 1, \lin]);
ControlSpec.add(\freqLimit, [30, 18000, \exp]);
)

Ndef(\stereo).clear;
Ndef(\stereo).play;
Ndef(\stereo).stop;

Ndef(\stereo) <<> Ndef(\conv);

z = NdefMixer(s);
s.meter;



Ndef(\playin, {
    In.ar(2, 2)
});


Ndef(\playin).play;

