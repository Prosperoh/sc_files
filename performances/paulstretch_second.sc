
Ndef('first').clear;

(
e.buffers['frecEnvSignal'].free;
e.buffers['frecEnvSignal'] = Env([0, 1, 1, 0], [1, 4, 1], \sin, 1).asSignal(s.sampleRate);
e.buffers['frecEnvBuf'].free;
e.buffers['frecEnvBuf'] = Buffer.alloc(s, s.sampleRate, 1);
e.buffers['frecEnvBuf'].loadCollection(e.buffers['frecEnvSignal']);
)

// use pianomess with stretch ~50, makes a nice "chord" loop
l.value('first');
e.sb['pstretch_singledir'].value('first');
h.value('first');

l.value('loophigher');
e.sb['pstretch_singledir'].value('loophigher');
h.value('loophigher');

(
Ndef('loophigherfx', {
    var sig, amp;

    sig = \in.ar([0, 0]);

    sig
});
)

z = NdefMixer(s);

l.value('frec');
m.value('frec');
Ndef('frec').set(\envBufNum, e.buffers['frecEnvBuf'].bufnum);
g.value('frec');

Ndef('frecrev');

l.value('frechigh');
m.value('frechigh');
Ndef('frechigh').set(\envBufNum, e.buffers['frecEnvBuf'].bufnum);
g.value('frechigh');
Ndef('frechigh');

l.value('noise');
n.value('noise');
g.value('noise');

Ndef('frec').clear;

(
var fftSize;
fftSize = 2 ** floor(log2(0.25 * SampleRate.ir));

Ndef(\frecfx, {
    arg mix = 0.33, room = 0.5, damp = 0.5, wipe = 0.0,
        width = 0.5, trig = 0,
        in1_amp = 1, in2_amp = 1, in3_amp = 1, in4_amp = 1;

    var sig, fft, amp;

    sig = Mix([
        \in1.ar([0, 0]) * in1_amp,
        \in2.ar([0, 0]) * in2_amp,
        \in3.ar([0, 0]) * in3_amp,
        \in4.ar([0, 0]) * in4_amp
    ]);

    //fft = FFT(Array.fill(2, { LocalBuf(fftSize, 1) }), sig);
    //fft = PV_BinScramble(fft, wipe, width, trig);
    //sig = IFFT(fft);

    // reverb
    sig = FreeVerb.ar(sig, mix, room, damp);

    // noise amp
    amp = LFNoise0.kr(0.5).range(0, 1);

    sig * amp
});
ControlSpec.add(\wipe, [0, 1, \lin]);
ControlSpec.add(\width, [0, 1, \lin]);
ControlSpec.add(\trig, [-1, 1, \lin]);
)

e.buffers['frecEnvBuf'];

Ndef('frecfx') <<>.in1 Ndef('frec');
Ndef('frecfx') <<>.in2 Ndef('frechigh');




// can be used as basis, then trigger with some effects, and let it flow for a while...


(
Ndef('frechigh').set('trigger', [ 3.4131012453668, 5.4656779371547 ], 'pan', [ -0.5, 0.5 ], 'rate', [ 0.52440590093881, 0.52440590093881 ], 'centerPos', [ 0.043811701484095, 0.87666658426448 ], 'reverseProb', 0.0, 'hpfreq', 1240.9296621516, 'fadeTime', 5, 'lpfreq', 9628.7668600903);
Ndef('frec').set('trigger', [ 0.22287753627442, 0.38522133841897 ], 'fadeTime', 5, 'grainDur', [ 0.030646112025125, 0.61712540003738 ], 'centerPos', [ 0.026665360501567, 0.95666634012539 ]);
Ndef('first').set('stretchMultiplier', 1.0, 'pan', 0, 'stretch', 77.184583032126, 'pos', [ 0.0083329826776071, 0.67333333333333 ], 'wipe', 0.29095354523227, 'hpfreq', 96.953210100917, 'noteShift', -12.0, 'lpfreq', 18000.0);
Ndef('frecfx').set('trig', 0.0052910052910053, 'in1_amp', 0.50793650793651, 'damp', 0.28042328042328, 'room', 0.1005291005291, 'in1', Ndef('frec'), 'in2', Ndef('frechigh'), 'wipe', 1.0, 'mix', 0.83068783068783);
);
