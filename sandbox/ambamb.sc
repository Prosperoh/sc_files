z = NdefMixer(s)


Env.new([0, 1, 1, 0], [0.1, 1, 0.1], curve: [\sin, \sin, \sin]).plot;


// TO BE CONTINUED
// try to divide the spectrum in two (below, above threshold
// frequency) and apply different effects to the two

Ndef(\bass).clear;


Ndef('s').clear;

// use piano-chord-explosion
l.value('s');
e.sb['pstretch'].value('s');
h.value('s');

Ndef(\s_fx).clear;

(
Ndef(\s_fx, {
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
ControlSpec.add(\prob, [0, 1, \lin]);
ControlSpec.add(\mult, [-1, 1, \lin]);
ControlSpec.add(\smoothing, [0, 2, \lin]);
ControlSpec.add(\srate, [1000, 48000, \exp]);
)

Ndef(\bass).set(\note, 0);

Ndef(\noise)

Env.perc(1, 3, curve: [5, 1]).plot;
Env.new([0, 1, 0.5, 0], [0.5, 1, 0.5], curve: [\sin, \sin, -5]).plot;

(
Ndef(\bass, {
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
ControlSpec.add(\timeScale, [0.01, 10, \exp]);
)

Ndef(\bass).clear;

Ndef(\s_fx_smoothing).gui;

Ndef(\s_fx).clear;


Ndef(\eq).copy(\s_eq) 



(
SynthDef("help-freeze", { arg out=0, bufnum=0;
    var in, chain, freeze;
    in = SinOsc.ar(LFNoise1.kr(5.2,250,400));
    chain = FFT(bufnum, in);
    freeze = MouseX.kr(-1, 1);
    // uncomment the line below to compare while ...
    //chain = PV_MagFreeze(chain, freeze);
    // commenting the line below
    chain = PV_Freeze(chain, freeze);
    Out.ar(out, 0.15 * IFFT(chain).dup);
}).add;
)

(
var n = 13;
e.buffers['buf_fft'].free;
e.buffers['buf_fft'] = Buffer.alloc(s, 2**n);
e.buffers['buf_fft'].numFrames;
)

x = Synth("help-freeze", [\out, 0, \bufnum, e.buffers['buf_fft'].bufnum]);
x.free;

s.freeAll;



(
Ndef(\cloud, {|gdur=0.1, buffer, density=1.0, rate=0.95, shape=0.5, ratespread=0.0, pos=0.25, posspread=0.0, rand=1.0, overlap=0.0|
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
}).set(\wet1, 0.35).play;

Ndef(\cloud)[1] = \filter -> {|in, verbtime=10, room=5| 
	JPverb.ar(in, verbtime, 0, room);
};

ControlSpec.add(\pos, [0.0, 1.0, \lin]);
ControlSpec.add(\density, [0.01, 100.0, \exp]);
ControlSpec.add(\rate, [0.1, 10.0, \exp]);
ControlSpec.add(\shape, [0.0, 1.0, \lin]);
ControlSpec.add(\ratespread, [0.0, 1.0, \lin]);
ControlSpec.add(\posspread, [0.0, 1.0, \lin]);
ControlSpec.add(\rand, [0.0, 1.0, \lin]);
ControlSpec.add(\overlap, [0.0, 1.0, \lin]);
ControlSpec.add(\gdur, [0.001, 1.0, \lin]);
ControlSpec.add(\wet1, [0.0, 1.0, \lin]);
)

(
Ndef(\fm, {
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
ControlSpec.add(\modfreq, [10, 40000, \exp]);
ControlSpec.add(\pmindex, [0.1, 10000, \exp]);
ControlSpec.add(\varMult, [0.01, 100, \exp]);
)

Ndef(\fm).clear;

l.value('cloud');
Ndef(\cloud).set(\buffer, e.buffers['cloud'].bufnum);

Ndef(\cloud).gui;
