z = NdefMixer(s)


(
SynthDef(\bursts, {
    arg bufs = #[0, 0, 0, 0], amps = #[1, 1, 1, 1], amp = 0.1, out = 0, pos = 0, atk = 0.05, rel = 0.2;

    var env, sigs, outsig;

    var controlEnv = EnvGen.kr(
        Env.perc(atk, rel),
        timeScale: 2,
        doneAction: 2
    );

    sigs = (0..bufs.size-1).collect({ |i|
        var b = bufs[i], a = amps[i];

        var env = EnvGen.kr(
            Env.perc((atk + 0.001) * Rand(0.9, 1.1),
                (rel + 0.001) * Rand(0.9, 1.1)),
            doneAction: 0
        );

        var sig = PlayBuf.ar(
            numChannels: 1,
            bufnum: b,
            startPos: pos * BufFrames.kr(b)
        ).dup;

        sig * (a / bufs.size) * env
    });

    outsig = Mix.new(sigs) * amp;

    Out.ar(out, outsig);
}).add;
)

s.meter;

l.value('test');
n.value('test');
g.value('test');

e.buffers['test'].bufnum;



(
var buf = e.buffers['test'].bufnum;
var bufs = Array.fill(4, { buf });
Synth(\bursts, [
    \bufs, bufs,
    \out, e.buses['fx_in'].index,
    \amp, 1,
    \atk, 0.1,
    \rel, 0.5,
    \pos, 0,
]);
)

l.value('drum');
l.value('drum2');
l.value('drum3');

(
//var buf = e.buffers['test'].bufnum;
//var bufs = Array.fill(4, { buf });

var bufs = ['test', 'drum', 'drum2', 'drum3'].collect({|s|
    e.buffers[s].bufnum
});

var stutters = [1, 2, 4, 8].collect({|i|
    Pstutter(i, Pseq([(1/i)]))
});

Pdef(\bursts, Pbind(
    \instrument, \bursts,
    \dur, Pxrand(stutters, inf),
    \out, e.buses['fx_in'].index,
    \bufs, bufs, 
    \amp, 0.3,
    \amps, [2, 0.5, 0.5, 0.5],
    \atk, 0.1,
    \rel, Pkey(\dur) * 0.8,
    \pos, 0.2,//Pwhite(0, 0.1),
));
)

"test".postln;

Pdef(\bursts).play;
Pdef(\bursts).stop;

e.buses['fx_in'] = Bus.audio(s, 2);

(
Ndef(\fx, {
    arg in, delaymix = 0.5, delaytime = 0.2;

    var sig = In.ar(in, 2);

    var delaysig = CombC.ar(sig,
        delaytime: delaytime,
        decaytime: delaytime * 2);

    sig = ((1 - delaymix) * sig) + (delaymix * delaysig);
    
    sig
});
ControlSpec.add(\delaymix, [0, 1, \lin]);
Ndef(\fx).set(\in, e.buses['fx_in']);
)




x = [ [1, 2, 3], 6, List["foo", 'bar']];
y = x.lace(12);
x.postln;
y.postln;
(0..x.size-1)

rrand(0.9, 1.1);
