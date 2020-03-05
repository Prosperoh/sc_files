(
p = ProxySpace.push(s.boot);
p.clock = TempoClock.default;
)

b = ();

(
p = { |name|
	Ndef(name, {
		arg centerPos = #[0, 1],
		trigger = #[0.1, 50],
		rate = 1,
		dur = #[0.001, 2],
		pan = #[-1, 1],
		amp = #[0, 1],
		reverseProb = 0.5;

		var trigWhite, trigFreqMess, centerPosWhite, durWhite, panWhite, ampWhite, coin, reverse, snd;

		trigFreqMess = LFNoise2.kr(12).range(0.5, 1);
		trigWhite = Impulse.kr(LFNoise0.kr(trigFreqMess).range(trigger[0], trigger[1]));

		centerPosWhite = Dwhite(centerPos[0], centerPos[1]);
		durWhite = Dwhite(dur[0], dur[1]);
		panWhite = Dwhite(pan[0], pan[1]);
		ampWhite = Dwhite(amp[0], amp[1]);
		coin = CoinGate.kr(reverseProb, trigWhite);
		reverse = Select.kr(coin, [1, -1]);
		// reverse.poll(trig);

		Demand.kr(trigWhite, 0, [centerPosWhite, durWhite, panWhite, ampWhite]);

		TGrains.ar(
			numChannels: 2,
			trigger: trigWhite,
			bufnum: b[name].bufnum,
			rate: rate * reverse,
			centerPos: centerPosWhite * b[name].duration,
			dur: durWhite,
			pan: panWhite,
			amp: ampWhite)
	});

	ControlSpec.add(\centerPos, [0, 1], \lin, [0, b[name].numFrames]);
	ControlSpec.add(\trigger, [0.1, 50], \exp);
	ControlSpec.add(\rate, [0.05, 20], \exp);
	ControlSpec.add(\dur, [0.001, 2], \exp);
	ControlSpec.add(\pan, [-1, 1], \lin);
	ControlSpec.add(\amp, [0, 1], \lin);
	ControlSpec.add(\reverseProb, [0, 1], \lin);
};
)

(
l = { |name|
	Dialog.openPanel(
		okFunc: { |path|
			// Load sound into buffer
			b[name].free;
			b[name] = Buffer.readChannel(Server.local, path, channels: [0]);
		},
		cancelFunc: {"cancelled".postln;}
	);
};
)

ControlSpec.specs[\pan];

(
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
			loFrames / b[name].numFrames,
			hiFrames / b[name].numFrames,
		]);
	};
	soundFile = SoundFile.new();
	soundFile.openRead(b[name].path);
	soundFileView.soundfile_(soundFile);
	soundFileView.read(0, soundFile.numFrames);

	NdefGui.new(Ndef(name), 10, bounds: width@nDefHeight, parent: window);
};
)

(
a = { |name|
	l.value(name);
	p.value(name);
	g.value(name);
};
)

b['aaa'].duration;

b[\aaa];

l.value('aaa');
p.value('aaa');
g.value('aaa');
NdefGui(Ndef(\aaa), options: NdefGui.big);

(
Ndef(\dfm, { |freq = 1000, res = 0.1|
	DFM1.ar(Ndef('aaa').ar, freq, res)
});
)

Ndef(\dfm).gui;