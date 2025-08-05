s.boot;


(
SynthDef(\kick_nopm, { |out = 0, freq = 50, atk = 0.01, rel = 0.5, bAtk = 1, fRel = 0.05, fHigh = 200, amp = 0.2|

	var sig, freqSig;

	freqSig = EnvGen.kr(Env.new([fHigh, freq], [fRel], curve: -2));
	sig = SinOsc.ar(freqSig, (pi/2).mod(2pi));
	sig = sig * EnvGen.kr(Env.new([bAtk, 1, 0], [atk, rel], curve: -1), doneAction: Done.freeSelf);

	sig = sig!2 * amp;
	Out.ar(out, sig);
}).add;
)



(
SynthDef(\kick_acoustic, { |out = 0, freq = 50,
	atk = 0.01, rel = 0.5, bAtk = 1,
	fRel = 0.05, fHigh = 200, fmInt = 1, fmRel = 0.01,
	farFmRel = 0.45, farAmp = 0.5, farFmInt = 10,
	amp = 0.2|

	var sig, freqSig, sig2, freqSig2;

	// first part: inside mic kick
	freqSig = EnvGen.kr(Env.new([fHigh, freq], [fRel], curve: -2));
	freqSig = freqSig + (freqSig * WhiteNoise.ar() * EnvGen.kr(Env.new([fmInt, 0], [fmRel])));
	sig = SinOsc.ar(freqSig, (pi/2).mod(2pi)); // adding a pi/2 shift to add a click at the beginning, control intensity of the click with bAtk
	sig = sig * EnvGen.kr(Env.new([bAtk, 1, 0], [atk, rel], curve: -4), doneAction: Done.freeSelf);
	sig = (sig * 1).tanh;

	// eq
	sig = BPeakEQ.ar(sig, 500, rq: 0.25, db: -10);
	sig = BHiShelf.ar(sig, 2000, db: 7);
	sig = BLowShelf.ar(sig, 100, db: 5);

	// 2nd part: faraway kick
	freqSig2 = freq + (freq * WhiteNoise.ar() * EnvGen.kr(Env.new([farFmInt, 0], [farFmRel])));
	sig2 = SinOsc.ar(freqSig2) * EnvGen.kr(Env.new([0, 1, 0], [0.001, 0.25], curve: -3));

	// eq
	sig2 = BLowShelf.ar(sig2, 50, db: -20);
	sig2 = BPeakEQ.ar(sig2, 200, db: 5);
	sig2 = BPeakEQ.ar(sig2, 800, db: -10, rq: 2.0);
	sig2 = BHiShelf.ar(sig2, 2000, db: 8);

	sig = sig + (sig2 * farAmp);
	//sig = sig2;

	sig = sig!2 * amp;
	Out.ar(out, sig);
}).add;
)

Synth(\kick);

t = TempoClock.new(138 / 60);

(
Pdef(\p_kick_nopm, Pbind(
	\instrument, \kick,
	\dur, 1,
	\midinote, 29,
	\fRel, 0.08,
	\fHigh, 200,
	\rel, 0.34,
	\atk, 0.01,
	\bAtk, 0.1,
	\amp, 0.25,
));
)

29.midicps;

(
Pdef(\p_kick, Pbind(
	\instrument, \kick,
	\dur, Pseq([1, 1, 1, 1], inf),
	\midinote, 29,
	\fRel, 0.04,
	\fHigh, 250,
	\rel, 0.1,
	\atk, 0.005,
	\bAtk, 0.1,
	\amp, 0.3,
	\fmInt, 5,
	\fmRel, 0.01,
));
)

Pdef(\p_kick).play(argClock: t);
Pdef(\p_kick).stop;

(
Synth(\kick, [

]);
)




