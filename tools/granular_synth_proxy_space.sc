(
a = ProxySpace.push(s.boot);
a.clock = TempoClock.default;
a.fadeTime = 5;

e = ();
e.semiTone = 2 ** (1.0 / 12.0);

// Midi
e.buffers = ();
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

n = { |name|
	Ndef(name, {
		arg centerPos = #[0, 1],
		trigger = #[0.1, 50],
		rate = #[1, 1],
		dur = #[0.001, 2],
		pan = #[-1, 1],
		amp = #[0, 1],
		reverseProb = 0.5;

		var trigWhite, trigFreqMess, centerPosWhite, durWhite, panWhite, ampWhite, rateWhite, coin, reverse, snd;

		trigFreqMess = LFNoise2.kr(12).range(0.5, 1);
		trigWhite = Impulse.kr(LFNoise0.kr(trigFreqMess).range(trigger[0], trigger[1]));

		centerPosWhite = Dwhite(centerPos[0], centerPos[1]);
		durWhite = Dwhite(dur[0], dur[1]);
		panWhite = Dwhite(pan[0], pan[1]);
		ampWhite = Dwhite(amp[0], amp[1]);
        rateWhite = Dwhite(rate[0], rate[1]);
		coin = CoinGate.kr(reverseProb, trigWhite);
		reverse = Select.kr(coin, [1, -1]);
		// reverse.poll(trig);

		Demand.kr(trigWhite, 0, [centerPosWhite, durWhite, panWhite, ampWhite, rateWhite]);

		TGrains.ar(
			numChannels: 2,
			trigger: trigWhite,
			bufnum: e.buffers[name].bufnum,
			rate: rateWhite * reverse,
			centerPos: centerPosWhite * e.buffers[name].duration,
			dur: durWhite,
			pan: panWhite,
			amp: ampWhite)
	});

    Ndef(name).fadeTime = 5;

	ControlSpec.add(\centerPos, [0, 1, \lin, 0, e.buffers[name].numFrames]);
	ControlSpec.add(\trigger, [0.1, 100, \exp]);
	ControlSpec.add(\rate, [0.05, 20, \exp]);
	ControlSpec.add(\dur, [0.001, 2, \exp]);
	ControlSpec.add(\pan, [-1, 1, \lin]);
	ControlSpec.add(\amp, [0, 1, \lin]);
	ControlSpec.add(\reverseProb, [0, 1, \lin]);
};

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

)
