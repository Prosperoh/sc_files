s.boot;
s.quit;


(
SynthDef(\sine, {
    arg out = 0, freq = 440, amp = 0.3, rel = 1.0;

    var sig, env;

    sig = SinOsc.ar(freq).dup;
    env = EnvGen.kr(Env.perc(releaseTime: rel), doneAction: 2);
    
    Out.ar(out, sig * env * amp);
}).add;
)


Synth(\sine);


(
Pdefn(\sineCommon, Pbind(
    \instrument, \sine,
    \out, 0,
    \rel, 0.5,
    \dur, 0.5,
));
)


(
Pdefn(\s, Pbind(
    \freq, 440,
    \amp, 0.1,
    \dur, 1.0,
));
Pdefn(\ss, Pbind(
    \freq, #[880, 440],
    \amp, 0.5,
));
)
(
~dict = ();
[\s, \ss].do({ |symbol|
    ~dict.put(symbol, Pdefn(symbol));
});
)


(
Pdef(\sine, Pchain(
    Psym1(
        Prand(#[\s, \ss], inf),
        ~dict,
    ),
    Pdefn(\sineCommon)
));
)

Pdef(\sine).play;
Pdef(\sine).stop;
Pdef(\sine).clear;



Pdef(\sine).clear;
Pdef(\sineCommon).stop;
Pdef(\sine1).stop;
Pdef(\sine2).stop;

x.stop;

Pdef(\sine).play;
Pdef(\sine).stop;




