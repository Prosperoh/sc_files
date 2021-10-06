(
Ndef(\freq, { SinOsc.kr(1).range(200, 500) });

Ndef(\test, {
    arg amp = 1, freq = 200;
    SinOsc.ar([freq, freq * 1.01]) * amp * 0.1 
});

Ndef(\test)[1] = \set -> Pbind(
    \dur, 0.7,
    \freq, Pseq([200, Ndef(\freq)], inf),
    \amp, ~testAmp,
);
Ndef(\test).play;
)
Ndef(\test).stop;
