// loading percs
(
var keysPaths = [
    ["k", "/mnt/Data/prosperoh/Musique/Samples/tidalcycles/edmbd"],
    /*["cp", "/mnt/Data/prosperoh/Musique/Samples/tidalcycles/edmcp"],
    ["hh", "/mnt/Data/prosperoh/Musique/Samples/tidalcycles/edmhh"],
    ["p", "/mnt/Data/prosperoh/Musique/Samples/tidalcycles/edmperc"],
    ["sn", "/mnt/Data/prosperoh/Musique/Samples/tidalcycles/edmsn"],*/
];

keysPaths.do({ |keyPath, i|
    var key, folderPath, folder;

    key = keyPath[0].asString;
    folderPath = keyPath[1].asString;
    folderPath.postln;

    folder = PathName.new(folderPath);
    folder.entries.do({ |path, i|
        var sampleKey;

        sampleKey = (key ++ i.asString).asSymbol;

        e.buffers[sampleKey].free;
        e.buffers[sampleKey] = Buffer.read(s, path.fullPath);
        postf("Loaded % into %\n", path.fileName, sampleKey);
    });
});
)

(
SynthDef(\percbuf, {
    arg amp = 1, bufnum, out = 0, rate = 1;
    var sig;

    sig = PlayBuf.ar(2, bufnum, rate, doneAction: 2);
    sig = sig * amp * 0.5;

    Out.ar(out, sig);
}).add;
)

b = e.buffers;

(
var bpm_norm = 60.0 / 110.0;

Pdef(\corepercs, Pbind(
    \instrument, \percbuf,
    \out, 0,
    \dur, Pseq([bpm_norm], inf),
    \amp, 0.3,
    \bufnum, Pfunc({b[('k' ++ 20.rand).asSymbol].bufnum}, inf),
))
)

s.meter;

x = { SinOsc.ar([440, 442]) * 0.1 };
x.stop;

Pdef(\corepercs).play;
Pdef(\corepercs).stop;

// testing Pseg
(
SynthDef(\sine, {
    arg freq = 440, amp = 0.5, out = 0, atk = 0.01, rel = 0.3;

    var sig, env;
    env = EnvGen.kr(Env.perc(atk, rel), doneAction: Done.freeSelf);

    sig = SinOsc.ar([freq * 0.99, freq * 1.01]);
    sig = sig * amp * 0.3;

    Out.ar(out, sig);
}).add;
)

Synth(\sine);

(
Pdef(\psine, Pbind(
    \instrument, \sine,
    \out, 0,
    \dur, 0.5,
    \rel, Pkey(\dur),
    \freq, Pseq([440, 660], inf),
    //\amp, Pstep([0.1, 0.5], 0.2, inf),
    \amp, 0.25 * Pseg( Pseq([0.1, 0.5], inf), Pseq([3, 4], inf), \linear),
));
)

Pdef(\psine).play;
Pdef(\psine).stop;

