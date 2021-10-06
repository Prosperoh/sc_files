Ndef(\pad).clear;

// use guitar sample (115671)
l.value('pad');
e.sb['pstretch'].value('pad');
h.value('pad');

l.value('orn');
n.value('orn');
g.value('orn');

Ndef(\pad).fadeTime = 10;

Ndef(\pad).copy(\padcopy);
Ndef(\pad).copy(\padcopy2);

Ndef(\padcopy).gui;
Ndef(\padcopy2).gui;

// part1: increasing noise
// 0.66 -> 0.45 (increasing volume)
Ndef(\pad).xset(\pos, [0.65, 0.68]);
Ndef(\pad).xset(\pos, [0.55, 0.6]);
Ndef(\pad).xset(\pos, [0.5, 0.54]);
Ndef(\pad).xset(\pos, [0.47, 0.52]);

// part2: with pad
// 0.23 -> 0.0 (increasing volume)
Ndef(\pad).xset(\pos, [0.2, 0.23]);
Ndef(\pad).xset(\pos, [0.15, 0.2]);
Ndef(\pad).xset(\pos, [0.1, 0.15]);
Ndef(\pad).xset(\noteShift, 0);
Ndef(\pad).xset(\noteShift, -2);
Ndef(\pad).xset(\noteShift, 4);

// part3: other pad
// <= 0.91
Ndef(\pad).xset(\pos, [0.75, 0.8]);
Ndef(\pad).xset(\pos, [0.8, 0.85]);
Ndef(\pad).xset(\pos, [0.85, 0.9]);

Ndef(\pad).xset(\pos, [0.35, 0.4]);

Ndef(\pad).xset(\noteShift, 0);

Ndef(\master).gui;

l.value('pad2');
e.sb['pstretch'].value('pad2');
h.value('pad2');

l.value('frec');
n.value('frec');
g.value('frec');

(
var fftSize, window;
window = 0.5;
fftSize = 2 ** floor(log2(window * SampleRate.ir));
fftSize = 2048; 

Ndef(\padfx, {
    arg ampfreq = 1,
        amprange = #[0, 1];

    var sig;

    sig = \in.ar([0, 0]);
    sig = sig * LFNoise1.kr(ampfreq).range(amprange[0], amprange[1]);

});
ControlSpec.add(\amprange, [0, 1, \lin]);
ControlSpec.add(\ampfreq, [0.05, 20, \exp]);
)

Ndef(\padfx) <<>.in Ndef(\pad);
Ndef(\padfx).gui;



l.value('orn2');
n.value('orn2');
g.value('orn2');


Ndef(\pad).xset(\noteShift, -9);

Ndef(\pad).copy(\padorn);

Ndef(\padorn).xset(\noteShift, -5);
Ndef(\padorn).gui;

File.use("~/pad.txt".standardizePath, "w", { |f| f.write(Ndef(\pad).asCode) });



"hello".postln;

(
Ndef(\padorn).set(
    \pan, Ndef(\padorn_panpos, { LFNoise1.kr(0.5).range(-1, 1) }),
    \hpfreq, Ndef(\padorn_hpfreq, {
        arg hpfreqlow = 500, hpfreqhigh = 800;
        LFNoise1.kr(0.5).range(hpfreqlow, hpfreqhigh)
    }),
    \lpfreq, Ndef(\padorn_lpfreq, { 
        arg lpfreqlow = 700, lpfreqhigh = 1000;
        LFNoise1.kr(0.5).range(lpfreqlow, lpfreqhigh)
    })
);
ControlSpec.add(\hpfreqlow, [30, 18000, \exp]);
ControlSpec.add(\hpfreqhigh, [30, 18000, \exp]);
ControlSpec.add(\lpfreqlow, [30, 18000, \exp]);
ControlSpec.add(\lpfreqhigh, [30, 18000, \exp]);
)

