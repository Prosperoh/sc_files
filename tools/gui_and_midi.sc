MIDIClient.init;

MIDIIn.connectAll;

~initWavetables.value;
~initSynthDefs.value;
~initAll.value;
~initPatterns.value;

(
// ----------------------------------------------------------------
// Synthdefs
// ----------------------------------------------------------------
~initSynthDefs = {

	SynthDef.new(\shaper, {
		arg freq=55, out=0, buf=0, amp=1,
		atk=0.01, dl=0.01, sus=1, rel=0.1, gate=1;
		var in, sig, envGen;

		envGen = EnvGen.kr(
			envelope: Env.adsr(atk, dl, sus, rel),
			gate: gate,
			doneAction: 2
		);

		in = SinOsc.ar({freq * Rand(-0.1, 0.1).midiratio}!8);
		in = in * LFNoise1.kr(0.5!8).range(0.1, 1);

		sig = Shaper.ar(buf, in) * envGen;
		sig = Splay.ar(sig) * amp;
		Out.ar(out, sig);
	}).add;

	SynthDef.new(\vosc, {
		arg out=0, buf=0, numBufs=2, freq=50, bufFreq=1, amp=1,
		atk=0.01, dl=0.01, sus=1, rel=0.1, gate=1;
		var sig, bufpos, detuneSig, envGen;
		envGen = EnvGen.kr(
			envelope: Env.adsr(atk, dl, sus, rel),
			gate: gate,
			doneAction: 2
		);
		detuneSig = LFNoise1.kr(0.2!4).bipolar(0.2).midiratio;
		bufpos = buf + SinOsc.kr(bufFreq).range(0, numBufs - 1);
		sig = VOsc.ar(bufpos, freq * detuneSig) * envGen;
		sig = Splay.ar(sig);
		sig = LeakDC.ar(sig) * amp * 0.2;
		Out.ar(out, sig);
	}).add;

	SynthDef.new(\oscDust, {
		arg buf=0, freq=220, detune=0.2,
		amp=1, pan=0, out=0,
		ffreq = 440, rq = 1,
		density=1, atk=0.01, rel=0.1;
		var sig, envGen = nil, detuneCtrl, ampFactor = 0.4;

		envGen = EnvGen.kr(
			envelope: Env.perc(attackTime: atk, releaseTime: rel),
			gate: Dust.kr(density),
		);

		// array of eight Oscs with uniquely detune frequencies
		// and unique initial phase offsets
		detuneCtrl = LFNoise1.kr(0.1!8).bipolar(detune).midiratio;
		sig = Osc.ar(buf, freq * detuneCtrl, {Rand(0, 2pi)}!8);
		sig = BPF.ar(sig, ffreq, rq, 1 / rq.sqrt);
		sig = sig * envGen;

		sig = Splay.ar(sig); //spread 8 signals over stereo field
		sig = LeakDC.ar(sig); //remove DC bias
		sig = Balance2.ar(sig[0], sig[1], pan, amp * ampFactor); //L/R balance (pan)*/

		Out.ar(out, sig);
	}).add;

	SynthDef.new(\oscSust, {
		arg buf=0, freq=220, detune=0.2,
		amp=1, pan=0, out=0,
		ffreq = 440, res = 0.5,
		atk=0.01, dl=0.01, sus=1, rel=0.1, gate=1;
		var sig, envGen = nil, detuneCtrl, ampFactor = 0.4;

		envGen = EnvGen.kr(
			envelope: Env.adsr(atk, dl, sus, rel),
			gate: gate,
			doneAction: 2
		);

		// array of eight Oscs with uniquely detune frequencies
		// and unique initial phase offsets
		detuneCtrl = LFNoise1.kr(0.1!8).bipolar(detune).midiratio;
		sig = Osc.ar(buf, freq * detuneCtrl, {Rand(0, 2pi)}!8);
		sig = sig * envGen;

		sig = Splay.ar(sig); //spread 8 signals over stereo field
		sig = LeakDC.ar(sig); //remove DC bias
		sig = Balance2.ar(sig[0], sig[1], pan, ampFactor); //L/R balance (pan)*/

		sig = DFM1.ar(sig,
			freq: ffreq * (1 + LFNoise1.kr(0.1, 0.01)),
			res: SinOsc.kr(0.05).range(res * 0.9, res * 1.1),
			inputgain: 1,
			type: 0,
			noiselevel: 0.002,
			mul: 0.5,
		);

		Out.ar(out, sig * amp);
	}).add;

	SynthDef.new(\pad, {
		arg out=0, freq=40, ffreq = 440, res=1, amp=1;
		var sig = 0;

		sig = DFM1.ar(
			SawDPW.ar([freq,freq*1.01],
				0,
				0.1),
			ffreq * (1 + LFNoise1.kr(0.01)),
			SinOsc.kr(0.05).range(res * 0.9, res * 1.1),
			1,
			0,
			0.002,
			0.08);

		Out.ar(out, sig * amp.lag(0.2));
	}).add;

	SynthDef.new(\reverb, {
		arg in, out = 0, amp = 1;
		var sig = In.ar(in, 2);
		sig = GVerb.ar(sig, mul: amp);
		Out.ar(out, sig);
	}).add;

	SynthDef.new(\dfm, {
		arg in, out = 0, ffreq = 440, res = 1;
		var sig = In.ar(in, 2);
		sig = DFM1.ar(sig,
			freq: ffreq * (1 + LFNoise1.kr(0.1, 0.01)),
			res: SinOsc.kr(0.05).range(res * 0.9, res * 1.1),
			inputgain: 1,
			type: 0,
			noiselevel: 0.002,
			mul: 0.5,
		);
		Out.ar(out, sig);
	}).add;

	SynthDef.new(\ampSynth, {
		arg in, out = 0, amp = 1;
		var sig = amp * In.ar(in, 2);
		sig = Limiter.ar(sig);
		Out.ar(out, sig);
	}).add;
};

// ----------------------------------------------------------------
// Patterns
// ----------------------------------------------------------------
~initPatterns = {
	/*var p = Pcollect(
		{ |a| a.collect({ arg n, i;
			if (i > 0,
				{n + [-12, 0, 12].choose },
				{n} )!10;
		})
		},
		Pseq([[-12, 2, 4, 6]], inf));*/

	var p = Pseq([[0, 3, 4, 6], [-2, 5, 7, 8]], inf);

	Pbindef(\pt,
		\instrument, \oscSust,
		\dur, 5,
		\legato, 1,
		\scale, Scale.major,
		\degree, p,
		\atk, 0.1,
		\rel, 1,
		\sus, 1,
	);
};


// ----------------------------------------------------------------
// Wavetables
// ----------------------------------------------------------------
~initWavetables = {
	var wtLength = 1024;
	var nMultWt = 8;
	var complexity = { exprand(5, 10).round }; // number between 1 and 10

	var generateEnv = { arg cplx = 3;
		var numSegs = cplx.linexp(1, 10, 3, 30).round;

		Env(
			// random segments
			levels: [0] ++ ({1.0.rand}.dup(numSegs - 1) * [1, -1]).scramble ++ [0],

			// controlled random durations
			times: {exprand(1, cplx.linexp(1, 10, 1, 50))}.dup(numSegs),

			// low complexity -> sine, high complexity -> sharper
			curve: {[\sine, 0, exprand(1, 20) * [-1, 1].choose]
				.wchoose([10 - cplx, 3, cplx].normalizeSum)}.dup(numSegs),
		);
	};

	~wt_sig.free;
	~wt_sig = generateEnv.value(complexity.value()).asSignal(wtLength);
	~wt_sig.plot;

	~wt_mult.free;
	~wt_mult = nMultWt.collect({
		generateEnv.value(complexity.value()).asSignal(wtLength);
	});
	~wt_mult_buf.free;
	~wt_mult_buf = Buffer.allocConsecutive(nMultWt, s, wtLength*2);
	~wt_mult_buf.do({ arg buf, i;
		buf.loadCollection(~wt_mult[i].asWavetable);
	});

	~wt_buf.free;
	~wt_buf = Buffer.alloc(s, wtLength, 1, {
		arg buf;
		buf.setnMsg(0, ~wt_sig.asWavetable);
	});

	~tf = Env([-1, 1], [1], [0]).asSignal(wtLength + 1);
	~tf = ~tf +
		Signal.sineFill(
			wtLength + 1,
			(0!3) ++ [0, 0, 0, 1, 1, 1].scramble,
			{rrand(0, 2pi)}!9
		) / 12;
	~tf = ~tf.normalize;
	~tf.plot;

	~tf_buf.free;
	~tf_buf = Buffer.alloc(s, wtLength * 2);
	~tf_buf.loadCollection(~tf.asWavetableNoWrap);
	~tf_buf.plot;
};

~initBuses = {
	~dfm1Bus = Bus.audio(s, 2);
	~reverbBus = Bus.audio(s, 2);

	~trackBuses = 8.collect({
		Bus.audio(s, 2);
	});
};

~initGroups = {
	~groups = 8.collect({
		Group.new(addAction:\addToHead);
	});
};

~initRunningSynths = {

	~dfm1 = Synth.new(\dfm, [
		\in, ~dfm1Bus,
		\out, ~trackBuses[1],
	],
	target: ~groups[1],
	);

	~busSynths = 8.collect({ arg i;
		Synth.new(\ampSynth, [
			\in, ~trackBuses[i],
			\out, ~reverb_bus,
		],
		addAction:\addToTail
		);
	});

	Synth.new(\reverb, [
		\in, ~reverbBus,
		\out, 0,
	],
	addAction: \addToTail
	);
};

~initGUI = {

	var n = 8;
	var margin = 20;
	var unitWidth = 100;

	// ----------------------------------------------------------------
	// Window
	// ----------------------------------------------------------------
	Window.closeAll;
	w = Window("My great GUI", Rect(700, 50, (n * unitWidth)  , 400))
	.front
	.alwaysOnTop_(false);

	w.view.decorator_(FlowLayout(w.bounds, margin@margin, 10@10));

	c = Array.fill(n, {
		arg view;
		view = CompositeView(w, 80@(w.bounds.height - (2*margin)))
		.background_(Color.rand);

		view.decorator_(FlowLayout(view.bounds, 5@5, 5@5));
	});

	n.do({ |i|
		3.do({Knob(c[i], 40@40);});
		2.do({
			Button(c[i], 40@20)
			.states_([
				["", Color.black, Color.grey],
				["", Color.white, Color.new(1, 0.5, 0.5)]
			]);
		});
		Slider(c[i], 40@150);
	});

	// ----------------------------------------------------------------
	// Midi -> GUI mappings
	// ----------------------------------------------------------------
	~ccGUIMapping.free;
	~ccGUIMapping = Array.newClear(100);
	~noteOnGUIMapping.free;
	~noteOnGUIMapping = Array.newClear(50);

	// First 4 columns
	4.do({ arg i;
		// Knobs
		3.do({ arg j;
			~ccGUIMapping[16 + (i * 4) + j] = c[i].children[j];
		});

		// Slider
		~ccGUIMapping[16 + (i * 4) + 3] = c[i].children[5];
	});

	// Last 4 columns
	4.do({ arg i;
		// Knobs
		3.do({ arg j;
			~ccGUIMapping[46 + (i * 4) + j] = c[i + 4].children[j];
		});

		// Slider
		~ccGUIMapping[46 + (i * 4) + 3] = c[i + 4].children[5];
	});

	// Buttons
	8.do({ arg i;
		~noteOnGUIMapping[(i * 3) + 1] = c[i].children[3];
		~noteOnGUIMapping[(i * 3) + 3] = c[i].children[4];
	});

	// ----------------------------------------------------------------
	// Midi events
	// ----------------------------------------------------------------
	MIDIdef.cc(\cc, {
		arg val, num, chan, src;
		{ if (~ccGUIMapping[num] != nil,
			{~ccGUIMapping[num].valueAction_(val / 127.0);}
		)}.defer;
	});

	MIDIdef.noteOn(\noteOn, {
		arg val, num, chan, src;
		{
			if (~noteOnGUIMapping[num] != nil,
				{ var obj = ~noteOnGUIMapping[num];
					if (obj.value == 1, {obj.valueAction_(0);}, {obj.valueAction_(1);});
				}
			);
		}.defer;
	});

	// ----------------------------------------------------------------
	// Client-side arrays
	// ----------------------------------------------------------------
	~knobs = Array.newClear(8);
	~sliders = Array.newClear(8);
	~buttons = Array.newClear(8);

	8.do({ arg i;
		~knobs[i] = Array.newClear(3);
		~knobs[i][0] = c[i].children[2];
		~knobs[i][1] = c[i].children[1];
		~knobs[i][2] = c[i].children[0];

		~buttons[i] = Array.newClear(2);
		~buttons[i][0] = c[i].children[4];
		~buttons[i][1] = c[i].children[3];

		~sliders[i] = c[i].children[5];
	});

	// ----------------------------------------------------------------
	// Some controls config
	// ----------------------------------------------------------------
	~buttons[0][0].action_({
		arg obj;
		if (obj.value == 1,
			{
				[36, 39 + 12, 43 - 12].do({ arg note, i;
					var synth = Synth.new(\vosc, [
						\buf, ~wt_mult_buf[0].bufnum,
						\numBufs, ~wt_mult_buf.size,
						\out, ~trackBuses[0],
						\freq, note.midicps,
					],
					target: ~groups[0],
					);
					synth.register;
				});
			},
			{~groups[0].freeAll;}
		);
	});

	~buttons[1][0].action_({
		arg obj;
		if (obj.value == 1,
			{ Pbindef(\pt,
				\out, ~dfm1Bus,
			).play; },
			{ Pbindef(\pt).stop; }
		);
	});

	~buttons[2][0].action_({
		arg obj;
		var note = 36;
		if (obj.value == 1,
			{
				var density = 1;
				Synth.new(\oscDust, [
					\buf, ~wt_buf.bufnum,
					\out, ~trackBuses[2],
					\freq, note.midicps,
					\ffreq, 440,
					\rq, 0.5,
					\density, density,
				],
				target: ~groups[2],
				).register;
			},
			{ ~groups[2].freeAll; }
		);
	});

	~buttons[3][0].action_({
		arg obj;
		var note = 60;
		if (obj.value == 1,
			{
				var density = 1;
				Synth.new(\shaper, [
					\buf, ~tf_buf,
					\out, ~trackBuses[3],
					\freq, note.midicps,
				],
				target: ~groups[3],
				).register;
			},
			{ ~groups[3].freeAll; }
		);
	});

	~knobs[0][0].action_({ arg obj;
		var bufFreq = obj.value.linexp(0, 1, 0.5, 10);
		~groups[0].set(\bufFreq, bufFreq);
	});

	~knobs[1][0].action_({
		arg obj;
		var ffreq = obj.value.linexp(0, 1, 100, 10000);
		~groups[1].set(\ffreq, ffreq);
	});

	~knobs[1][1].action_({
		arg obj;
		var res = obj.value.linexp(0, 1, 0.4, 2);
		~groups[1].set(\res, res);
	});

	~knobs[2][0].action_({ arg obj;
		var density = obj.value.linexp(0, 1, 0.5, 10);
		~groups[2].set(\density, density);
	});

	~knobs[2][1].action_({ arg obj;
		var freq = obj.value.linexp(0, 1, 100, 10000);
		~groups[2].set(\freq, freq);
	});

	~knobs[2][2].action_({ arg obj;
		var ffreq = obj.value.linexp(0, 1, 100, 10000);
		~groups[2].set(\ffreq, ffreq);
	});

	~knobs[3][2].action_({ arg obj;
		var rq = obj.value.linexp(0, 1, 2, 0.01);
		~groups[2].set(\rq, rq);
	});

	8.do({ arg i;
		~sliders[i].action_({
			arg obj;
			var amp = obj.value;
			~busSynths[i].set(\amp, amp);
		});
	});
};

~initAll = {
	~initSynthDefs.value;
	~initWavetables.value;
	~initBuses.value;
	~initGroups.value;
	~initRunningSynths.value;
	~initPatterns.value;
	~initGUI.value;
};

)

~initAll.value;

(
~chords = [
	[36, 40 + 12, 43 - 12],
	[33 + 12, 36, 40 - 12]
];

~t = Task({
	loop {
		~chords.do({ arg chord;
			chord.do({ arg note, i;
				~pad_synths[i].set(\freq, note.midicps);
			});
			2.wait;
		});
	};
});
)

~t.play;
~t.stop;

~initWavetables;