(
SynthDef(\kick_techno, { |out = 0, freq = 45,
	atk = 0.01, rel = 0.5, bAtk = 0.25,
	fRel = 0.05, fHigh = 200, fmInt = 1, fmRel = 0.01,
	farFmRel = 0.45, farAmp = 0.5, farFmInt = 10,
	amp = 0.25|

	var sig, freqSig, sig2, freqSig2;

	// first part: inside mic kick
	freqSig = EnvGen.kr(Env.new([fHigh, freq], [fRel], curve: -2));
	freqSig = freqSig + (freqSig * WhiteNoise.ar() * EnvGen.kr(Env.new([fmInt, 0], [fmRel])));
	sig = SinOsc.ar(freqSig, (pi/2).mod(2pi)); // adding a pi/2 shift to add a click at the beginning, control intensity of the click with bAtk
	sig = sig * EnvGen.kr(Env.new([bAtk, 1, 0], [atk, rel], curve: -4), doneAction: Done.freeSelf);
	sig = (sig * 1).tanh;

	// eq
	sig = BPeakEQ.ar(sig, 500, rq: 0.25, db: -10);
	sig = BHiShelf.ar(sig, 2000, db: 7);
	sig = BLowShelf.ar(sig, 100, db: 5);

	// 2nd part: faraway kick
	freqSig2 = freq + (freq * WhiteNoise.ar() * EnvGen.kr(Env.new([farFmInt, 0], [farFmRel])));
	sig2 = SinOsc.ar(freqSig2) * EnvGen.kr(Env.new([0, 1, 0], [0, 0.2], curve: -3));

	// eq
	sig2 = BLowShelf.ar(sig2, 50, db: -20);
	sig2 = BPeakEQ.ar(sig2, 300, db: 5);
	sig2 = BPeakEQ.ar(sig2, 800, db: -10, rq: 1.0);
	sig2 = BHiShelf.ar(sig2, 2000, db: -2);

	sig = sig + (sig2 * farAmp);
	//sig = sig2;

	sig = sig!2 * amp;
	Out.ar(out, sig);
}).add;
)


(
Pdef(\p_kick_techno, Pbind(
	\instrument, \kick_techno,
	\dur, Pseq([1, 1, 1, 1], inf),
	\midinote, 29,
	\farAmp, 0.5,
));
)

Pdef(\p_kick_techno).play(argClock: t);
Pdef(\p_kick_techno).stop;


(
m = MKtl(\akai, "akai-midimix");
n = MKtl(\samson, "midi_0_samson_graphite_m25"); // TODO: implement support
)

(
~loadToMidi = {
	arg pat, parg, low, high, curve, defaultValue = low, button;

	var symbol = pat ++ "_" ++ parg;
    Ndef(symbol).clear;
	Pbindef(pat, parg, Pdefn(symbol));
    Pdefn(symbol, defaultValue);
    button.action_({ |el|
        var spec = ControlSpec.new(low, high, curve);
        var value = spec.map(el.value);
        Pdefn(symbol, value);
        (symbol ++ ": " ++ value.asString).postln;
    });
    button.elemDesc.label = symbol;
};
~loadToMidiWithNdef = {
	arg pat, parg, low, high, curve, defaultValue = low, button;

	var symbol = pat ++ "_" ++ parg;
    Ndef(symbol).clear;
	Pbindef(pat, parg, Pdefn(symbol));
    Pdefn(symbol, Ndef(symbol, { defaultValue }));
    button.action_({ |el|
        var spec = ControlSpec.new(low, high, curve);
        var value = spec.map(el.value);
        Pdefn(symbol, Ndef(symbol, { value }));
        (symbol ++ ": " ++ value.asString).postln;
    });
    button.elemDesc.label = symbol;
}
)

MKtl.find('midi');

(
~loadToMidi.value(\p_kick_techno, \amp, 0, 0.5, 'lin', 0.5, m.elAt('sl', '1'));
~loadToMidi.value(\p_kick_techno, \rel, 0.2, 0.5, 'lin', 0.25, m.elAt('kn', '3', '1'));
~loadToMidi.value(\p_kick_techno, \fRel, 0.02, 0.25, 'lin', 0.1, m.elAt('kn', '2', '1'));
)

Pdef(\p_kick_techno).play(argClock: t);
Pdef(\p_kick_techno).stop;

