(
s.options.memSize = 65536;
a = ProxySpace.push(s.boot());
a.clock = TempoClock.default;
a.fadeTime = 5;

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

///////////////////////////////////////////////////
// Granulator (TGrains)
///////////////////////////////////////////////////
n = { |name|
	Ndef(name, {
		arg centerPos = #[0, 1],
		trigger = #[0.1, 50],
		rate = #[1, 1],
		grainDur = #[0.001, 2],
		pan = #[-1, 1],
		amp = #[0, 1],
		reverseProb = 0.5;

		var trigWhite, trigFreqMess, centerPosWhite, durWhite, panWhite, ampWhite, rateWhite, coin, reverse, sig;

		//trigFreqMess = LFNoise2.kr(12).range(0.5, 1);
		trigWhite = Impulse.kr(LFNoise0.kr(1).range(trigger[0], trigger[1]));

		centerPosWhite = Dwhite(centerPos[0], centerPos[1]);
		durWhite = Dwhite(grainDur[0], grainDur[1]);
		panWhite = Dwhite(pan[0], pan[1]);
		ampWhite = Dwhite(amp[0], amp[1]);
        rateWhite = Dwhite(rate[0], rate[1]);
		coin = CoinGate.kr(reverseProb, trigWhite);
		reverse = Select.kr(coin, [1, -1]);
		// reverse.poll(trig);

		Demand.kr(trigWhite, 0, [durWhite, panWhite, ampWhite, rateWhite]);

        sig = TGrains.ar(
			numChannels: 2,
			trigger: trigWhite,
			dur: durWhite,
			bufnum: e.buffers[name],
			rate: rateWhite * reverse,
			centerPos: centerPosWhite * BufDur.kr(e.buffers[name].bufnum),
            interp: 4,
			pan: panWhite,
            amp: ampWhite,
        );

        sig
	});

    Ndef(name).fadeTime = 5;
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
		reverseProb = 0.5;

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
            envbufnum: -1 // default Hann envelope
        );

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
// EQ template
///////////////////////////////////////////////////
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
)

///////////////////////////////////////////////////
// Master
///////////////////////////////////////////////////
(
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
)

(
var y, h, c;

y = Signal.hammingWindow(s.sampleRate * 0.4);
h = Array.fill(s.sampleRate * 0.2, {|i| y[i]});

c = Buffer.alloc(s, s.sampleRate * 0.2, 1);
c.loadCollection(h);

e.signalWindows['hamming'] = c;
)

l.value('gran');
n.value('gran');
g.value('gran');

Ndef('gran').clear;

StageLimiter.activate;
StageLimiter.deactivate;
s.meter;
