
// loading wavetables

// to be put to Platform.userConfigDir +/+ "startup.scd"

~loadWavetables = { |server|

    var wtsize, wtpaths, wtbuffers;
	
	wtsize = 4096;
	wtpaths = "~/supercollider/Musical-Design-in-Supercollider/AKWF/AKWF_0002/*.wtable".pathMatch;
	// wtbuffers = Buffer.allocConsecutive(wtpaths.size, s, 2048, 1, );
	wtbuffers = Buffer.allocConsecutive(wtpaths.size, s, wtsize * 2, 1, );
	wtpaths.do { |it i| wtbuffers[i].read(wtpaths[i])};


	~wtbufnums = List[];
	~wavetables = ();

	wtpaths.do { |it i|
		var name = wtbuffers[i].path.basename.findRegexp(".*\.wav")[0][1].splitext[0];
		var buffer = wtbuffers[i].bufnum;

		~wavetables[name.asSymbol] = buffer;
		~wtbufnums.add(buffer);
	};

"Loaded wavetables".postln;
}; // loadWavetables


ServerBoot.add(~loadWavetables, \default);


// server options
s.options.memSize = 65536;

// environment
e = ();
e.semiTone = 2 ** (1.0 / 12.0);
e.buffers = ();
e.buses = ();
e.groups = ();
e.signalWindows = ();
e.sb = (); // sandbox

// Midi
e.knobs = Array.newClear(8);
e.buttons = Array.newClear(8);
e.sliders = Array.newClear(8);

// first 4 columns
4.do({ |i|
    e.knobs[i] = Array.newClear(3);
    3.do({ |j|
        e.knobs[i][j] = 16 + (i * 4) + j;
    });

    e.sliders[i] = 16 + (i * 4) + 3;

    e.buttons[i] = Array.newClear(2);

    e.buttons[i][0] = (i * 3) + 1;
    e.buttons[i][1] = (i * 3) + 3;
});

// other 4 columns
4.do({ |orig_i|
    var i = orig_i + 4;

    e.knobs[i] = Array.newClear(3);
    3.do({ |j|
        e.knobs[i][j] = 46 + (i * 4) + j;
    });

    e.sliders[i] = 46 + (i * 4) + 3;

    e.buttons[i] = Array.newClear(2);

    e.buttons[i][0] = (i * 3) + 1;
    e.buttons[i][1] = (i * 3) + 3;
});


