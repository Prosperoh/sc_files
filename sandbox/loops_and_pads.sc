s.boot;

/* Paulstretch:
 * - take windowed segments of the sample: window size, and step
* - apply window function for each sample: window(x) = (1 - x^2)^1.25 (gives a nice dome-like function), x in [-1, 1] (x = (2*i - window_size) / window_size, i from 0 to windowsize)
 * - for each segment: run the FFT, discard original phases and add random ones, IFFT
 * - play the segments with half the window size between them
 *
 * We should be able to play with some of the parameters to achieve various effects.
 */

(
Ndef(\pad, {});
)

(
FileDialog({ |paths|
	var path = paths[0];
	postln("Loading sample: " + path);
	b = Buffer.readChannel(s, path, channels: [0]);
});
)

(
SynthDef(\play, {
	arg out = 0, amp = 0.5;

	Out.ar(out, amp * PlayBuf.ar(1, b, doneAction: Done.freeSelf) ! 2);
}).add;
)

Synth(\play);

(
SynthDef(\paulstretch, { |out = 0, bufnum, envBufnum, pan = 0, panSpread = 1, lambdaStereo = 1.0, windowSize = 4096, stretch = 32, rate = 1, amp = 0.2|
	// envbuf should be one second long

	var bufDur, grains, grainDur, grainTrig, grainPos, durBetweenGrains;

	bufDur = BufDur.kr(bufnum);

	// forcing the window size into a power of 2, for the FFT
	windowSize = 2**floor(log2(windowSize));
	grainDur = windowSize / SampleRate.ir;
	grainTrig = Impulse.ar(1 / grainDur);

	// position is increased by one grain duration every time a set of two grains should play
	stretch = LFNoise0.kr(0.5).exprange(stretch / 10, stretch * 10);
	durBetweenGrains = Demand.ar(grainTrig, 0, demandUGens: grainDur / stretch);

	// note: the pos expects to be between 0 and 1, so we normalize by the total duration of the sample
	grainPos = Demand.ar(grainTrig, 0, demandUGens: Dseries(0, durBetweenGrains / bufDur));

	// there will be two grain buffers being played, because the second grain starts playing when the first is half done
	grains = [
		GrainBuf.ar(numChannels: 1, trigger: grainTrig, dur: grainDur, sndbuf: bufnum, rate: rate, pos: grainPos, envbufnum: envBufnum),
		GrainBuf.ar(numChannels: 1, trigger: grainTrig, dur: grainDur, sndbuf: bufnum, rate: rate, pos: grainPos + (durBetweenGrains / (bufDur * 2)), envbufnum: envBufnum)
	] ! 2; // stereo

	// now inserting random phases on the grains
	grains = grains.collect({ |grain_duo, i|
		grain_duo.collect({ |grain, j|
			var chain;
			chain = FFT(LocalBuf(windowSize), grain, hop: 1.0, wintype: -1); // not sure what hop and wintype are
			chain = PV_Diffuser(chain, 1 - grainTrig); // random phase is reset for each grain thanks to the trigger
			chain = IFFT(chain, wintype: -1);

			// the phase randomization removed the amplitude envelope, applying it again
			chain = chain * PlayBuf.ar(1, envBufnum, rate: 1 / grainDur, loop: 1);
			chain
		})
	});

	// now delaying the second grain to make it start at half the first grain duration
	grains.do({ |grain_duo, i|
		grain_duo[1] = DelayC.ar(grain_duo[1], grainDur / 2, grainDur / 2);
	});

	grains = grains.collect({ |grain_duo, i|
		Mix.new(grain_duo)
	});

	grains = ((1 - lambdaStereo) * Pan2.ar(grains[0], pos: pan)) + (lambdaStereo * Splay.ar(grains, spread: panSpread, center: pan));

	Out.ar(out, grains);
}).add;
)

