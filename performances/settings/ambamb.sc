(
Ndef('eq', {
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
);
(
Ndef('s_eq', {
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
)
(
Ndef('s_eq').set('db2', 8.0952380952381, 'freq2', 698.46, 'rq3', 0.83298066476583, 'in', Ndef('s_fx'), 'rq2', 0.29935772947205, 'db1', -2.4867724867725, 'rq1', 0.5372281118324, 'db3', -2.4867724867725, 'hpfreq', 72.327056644883, 'freq3', 3545.7677267044, 'lpfreq', 18000.0, 'freq1', 481.34528033683);
);
(
Ndef('cloud')[0] = ({|gdur=0.1, buffer, density=1.0, rate=0.95, shape=0.5, ratespread=0.0, pos=0.25, posspread=0.0, rand=1.0, overlap=0.0|
	var cloud;
	var numGrains=4;
	var numBufChans=1;

	cloud = Array.fill(numGrains, {|gNum|
		var coef=gNum+1/numGrains;

		// Add tiny difference to each grain generator
		var weight = Rand(0.9999,1.0);

		var finalgdur = gdur * weight * overlap.linlin(0.0,1.0,1.0,4.0);

		// Deterministic
		var imp = Impulse.ar(density, phase: coef);

		// Random impulses
		var dust = Dust2.ar(density);

		// Crossfade between them
		var trig = XFade2.ar(imp, dust, rand.linlin(0.0,1.0,-1.0,1.0));

		// Grain envelope

		// Soft envelope
		var sineenv = EnvGen.ar(
			Env.sine, 
			trig, 
			timeScale: finalgdur
		);

		// Hard envelope
		var clickenv = EnvGen.ar(
			Env([0,1,1,0], [0,1,0]), 
			trig, 
			timeScale: finalgdur
		);

		// Faded
		var env = XFade2.ar(sineenv, clickenv, shape.linlin(0.0,1.0,-1.0,1.0));

		// Calculate position in buffer
		var position = (weight * pos + (posspread * coef)).wrap(0.0,1.0) * BufFrames.ir(buffer);

		// Calculate playback rate
		var playbackrate =  weight * rate * BufRateScale.ir(buffer); 
		var sig = PlayBuf.ar(
			numBufChans, 
			buffer, 
			ratespread * (gNum + 1) + 1 * playbackrate, 
			trig, 
			position,  
			loop: 0.0,  
			doneAction: 0
		);

		LeakDC.ar(env * sig)
	});

	// Normalize sound levels a bit
	cloud = cloud / numGrains;
	cloud = cloud;

	Splay.ar(cloud.flatten)
});
Ndef('cloud')[1] = (('filter' -> {|in, verbtime=10, room=5| 
	JPverb.ar(in, verbtime, 0, room);
}));

Ndef('cloud').set('verbtime', 18.555237052105, 'buffer', 3.1461000128084, 'shape', 0.0, 'amp', 1.0, 'rate', 0.98789092225003, 'ratespread', 0.0, 'pos', 0.28328924162257, 'posspread', 0.0, 'rand', 0.17989417989418, 'fadeTime', 10.02, 'density', 0.46984831311548, 'wet1', 0.35, 'overlap', 0.76190476190476, 'gdur', 0.23885714285714);
);
(
Ndef('bass', {
    arg freq = 110, amp = #[0, 1], timeScale = 1, prob = 0.5, mult = 0;

    var sig, ampsig, env;

    env = Env.new([0, 1, 0.5, 0], [0.5, 1, 2], curve: [\sin, \sin, -5]);

    //env = Env.perc(0.05, 3, curve: [5, 1]),
    ampsig = LFNoise1.kr(2).range(amp[0], amp[1]);
    ampsig = ampsig * EnvGen.kr(
        envelope: env,
        gate: Dust.kr(0.2 / timeScale),
        timeScale: timeScale
    );

    //sig = (SinOsc.ar(freq, mul: 0.8) + SinOsc.ar(freq*1.5, mul: 1)).dup * ampsig;
    sig = SinOsc.ar(freq, mul: 1) * ampsig;

    sig = Disintegrator.ar(sig, prob, mult);

    sig = LPF.ar(sig, 500).dup;

    sig
});
Ndef('bass').set('timeScale', 0.57796928841533, 'amp', [ 0.26056338028169, 0.90845070422535 ], 'freq', 77.327075043848, 'prob', 0.17989417989418, 'mult', -1.0);
);
(
Ndef('s', { | pan = 0, width = 1, pos = [ 0, 1 ], stretch = 50, stretchMultiplier = 1, noteShift = 0, wipe = 0, lpfreq = 18000, hpfreq = 30 | "open Function" });
)
(
Ndef('s').set('stretchMultiplier', 2.0364937915823, 'wipe', 0.22493887530562, 'pan', 0.0024449877750612, 'noteShift', 0.0, 'stretch', 30.315350350716, 'pos', [ 0.010915041448804, 0.039084055533311 ]);
);
(
Ndef('s_fx', {
    arg prob = 0.5, mult = 0, smoothing = #[0, 0], srate = #[1000, 48000], amp = #[0, 1],
        mix = 0.33, room = 0.5, damp = 0.5;

    var sig;

    sig = \in.ar([0, 0]);

    sig = sig * LFNoise0.kr(0.5).range(amp[0], amp[1]);

    sig = Disintegrator.ar(sig, prob, mult);

    sig = SmoothDecimator.ar(sig, 
        rate: LFNoise1.kr(0.5).range(srate[0], srate[1]),
        smoothing: LFNoise1.kr(0.5).range(smoothing[0], smoothing[1])
    );


    sig = FreeVerb.ar(sig, mix, room, damp);

    sig
});
)
(
Ndef('s_fx').set('smoothing', [ 0.74647887323944, 1.2535211267606 ], 'amp', [ 0.65492957746479, 0.90845070422535 ], 'room', 0.8994708994709, 'br_rq', 1.0529100529101, 'in', Ndef('s'), 'prob', 0.079365079365079, 'mix', 0.13227513227513, 'br_freq', 50.0, 'mult', -0.5978835978836, 'srate', [ 5420.7961450965, 7518.6488100597 ]);
);
(
Ndef('fm', {
    arg freq = 440, modfreq = #[110, 220], pmindex = #[1, 10], dur = 0.1, density = 1.0, varMult = 1.0, hpfreq = 30, lpfreq = 18000;

    var numSynths = 4,
        cloud;

    cloud = Array.fill(numSynths, { |i|
        var sig, trig, env, modphase,
            modfreqsig, pmindexsig, varsig;

        trig = Dust.kr(density);

        env = EnvGen.kr(
            envelope: Env.sine,
            gate: trig,
            timeScale: dur
        );
        
        varsig = density * varMult;

        pmindexsig = LFNoise1.kr(varsig).range(pmindex[0], pmindex[1]);
        modfreqsig = LFNoise0.kr(varsig).range(modfreq[0], modfreq[1]);

        modphase = LFNoise1.kr(0.1).range(0, 2*pi);

        sig = PMOsc.ar(
            carfreq: freq,
            modfreq: modfreqsig,
            pmindex: pmindexsig,
            modphase: modphase,
        );

        LeakDC.ar(sig * env)
    });

    cloud = LPF.ar(cloud, lpfreq);
    cloud = HPF.ar(cloud, hpfreq);

    Splay.ar(cloud.flatten)
});
Ndef('fm').set('dur', 0.18555237052105, 'varMult', 0.049935878934731, 'modfreq', [ 3647.7795591993, 3647.7795591993 ], 'hpfreq', 244.60901485734, 'density', 15.695212344913, 'freq', 26.79255449036, 'lpfreq', 13273.32072448, 'pmindex', [ 0.27775505513342, 22.86414309998 ]);
);
(
Ndef('master', {
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
Ndef('master').set('in1_amp', 0.98412698412698, 'in2_amp', 0.14285714285714, 'room', 0.91005291005291, 'damp', 0.0, 'in1', Ndef('s_eq'), 'in2', Ndef('bass'), 'hpfreq', 45.031022695685, 'mix', 0.12698412698413, 'lpfreq', 18000.0);
);