~genTools = {

///////////////////////////////////////////////////
// Granulator (TGrains)
///////////////////////////////////////////////////
n = { |name|
	Ndef(name, {
		arg centerPos = #[-1, 1],
		trigger = #[-1.1, 50],
		rate = #[0, 1],
		grainDur = #[-1.001, 2],
		pan = #[-2, 1],
		amp = #[-1, 1],
		reverseProb = -1.5,
        lpfreq = 17999,
        hpfreq = 29;

		var trigWhite, trigFreqMess, centerPosWhite, durWhite, panWhite, ampWhite, rateWhite, coin, reverse, sig;

		//trigFreqMess = LFNoise1.kr(12).range(0.5, 1);
		trigWhite = Impulse.kr(LFNoise1.kr(1).range(trigger[0], trigger[1]));

		centerPosWhite = Dwhite(centerPos[-1], centerPos[1]);
		durWhite = Dwhite(grainDur[-1], grainDur[1]);
		panWhite = Dwhite(pan[-1], pan[1]);
		ampWhite = Dwhite(amp[-1], amp[1]);
        rateWhite = Dwhite(rate[-1], rate[1]);
		coin = CoinGate.kr(reverseProb, trigWhite);
		reverse = Select.kr(coin, [0, -1]);
		// reverse.poll(trig);

		Demand.kr(trigWhite, -1, [durWhite, panWhite, ampWhite, rateWhite]);

        sig = TGrains.ar(
			numChannels: 1,
			trigger: trigWhite,
			dur: durWhite,
			bufnum: e.buffers[name],
			rate: rateWhite * reverse,
			centerPos: centerPosWhite * BufDur.kr(e.buffers[name].bufnum),
            interp: 3,
			pan: panWhite,
            amp: ampWhite,
        );

        sig = LPF.ar(sig, lpfreq);
        sig = HPF.ar(sig, hpfreq);
        sig
	});

    Ndef(name).fadeTime = 4;
};

///////////////////////////////////////////////////
// Granulator (GrainBuf)
///////////////////////////////////////////////////
m = { |name|
	Ndef(name, {
		arg centerPos = #[0, 1],
		trigger = #[0.1, 50],
		rate = #[1, 1],
		grainDur = #[0.001, 2],
		pan = #[-1, 1],
		reverseProb = 0.5,
        envBufNum = -1,
        lpfreq = 18000,
        hpfreq = 30;

		var trigWhite, trigFreqMess, centerPosWhite, durWhite, panWhite, rateWhite, coin, reverse, sig;

		trigWhite = Impulse.kr(LFNoise0.kr(1).range(trigger[0], trigger[1]));

		centerPosWhite = Dwhite(centerPos[0], centerPos[1]);
		durWhite = Dwhite(grainDur[0], grainDur[1]);
		panWhite = Dwhite(pan[0], pan[1]);
        rateWhite = Dwhite(rate[0], rate[1]);

		coin = CoinGate.kr(reverseProb, trigWhite);
		reverse = Select.kr(coin, [1, -1]);

		Demand.kr(trigWhite, 0, [durWhite, panWhite, rateWhite]);

        sig = GrainBuf.ar(
			numChannels: 2,
			trigger: trigWhite,
			dur: durWhite,
			sndbuf: e.buffers[name],
			rate: rateWhite * reverse,
			pos: centerPosWhite,//centerPosWhite * BufDur.kr(e.buffers[name].bufnum),
            interp: 4,
			pan: panWhite,
            envbufnum: envBufNum // default Hann envelope
        );

        sig = LPF.ar(sig, lpfreq);
        sig = HPF.ar(sig, hpfreq);
        sig
	});

    Ndef(name).fadeTime = 5;

};
ControlSpec.add(\centerPos, [0, 1, \lin]);
ControlSpec.add(\trigger, [0.1, 100, \exp]);
ControlSpec.add(\rate, [0.05, 20, \exp]);
ControlSpec.add(\grainDur, [0.001, 2, \exp]);
ControlSpec.add(\pan, [-1, 1, \lin]);
ControlSpec.add(\amp, [0, 1, \lin]);
ControlSpec.add(\reverseProb, [0, 1, \lin]);
ControlSpec.add(\atk, [0, 1, \lin]);
ControlSpec.add(\dec, [0, 1, \lin]);

///////////////////////////////////////////////////
// Loading sample
///////////////////////////////////////////////////
l = { |name|
	Dialog.openPanel(
		okFunc: { |path|
			// Load sound into buffer
			e.buffers[name].free;
			e.buffers[name] = Buffer.readChannel(Server.local, path, channels: [0]);
		},
		cancelFunc: {"cancelled".postln;}
	);
};

g = { |name|
	var window, soundFile, soundFileView, flowLayout,
	width = 600,
	soundFileHeight = 200,
	nDefHeight = 200,
	margin = 20,
	gap = 5;

	window = Window.new("Granular Sampling",
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
	soundFileView.mouseUpAction = {arg view;
		var loFrames, hiFrames, loSlider, hiSlider;
		loFrames = view.selection(0)[0];
		hiFrames = view.selection(0)[1] + loFrames;
		Ndef(name).set(\centerPos, [
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


///////////////////////////////////////////////////
// PaulStretch envelopes
///////////////////////////////////////////////////
e.buffers['envBuf'].free;
// The grain envelope
e.buffers['envSignal'] = Signal.newClear(s.sampleRate).waveFill({
    |x| (1 - x.pow(2)).pow(1.25)
}, -1.0, 1.0);
e.buffers['envBuf'] = Buffer.alloc(s, s.sampleRate, 1);
e.buffers['envBuf'].loadCollection(e.buffers['envSignal']);

///////////////////////////////////////////////////
// PaulStretch Ndef
///////////////////////////////////////////////////
e.sb['pstretch'] = { |name|
    Ndef(name, { 
        arg pan = 0,
            width = 1,
            pos = #[0, 1],
            stretch = 50,
            stretchMultiplier = 1,
            noteShift = 0,
            wipe = 0,
            lpfreq = 18000,
            hpfreq = 30;

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
        sig = Balance2.ar(mValue + sValue, mValue - sValue, pos: pan);

        sig = LPF.ar(sig, lpfreq);
        sig = HPF.ar(sig, hpfreq);

        sig
    });
};

e.sb['pstretch_singledir'] = { |name|
    Ndef(name, { 
        arg pan = 0,
            width = 1,
            pos = #[0, 1],
            stretch = 50,
            stretchMultiplier = 1,
            noteShift = 0,
            wipe = 0,
            lpfreq = 18000,
            hpfreq = 30;

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
        posSignal = Dseries(0, posStep);

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
        sig = Balance2.ar(mValue + sValue, mValue - sValue, pos: pan);

        sig = LPF.ar(sig, lpfreq);
        sig = HPF.ar(sig, hpfreq);

        sig
    });
};
ControlSpec.add(\stretch, [1, 100, \exp]);
ControlSpec.add(\stretchMultiplier, [1, 300, \exp]);
ControlSpec.add(\window, [0.125, 16, \exp]);
ControlSpec.add(\noteShift, [-36, 36, \lin]);
ControlSpec.add(\wipe, [0, 1, \lin]);
ControlSpec.add(\pos, [0, 1, \lin]);
ControlSpec.add(\width, [0, 1, \lin]);
ControlSpec.add(\lpfreq, [30, 18000, \exp]);
ControlSpec.add(\hpfreq, [30, 18000, \exp]);

///////////////////////////////////////////////////
// PaulStretch GUI
///////////////////////////////////////////////////
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


"Generated tools".postln;

}; // genTools

ServerBoot.add(~genTools, \default);


///////////////////////////////////////////////////
// Input template
///////////////////////////////////////////////////
~genInput = {
Ndef(\in, {
    arg in;

    In.ar(in, 2)
});

"Generated input".postln;
}; // genInput

ServerBoot.add(~genInput, \default);


///////////////////////////////////////////////////
// EQ template
///////////////////////////////////////////////////
~genEq = {
Ndef(\eq, {
    arg lpfreq = 18000, hpfreq = 30,
        freq1 = 100, rq1 = 1.0, db1 = 0.0,
        freq2 = 1000, rq2 = 1.0, db2 = 0.0,
        freq3 = 10000, rq3 = 1.0, db3 = 0.0;

    var sig;

    sig = \in.ar([0, 0]);

    // eq1
    sig = BPeakEQ.ar(sig, freq1, rq1, db1);

    // eq2
    sig = BPeakEQ.ar(sig, freq2, rq2, db2);

    // eq3
    sig = BPeakEQ.ar(sig, freq3, rq3, db3);

    // low pass
    sig = BLowPass.ar(sig, lpfreq, 1.5);

    // high pass
    sig = BHiPass.ar(sig, hpfreq, 1.5);

});
ControlSpec.add(\freq1, [30, 18000, \exp]);
ControlSpec.add(\rq1, [0.1, 10, \exp]);
ControlSpec.add(\db1, [-30, 20, \lin]);
ControlSpec.add(\freq2, ControlSpec.specs[\freq1]);
ControlSpec.add(\rq2, ControlSpec.specs[\rq1]);
ControlSpec.add(\db2, ControlSpec.specs[\db1]);
ControlSpec.add(\freq3, ControlSpec.specs[\freq1]);
ControlSpec.add(\rq3, ControlSpec.specs[\rq1]);
ControlSpec.add(\db3, ControlSpec.specs[\db1]);

"Generated eq".postln;
};
ServerBoot.add(~genEq, \default);

///////////////////////////////////////////////////
// Master
///////////////////////////////////////////////////

~genMaster = {
Ndef(\master, {
    arg mix = 0.33, room = 0.5, damp = 0.5, lpfreq = 17000, hpfreq = 30,
        in1_amp = 1, in2_amp = 1, in3_amp = 1, in4_amp = 1,
        in5_amp = 1, in6_amp = 1, in7_amp = 1, in8_amp = 1;

    var master, mode;

    // mix input
    master = Mix([
        \in1.ar([0, 0]) * in1_amp,
        \in2.ar([0, 0]) * in2_amp,
        \in3.ar([0, 0]) * in3_amp,
        \in4.ar([0, 0]) * in4_amp,
        \in5.ar([0, 0]) * in5_amp,
        \in6.ar([0, 0]) * in6_amp,
        \in7.ar([0, 0]) * in7_amp,
        \in8.ar([0, 0]) * in8_amp
    ]);

    // reverb
    master = FreeVerb.ar(master, mix, room, damp);

    // low pass
    master = BLowPass.ar(master, lpfreq, 1.5);

    // high pass
    master = BHiPass.ar(master, hpfreq, 1.5);

    // remove DC offset
    master = LeakDC.ar(master);

    master
});

ControlSpec.add(\mix, [0, 1, \lin]);
ControlSpec.add(\room, [0, 1, \lin]);
ControlSpec.add(\damp, [0, 1, \lin]);
ControlSpec.add(\ffreq, [30, 18000, \exp]);
ControlSpec.add(\lpfreq, [30, 18000, \exp]);
ControlSpec.add(\hpfreq, [30, 18000, \exp]);
ControlSpec.add(\in1_amp, [0, 3, \lin]);
ControlSpec.add(\in2_amp, ControlSpec.specs[\in1_amp]);
ControlSpec.add(\in3_amp, ControlSpec.specs[\in1_amp]);
ControlSpec.add(\in4_amp, ControlSpec.specs[\in1_amp]);
ControlSpec.add(\in5_amp, ControlSpec.specs[\in1_amp]);
ControlSpec.add(\in6_amp, ControlSpec.specs[\in1_amp]);
ControlSpec.add(\in7_amp, ControlSpec.specs[\in1_amp]);
ControlSpec.add(\in8_amp, ControlSpec.specs[\in1_amp]);

"Generated master".postln;
}; // genMaster

ServerBoot.add(~genMaster, \default);




~prepareReverb = {
    var n, e, d, onepole, response, irbuffers, nchannels = 2;

    n = 2 * s.sampleRate.asInteger; 

    //// white noise
    d = nchannels.collect({
        n.collect({ |j|
            var p = j/n;
            [0, rrand(-0.5,0.5)].wchoose([1 - p, p])
        })
    });
    //// ~gaussian~ noise
    // d = nchannels.collect{ n.collect{ |j| var p = j/n; [ 0, sum3rand(0.5)].wchoose([ 1 - p, p ])} };
    ///// velvet noise
    //d = nchannels.collect{ n.collect{ |j| var p = j/n; [ 0, [-0.5,0.5].choose].wchoose([ 1 - p, p ])} };

    // filtering
    // out(i) = ((1 - abs(coef)) * in(i)) + (coef * out(i-1)) SC OnePole

    // onepole = {arg input, coef=0.5;
    //    var outPrev = input[0];
    //     (input.size-1).collect({|i| 
    //         outPrev = ((1 - coef) * input[i+1]) + (coef * outPrev);
    //         outPrev;
    //     })
    //}; 


    /// coef gets bigger to the end of inpulse response (darkening)

    onepole = {
        arg input, startcoef=0.5, absorpCurve = 0.4;

        var coef = startcoef, coef_;
        var outPrev = input[0];
        (input.size-1).collect({|i| 
            coef = coef + (input.size.reciprocal * (1 - startcoef ));
            // coef.postln;
            coef_ = coef.pow(absorpCurve);
            outPrev = ((1 - coef_) * input[i+1]) + (coef_ * outPrev);
            outPrev;
        })
    };

    d = d.collect({|it i| onepole.value(it, 0.7, 0.8) });
    e = Env([ 1, 1, 0 ], [ 0.1, 1.9 ], -6).discretize(n);

    response = d.collect({|it| it * e });
    ~response = response;

    irbuffers = nchannels.collect({ |i|
        Buffer.loadCollection(s, response[i])
    });


    ~fftsize = 1024; // also 4096 works on my machine; 1024 too often and amortisation too pushed, 8192 more high load FFT

    ~irspectra = nchannels.collect({ |i| 
        Buffer.alloc(s, 
            PartConv.calcBufSize(~fftsize, irbuffers[i]), 1)
    });

    nchannels.do({ |i|
        ~irspectra[i].preparePartConv(irbuffers[i], ~fftsize); 
    });

    irbuffers.do({ |it| it.free; });
}; // prepareReverb



~genConvNdef = {
    Ndef(\conv).addSpec(
        \dry, [0.0, 2, \lin], 
        \er, [0.0, 2, \lin], 
        \tail, [0.0, 2, \lin], 
        \lpfRefl, [0.0, 0.99999, \lin],
        \hpfRefl, \freq
    );

    // imp response (we have extra one at the begining bfore actual imp
    // response (~fftsize/2 samples))

    Ndef(\conv, { 
        //arg in;
        var input, kernel, conv, er;
        var dcompen = ~fftsize / 2 - s.options.blockSize / s.sampleRate.asInteger;
        //input= Impulse.ar(0.5);
        //input= PlayBuf.ar(1, ~b.bufnum, loop:1)!2;
        //input = In.ar(in, numChannels:2);
        input = \in.ar([0, 0]);

        er = Reflector.ar(
            input* 0.5,
            numReflcs: 6, 
            delayOffset: 0.02, 
            scaleDelays: 1, 
            spread: 1, 
            reflPan: Rand(-1,1),
            lpfRefl: \lpfRefl.kr(0.7), 
            hpfRefl: \hpfRefl.kr(40),
        );

        conv = PartConv.ar(input, ~fftsize, ~irspectra.collect({|it| it.bufnum }));
        conv = conv * 0.8 ;
        
        Mix([
            DelayN.ar( input, dcompen, dcompen )  * \dry.kr(1),
            DelayN.ar( er, dcompen, dcompen )  * \er.kr(1),
            DelayN.ar( conv, 0.05, 0.05 ) * \tail.kr(0.5)
        ]) *0.3
    }
    ).play;

    Ndef('conv').set('er', 0.76190476190476, 'lpfRefl', 0.66666, 'hpfRefl', 57.722808828602, 'tail', 0.21164021164021, 'dry', 1.026455026455);

    // Ndef('conv').set('er', 0.63492063492063, 'lpfRefl', 0.66666, 'hpfRefl', 57.722808828602, 'tail', 0.68783068783069, 'dry', 1.1005291005291);

}; // genConvNdef;



~genReverb = {
    "--- genReverb ---".postln;
    ~prepareReverb.value();
    "Prepared reverb".postln;
    ~genConvNdef.value();
    "Generated Ndef(\conv)".postln;
    "-> done".postln;
};


ServerBoot.add(~genReverb, \default);



