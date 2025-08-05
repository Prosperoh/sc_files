{
}

s.boot;

StageLimiter.activate;

(
Ndef(\test, {
    arg note = 2;

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
    basenote = octave * 12 + note;
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

        sig = RLPF.ar(preSig,
            freq: freq * SinOsc.kr(0.25).range(2.8, 4.0),
            rq: SinOsc.kr(0.6).range(0.01, 0.1));
        sig = sig + (2 * SinOsc.kr(0.3).range(0.05, 0.2) * RHPF.ar(preSig, freq * 8.0, rq: 0.8));
        sig = sig * 1.0;
        sig * 1.0
    }));

    rq = 1.0;

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

    sig = 0.3 * sig + CombC.ar(sig,
        maxdelaytime: 60 / (bpm * (2/3)),
        delaytime: 60 / (bpm * (6/3)),
        decaytime: 1.5,
    );

    //sig = (0.5 * sig) + Fresupercollider synchronize impulse between synthdefseVerb.ar(sig);
    //sig = sig + 0.05 * HPF.ar(GVerb.ar(sig), 500);

	sig = Latch.ar(sig, Impulse.ar(48000 / LFNoise0.kr(freqImpulse).exprange(1, 4)));

    pan = LFNoise1.kr(10.0).range(-1, 1);
    stereoCutoff = 220;
    sig = LPF.ar(sig, stereoCutoff).dup
	    + FreeVerb.ar(Pan2.ar(HPF.ar(sig, stereoCutoff), pan), room: 0.2,
            damp: 0.1);

	sig = BBandStop.ar(sig, basenote.midicps, bw: 0.2);

    HPF.ar(sig, basenote.midicps)
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

(
Ndef(\a, {
    var sig, freq;

    //freq = MouseX.kr().exprange(50, 8000);
    freq = 180;

    sig = 4.collect({
        var sig, env, envTrig, modratio, modindex;

        //modratio = LFNoise1.kr(0.8).exprange(1, 1) * 1000;
        //modratio = LFNoise1.kr(0.8).exprange(998, 1002);
        modratio = 1000;
        //modindex = LFNoise2.kr(0.8).exprange(10, 500);
        modindex = LFNoise2.kr(0.8).exprange(400, 500);

        envTrig = Dust.ar(4.0);
        //envTrig = Impulse.ar(1.0);
        env = EnvGen.ar(Env.perc(0.01, 1.0), gate: envTrig);

        sig = PMOsc.ar(freq, freq * modratio, modindex, 0) * env;
        sig = sig + (PinkNoise.ar() * 0.2) * env;
        //sig = BPF.ar(sig, 2500 * LFNoise1.kr(0.2).range(0.9, 0.9.reciprocal), 0.02);
        sig = BPF.ar(sig, 5000 * LFNoise1.kr(0.2).range(0.9, 0.9.reciprocal), 0.02);
        sig = HPF.ar(sig, freq * 3);
        sig
    });

    sig = Splay.ar(sig);

    sig = FreeVerb.ar(sig, mix: 0.33, room: 1.0);

    sig
});
)

Ndef(\a).play;
Ndef(\a).stop;

s.quit;

(
x.stop;
x = Synth(\test, [
    \freq, 50,
~loadToMidi.value(\posMove,
    0, 0.2, \lin, 0.1,
    m.elAt('kn', '3', '1')
);
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

(
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
)

MKtl.find();
m = MKtl(\akai, "akai-midimix");

m.elAt('sl', '1');

(
~loadToMidiWidescthNdef.value(\posMove,
    0, 0.2, \lin, 0.1,
    m.elAt('kn', '3', '1')
);
)

(
Ndef(\b, {
    arg modratio = 1, modindex = 1, freq = 180;

    var sig;

    sig = 4.collect({
        var sig, env, envTrig, rq;

        //modratio = LFNoise1.kr(0.8).exprange(1, 1) * 1000;
        //modratio = LFNoise1.kr(0.8).exprange(998, 1002);
        //modindex = LFNoise2.kr(0.8).exprange(10, 500);

        rq = 0.1;

        envTrig = Dust.ar(4.0);
        //envTrig = Impulse.ar(1.0);
        env = EnvGen.ar(Env.perc(0.01, 1.0), gate: envTrig);

        sig = PMOsc.ar(freq, freq * modratio, modindex, 0) * env;
        sig = sig + (PinkNoise.ar() * 0.3) * env;
        //sig = BPF.ar(sig, 2500 * LFNoise1.kr(0.2).range(0.9, 0.9.reciprocal), 0.02);
        sig = BPF.ar(sig, 5000 * LFNoise1.kr(0.2).range(0.9, 0.9.reciprocal),
            rq);
        sig = HPF.ar(sig, freq * 2);
        sig * 2
    });

    sig = Splay.ar(sig);

    sig = FreeVerb.ar(sig, mix: 0.33, room: 1.0);

    sig
});
)

Ndef(\b).play;
Ndef(\b).stop;

(
var button = m.elAt('kn', '3', '1');
button.action_({ |el|
    var diff = 0.1;
    var baseval  = 1000;
    var modratio = ControlSpec.new(baseval - diff, baseval + diff, \lin).map(el.value);
    ('modratio: ' ++ modratio).postln;
    Ndef(\b).set(\modratio, modratio);
});
button.elemDesc.label = 'modratio';

button = m.elAt('kn', '2', '1');
button.action_({ |el|
    var modindex = ControlSpec.new(0.01, 100, \exp).map(el.value);
    ('modindex: ' ++ modindex).postln;
    Ndef(\b).set(\modindex, modindex);
});
button.elemDesc.label = 'modindex';

button = m.elAt('kn', '1', '1');
button.action_({ |el|
    var freq = ControlSpec.new(80, 280, \exp).map(el.value);
    ('freq: ' ++ freq).postln;
    Ndef(\b).set(\freq, freq);
});
button.elemDesc.label = 'freq';
)


(
var button = m.elAt('kn', '3', '1');
button.action_({ |el|
    var note = (12 * el.value).floor;
    note.postln;
    Ndef(\test).set(\note, note);
});
button.elemDesc.label = 'chord dub';
)
m.gui;

Ndef(\test).stop;

NdefMixer(s);

MKtlGUI;

Ndef(\b).play;
Ndef(\b).stop;

MKtl;

m.gui;


(
SynthDef(\c, {
	arg out = 0, amp = 0, freq = 440, duration = 1;

	var sig, scale;

	sig = Mix.new((1..16).collect({
		arg n;

		var sig;

		sig = SinOsc.ar(freq * n);
		sig = sig / n;

		sig
	}));

	// distortion
	sig = (sig * Rand(0.3, 2) + Rand(0, 1)).fold2;

	// crush
	sig = Latch.ar(sig, Impulse.ar(s.sampleRate / Rand(1, 10)));

	scale = 2 ** Rand(3, 8);
	sig = (sig * scale).round / scale;

	// env
	sig = sig * EnvGen.kr(Env.perc(duration / Rand(1, 8), duration), doneAction: Done.freeSelf);

	sig = Pan2.ar(sig);
	sig = sig * ((-10 + amp).dbamp);
	sig = LeakDC.ar(sig);

	Out.ar(out, sig);
}).add;
)

Synth(\c);

(
Pdef(\pc, Pbind(
	\instrument, \c,
	\dur, Pexprand(0.01, 1),
	\duration, Pkey(\dur) * 1.5,
	\amp, 0,
	\octave, 4,
	\note,
));
)

Pdef(\pc).play;
Pdef(\pc).stop;