(
var fftSize;
fftSize = 2 ** floor(log2(0.25 * SampleRate.ir));
fftSize = 2048; 

Ndef(\ornfx, {
    arg mix = 0.33, room = 0.5, damp = 0.5, wipe = 0.0,
        width = 0.5, trig = 0,
        in1_amp = 1, in2_amp = 1, in3_amp = 1, in4_amp = 1;

    var sig, fft;

    sig = Mix([
        \in1.ar([0, 0]) * in1_amp,
        \in2.ar([0, 0]) * in2_amp,
        \in3.ar([0, 0]) * in3_amp,
        \in4.ar([0, 0]) * in4_amp
    ]);

    fft = FFT(Array.fill(2, { LocalBuf(fftSize, 1) }), sig);
    fft = PV_BinScramble(fft, wipe, width, trig);
    sig = IFFT(fft);

    // reverb
    sig = FreeVerb.ar(sig, mix, room, damp);

    sig
});
ControlSpec.add(\wipe, [0, 1, \lin]);
ControlSpec.add(\width, [0, 1, \lin]);
ControlSpec.add(\trig, [-1, 1, \lin]);
)

Ndef(\ornfx) <<>.in1 Ndef(\orn);
Ndef(\ornfx) <<>.in2 Ndef(\orn2);

Ndef(\ornfx).gui;

z = NdefMixer(s);
ProxyMixer.new(a);


(
    var combs, comb;
    combs = [
        [-12, 0],
        [-9, 0],
        [-9, 3],
        [-15, 0], // seem to work well only on the chord shift
    ];
    comb = combs.choose;
    comb.postln;
    Ndef(\pad).xset(\noteShift, comb[0]);
    Ndef(\padorn).xset(\noteShift, comb[1]);
)

(
var fftSize;
fftSize = 2 ** floor(log2(0.5 * SampleRate.ir));
fftSize = 2048; 

Ndef(\fx1, {
    arg fftStretch = 1.0, fftShift = 0.0, threshold = 0.1,
        fftRatio = 1,
        fftStrength = 0.1,
        fftNumPartials = 24,
        hpf = 1000;

    var sig, fft, sigFft;

    sig = \in.ar([0, 0]);

    //sig = Shaper.ar(e.buffers['tf'].bufnum, sig);
    fft = FFT(Array.fill(2, { LocalBuf(fftSize, 1) }), sig);
    fft = PV_MagShift(fft, fftStretch, fftShift);
    fft = PV_SpectralEnhance(fft,
        numPartials: fftNumPartials,
        ratio: fftRatio,
        strength: fftStrength
    );
    fft = PV_PartialSynthP(fft, threshold,
        numFrames: 2, initflag: 0);
    sigFft = IFFT(fft);
    sigFft = HPF.ar(sigFft, hpf);

    sigFft * LFNoise1.kr(0.5).range(0, 1)
});
ControlSpec.add(\fx, [0, 2, \lin]);
ControlSpec.add(\fftNumPartials, [0, 48, \lin]);
ControlSpec.add(\fftStrength, [0, 1, \lin]);
ControlSpec.add(\fftRatio, [1, 8, \lin]);
ControlSpec.add(\fftStretch, [(1/12), 12, \exp]);
ControlSpec.add(\fftShift, [-24, 24, \lin]);
ControlSpec.add(\threshold, [0, pi, \lin]);
ControlSpec.add(\hpf, [20, 20000, \exp]);
)


e.buffers['tf'].plot;

Ndef(\fx1) <<>.in Ndef(\pad);

Ndef(\eq).copy(\padeq);
Ndef(\eq).copy(\pad2eq);

Ndef(\padeq) <<>.in Ndef(\pad);
Ndef(\pad2eq) <<>.in Ndef(\pad2);

h.value('pad');
Ndef(\fx1).gui;
h.value('pad2');

Ndef(\padeq).gui;
Ndef(\pad2eq).gui;

Ndef(\master).gui;

l.value(\guitar);
n.value(\guitar);
g.value(\guitar);


l.value(\basstom);
e.sb['pstretch'].value(\basstom);
h.value(\basstom);



s.freeAll;


(
var sig, n;
n = 256;
sig = Signal.chebyFill(n + 1, [0, 1, 1, 1]);
//sig = Env([-0.8, 0, 0.8], [1, 1], [20, -20]).asSignal(n+1);

e.buffers['tf'].free;
e.buffers['tf'] = Buffer.loadCollection(s, sig.asWavetableNoWrap)
)


e.buffers['tf'].plot;

