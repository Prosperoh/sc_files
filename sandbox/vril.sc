{
}

s.boot;

(
Ndef(\test, {
    var sig, trig, env, envSig, pan, chord, basenote, octave, rq, stereoCutoff, bpm, freqImpulse;

    bpm = 140;
    freqImpulse = bpm / 60 / 4;

    // regular impulse cycle
    trig = Impulse.ar(freqImpulse)
        + Impulse.ar(freqImpulse, phase: 0.875)
        + Impulse.ar(freqImpulse, phase: 0.425) 
        + Dust.ar(freqImpulse * 0.5);
    /*
    trig = Impulse.ar(freqImpulse)
        + Impulse.ar(freqImpulse, phase: (2/6))
        + Impulse.ar(freqImpulse, phase: (5/6)) 
        + Dust.ar(freqImpulse * 0.01);
        */

    envSig = LFNoise0.kr(5.0);
    
    env = Env.perc(
        attackTime: envSig.exprange(0.01, 0.8) * freqImpulse.reciprocal / 4,
        releaseTime: envSig.exprange(0.8, 0.01) * freqImpulse.reciprocal / 0.5,
    );

    octave = 3;
    basenote = octave * 12 + 2;
    //chord = [0, 3, 7, 11];
    //chord = [0, 4 + 24, 9 + 12, 11];
    chord = [0, 3, 7];

    sig = Mix.new(chord.collect({
        arg noteChord;
        
        var note, amp, preSig, sig, freq, ampRatio;
        note = basenote + noteChord;
        freq = note.midicps;
        freq.postln;

        ampRatio = 0;

        amp = 1;
        ampRatio = (1 + noteChord).pow(ampRatio);
        ampRatio.postln;

        preSig = SawDPW.ar(freq) * amp / ampRatio;

        //sig = RLPF.ar(preSig, freq * SinOsc.kr(0.6).range(2.75, 3.5),
        //    rq: SinOsc.kr(0.6).range(0.02, 0.15));
        
        sig = RLPF.ar(preSig, freq * 3.5,
            rq: SinOsc.kr(0.6).range(0.01, 0.1));
        sig = sig + (SinOsc.kr(0.3).range(0.05, 0.2) * RHPF.ar(preSig, freq * 8.0, rq: 0.8));
        sig = sig * 1.0;
        sig
    }));

    rq = 0.7;

    sig = BPF.ar(sig,
        //freq: LFNoise1.kr(freqImpulse * 8).exprange(20, 550),
        freq: SinOsc.kr(freqImpulse * (7/3)).exprange(20, 550),
        rq: rq);
    sig = sig * EnvGen.ar(env, gate: trig);


    // This has a weird effect on the pitch
    /*
    sig = CombC.ar(sig, maxdelaytime: 0.5,
        delaytime: LFNoise1.kr(3.0).range(0.2, 0.3),
        decaytime: 1.0,
    );
    */

    sig = 0.2 * sig + CombC.ar(sig,
        maxdelaytime: 60 / (bpm * (2/3)),
        delaytime: 60 / (bpm * (6/3)),
        decaytime: 1.5,
    );

    //sig = (0.5 * sig) + FreeVerb.ar(sig);
    //sig = sig + 0.05 * HPF.ar(GVerb.ar(sig), 500);

    sig = FreeVerb.ar(sig, room: 0.2);

    pan = LFNoise1.kr(10.0).range(-1, 1);
    stereoCutoff = 220;
    sig = LPF.ar(sig, stereoCutoff).dup
        + FreeVerb.ar(Pan2.ar(HPF.ar(sig, stereoCutoff), pan), room: 0.2,
            damp: 0.9);

    HPF.ar(sig, basenote.midicps * 1.8)
});
)

(
Ndef(\test, {
    SinOsc.ar().dup
})
)

z = NdefMixer(s);

Ndef(\test).play;
Ndef(\test).stop;

s.quit;

(
x.stop;
x = Synth(\test, [
    \freq, 50,
    \amp, 1,
]);
)

(
Pdef(\p, Pbind(
    \instrument, \test,
    \dur, 1,
    \freq, 
));
)

Pdef(\p).stop;


StageLimiter.activate;
