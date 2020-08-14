// with Ndef
(
e.sb['pstretch'] = { |name|
    Ndef(name, { 
        arg pan = 0,
            width = 1,
            pos = #[0, 1],
            stretch = 50,
            stretchMultiplier = 1,
            noteShift = 0,
            wipe = 0;

        // Paulstretch for SuperCollider
        // Based on the Paul's Extreme Sound Stretch algorithm by Nasca Octavian PAUL
        // https://github.com/paulnasca/paulstretch_python/blob/master/paulstretch_steps.png
        //
        // By Jean-Philippe Drecourt
        // http://drecourt.com
        // April 2020
        var window, stretchVal, trigPeriod, trig,
            bufnum, envBufnum,
            posSignal, posVal, posNextVal, posScale, posMax, posNSteps, posStep,
            fftSize, sig, rate, grains,
            mCoeff, sCoeff, mValue, sValue;

        bufnum = e.buffers[name].bufnum;
        envBufnum = e.buffers['envBuf'].bufnum;

        // should be fixed to avoid glitches?
        window = 0.25;

        stretchVal = stretch * stretchMultiplier;

        // pitch value
        rate = 2 ** (noteShift / 12.0);

        // Calculating fft buffer size according to suggested window size
        fftSize = 2 ** floor(log2(window * SampleRate.ir));

        // Grain parameters
        // The grain is the exact length of the FFT window
        trigPeriod = fftSize / SampleRate.ir;
        trig = Impulse.kr(1 / trigPeriod);
        posStep = trigPeriod / stretchVal;
        posNSteps = (stretchVal / trigPeriod).floor;
        posSignal = Dseq([
            Dseries(0, posStep, posNSteps),
            Dseries(posStep * posNSteps, posStep.neg, posNSteps),
        ], inf);

        //posSignal = Dseries(0, posStep);

        posScale = pos[1] - pos[0];
        posSignal = posSignal.mod(1) * posScale + pos[0];
        posVal = Demand.kr(trig, 0, demandUGens: posSignal);
        posNextVal = posVal + ((trigPeriod / (2 * stretchVal)) * posScale);

        //Duty.kr(0.5, 0, Dpoll(posVal));

        // Extraction of 2 consecutive grains
        // Both grains need to be treated together for superposition afterwards
        grains = [
            GrainBuf.ar(numChannels: 1,
                trigger: trig,
                dur: trigPeriod, 
                sndbuf: bufnum,
                rate: rate,
                pos: posVal, 
                envbufnum: envBufnum),
            GrainBuf.ar(numChannels: 1,
                trigger: trig,
                dur: trigPeriod, 
                sndbuf: bufnum,
                rate: rate,
                pos: posNextVal,
                envbufnum: envBufnum)
        ];

        // FFT magic
        sig = [0, 1].collect({
            var isig;

            isig = grains.collect({ |item, i|
                var chain;

                chain = FFT(LocalBuf(fftSize), item, hop: 1.0, wintype: -1);
                // PV_Diffuser is only active if its trigger is 1
                // And it needs to be reset for each grain to get the smooth envelope
                chain = PV_Diffuser(chain, 1 - trig);

                chain = PV_RandComb(chain, wipe, 1 - trig);

                item = IFFT(chain, wintype: -1);
            });

            // Reapply the grain envelope because the FFT phase randomization removes it
            isig = isig * PlayBuf.ar(1, envBufnum, 1/(trigPeriod), loop:1);
            // Delay second grain by half a grain length for superposition
            isig[1] = DelayC.ar(isig[1], trigPeriod/2, trigPeriod/2);

            Mix.new(isig)
        });

        // stereo widening
        mCoeff = 1 / (1 + width);
        sCoeff = width * 0.5;

        mValue = (sig[0] + sig[1]) * mCoeff;
        sValue = (sig[0] - sig[1]) * sCoeff;

        // Panned output
        Balance2.ar(mValue + sValue, mValue - sValue, pos: pan)
    });
};

ControlSpec.add(\stretch, [1, 100, \exp]);
ControlSpec.add(\stretchMultiplier, [1, 300, \exp]);
ControlSpec.add(\window, [0.125, 16, \exp]);
ControlSpec.add(\noteShift, [-36, 36, \lin]);
ControlSpec.add(\wipe, [0, 1, \lin]);
ControlSpec.add(\pos, [0, 1, \lin]);
ControlSpec.add(\width, [0, 1, \lin]);
)

(
// Example
e.buffers['envBuf'].free;
// The grain envelope
e.buffers['envSignal'] = Signal.newClear(s.sampleRate).waveFill({
    |x| (1 - x.pow(2)).pow(1.25)
}, -1.0, 1.0);
e.buffers['envBuf'] = Buffer.alloc(s, s.sampleRate, 1);
e.buffers['envBuf'].loadCollection(e.buffers['envSignal']);

h = { |name|
	var window, soundFile, soundFileView, flowLayout,
	width = 600,
	soundFileHeight = 200,
	nDefHeight = 200,
	margin = 20,
	gap = 5;

	window = Window.new("PaulStretch sample",
		Rect(50, 50,
			width + (2 * margin),
			(soundFileHeight + nDefHeight) + gap + (2 * margin)),
		false).front;
	window.background = Color.grey(0.7, 0.9);
	flowLayout = window.addFlowLayout(margin@margin, gap@gap);

	soundFileView = SoundFileView.new(window, Rect(0, 0, width, soundFileHeight))
	.gridOn_(false);
	// What to do when user selects portion of sound file directly
	// (i.e., on waveform, not using slider)
	soundFileView.mouseUpAction = {
        arg view;
		var loFrames, hiFrames, loSlider, hiSlider;
		loFrames = view.selection(0)[0];
		hiFrames = view.selection(0)[1] + loFrames;
		Ndef(name).set(\pos, [
			loFrames / e.buffers[name].numFrames,
			hiFrames / e.buffers[name].numFrames,
		]);
	};
	soundFile = SoundFile.new();
	soundFile.openRead(e.buffers[name].path);
	soundFileView.soundfile_(soundFile);
	soundFileView.read(0, soundFile.numFrames);

	NdefGui.new(Ndef(name), 10, bounds: width@nDefHeight, parent: window);
};
)


Ndef(\pad).clear;

// use guitar sample (115671)
l.value('pad');
e.sb['pstretch'].value('pad');
h.value('pad');

Ndef(\pad).fadeTime = 10;


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
Ndef(\master) <<>.in1 Ndef(\padeq);
Ndef(\master) <<>.in2 Ndef(\fx1);
Ndef(\master) <<>.in3 Ndef(\pad2eq);
Ndef(\master) <<>.in4 Ndef(\frec);





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