(
var envSignal;

e = Buffer.alloc(s, s.sampleRate, 1);
envSignal = Signal.newClear(s.sampleRate).waveFill({ |x| (1 - x.pow(2)).pow(1.25) }, -1.0, 1.0);
e.loadCollection(envSignal);
)


// noiseloop025.wav, I think
(
p.free;
p = Synth(\paulstretch, [
	\bufnum, b,
	\envbufnum, e,
	\stretch, 1.0, // LFNoise0.kr(0.5).exprange(stretch / 10, stretch * 10)
	\windowSize, 4096,
	\rate, -12.midiratio,
	\panSpread, 1.0,
	\amp, 0.5,
]);
)

p.set(\lambdaStereo, 1.0);

p.destroy;
p.free;

p.play;
p.stop;

Synth(\play);


































// this is the reference (and it works...)
(
SynthDef(\paulstretchMono, { |out = 0, bufnum, envBufnum, pan = 0, stretch = 128, window = 0.25, amp = 1|
	// Paulstretch for SuperCollider
	// Based on the Paul's Extreme Sound Stretch algorithm by Nasca Octavian PAUL
	// https://github.com/paulnasca/paulstretch_python/blob/master/paulstretch_steps.png
	//
	// By Jean-Philippe Drecourt
	// http://drecourt.com
	// April 2020
	//
	// Arguments:
	// out: output bus (stereo output)
	// bufnum: the sound buffer. Must be Mono. (Use 2 instances with Buffer.readChannel for stereo)
	// envBufnum: The grain envelope buffer created as follows:
	//// envBuf = Buffer.alloc(s, s.sampleRate, 1);
	//// envSignal = Signal.newClear(s.sampleRate).waveFill({|x| (1 - x.pow(2)).pow(1.25)}, -1.0, 1.0);
	//// envBuf.loadCollection(envSignal);
	// pan: Equal power panning, useful for stereo use.
	// stretch: stretch factor (modulatable)
	// window: the suggested grain size, will be resized to closest fft window size
	// amp: amplification
	var trigPeriod, sig, chain, trig, pos, fftSize;
	// Calculating fft buffer size according to suggested window size
	fftSize = 2**floor(log2(window*SampleRate.ir));
	// Grain parameters
	// The grain is the exact length of the FFT window
	trigPeriod = fftSize/SampleRate.ir;
	trig = Impulse.ar(1/trigPeriod);
	pos = Demand.ar(trig, 0, demandUGens: Dseries(0, trigPeriod/stretch));
	// Extraction of 2 consecutive grains
	// Both grains need to be treated together for superposition afterwards
	sig = [GrainBuf.ar(1, trig, trigPeriod, bufnum, 1, pos, envbufnum: envBufnum),
		GrainBuf.ar(1, trig, trigPeriod, bufnum, 1, pos + (trigPeriod/(2*stretch)), envbufnum: envBufnum)]*amp;
	// FFT magic
	sig = sig.collect({ |item, i|
		chain = FFT(LocalBuf(fftSize), item, hop: 1.0, wintype: -1);
		// PV_Diffuser is only active if its trigger is 1
		// And it needs to be reset for each grain to get the smooth envelope
		chain = PV_Diffuser(chain, 1 - trig);
		item = IFFT(chain, wintype: -1);
	});
	// Reapply the grain envelope because the FFT phase randomization removes it
	sig = sig*PlayBuf.ar(1, envBufnum, 1/(trigPeriod), loop:1);
	// Delay second grain by half a grain length for superposition
	sig[1] = DelayC.ar(sig[1], trigPeriod/2, trigPeriod/2);
	// Panned output_
	Out.ar(out, Pan2.ar(Mix.new(sig), pan));
}).add;
)

(
p.free;
p = Synth(\paulstretchMono, [
	\bufnum, b,
	\envbufnum, e,
	\stretch, 100,
	\window, 0.125,
	\amp, 0.5,
]);
)

p.set(\window, 0.125);

p.free;

s.quit;