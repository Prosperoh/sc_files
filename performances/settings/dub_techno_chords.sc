(
Ndef('conv', { 
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
    });
Ndef('conv').set('er', 5.0242092902213, 'lpfRefl', 0.66666, 'hpfRefl', 57.722808828602, 'in', Ndef('fx_chain'), 'tail', 0.28601549535719, 'dry', 1.0428544107986);
);
(
Ndef('Aoise_balancer');
Ndef('Aoise_balancer').set('in2', Ndef('pn_eq'));
);
(
Ndef('sing', {
    arg bufnum, pos = #[0, 1], sgate = 0, rate = 1;

    var bufFrames, startLoop, endLoop;

    bufFrames = BufFrames.kr(bufnum);
    startLoop = (bufFrames * pos[0]).asInteger;
    endLoop = (bufFrames * pos[1]).asInteger;

    LoopBuf.ar(
        numChannels: 1,
        bufnum: bufnum, 
        rate: rate * BufRateScale.kr(bufnum), 
        gate: sgate, 
        startPos: startLoop,
        startLoop: startLoop,
        endLoop: endLoop
    ).dup
});
Ndef('sing').set('sgate', 0.4973544973545, 'rate', 0.98427452176083, 'bufnum', 13.0, 'pos', [ 0.47887323943662, 0.70140845070423 ]);
);
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
Ndef('fx_chain', {
    arg in, bpm = 120,
        delayLeft = 0.25, delayRight = 0.25, feedback = 0.1;

    var sig,
        delay = [delayLeft, delayRight],
        delaytime = bpm * delay / 60.0,
        minFeedback = (1/20),
        decaytime = delaytime * 0.001.log() / feedback.log();

    sig = In.ar(in, 2);

    sig = sig.collect({ |msig, i|

        msig + CombL.ar(msig,
            maxdelaytime: decaytime[i],
            delaytime: delaytime[i],
            decaytime: decaytime[i],
        )
    });

    sig = sig.pow([0.79, 0.81]).tanh();
});
Ndef('fx_chain').set('delayRight', (0.625 * { "open Function" }), 'bpm', 85, 'in', Bus('audio', 12, 2, s), 'feedback', 0.6, 'delayLeft', (0.375 * { "open Function" }));
);
(
Ndef('asmr', { | centerPos = [ 0, 1 ], trigger = [ 0.1, 50 ], rate = [ 1, 1 ], grainDur = [ 0.001, 2 ], pan = [ -1, 1 ], amp = [ 0, 1 ], reverseProb = 0.5, lpfreq = 18000, hpfreq = 30 | "open Function" });
Ndef('asmr').set('trigger', [ 3.2232002852703, 47.218058498054 ], 'hpfreq', 2213.4583884628, 'fadeTime', 5, 'grainDur', [ 0.018907818936129, 0.84558336894883 ], 'rate', [ 0.56029869906534, 1.8756181167755 ], 'lpfreq', 8766.2811728442, 'reverseProb', 0.87041564792176);
);
(
Ndef('pinknoise', {
    var sig;
    
    sig = PinkNoise.ar();
    sig = sig.pow(0.7).tanh();
    sig = sig * LFNoise1.kr(0.5!2).range(0.5, 1) * 0.2;
    sig = sig.pow(0.9).tanh();

    sig = BRF.ar(sig,
        freq: LFNoise0.kr(0.5).range(400, 8000).lag(2),
        rq: LFNoise0.kr(0.5!2).range(0.1, 0.5).lag(2)
    );

    LeakDC.ar(sig)
});
);
(
Ndef('noise_eq', {
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
Ndef('noise_eq').set('hpfreq', 253.02979959052, 'in', Ndef('noise'), 'lpfreq', 3794.0994359507);
);
(
Ndef('pn_eq', {
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
Ndef('pn_eq').set('rq1', 1.1434075569414, 'hpfreq', 1741.914404547, 'in', Ndef('pinknoise'));
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
Ndef('master').set('hpfreq', 30.0, 'room', 0.084656084656085, 'mix', 0.34391534391534, 'damp', 0.62962962962963, 'in1', Ndef('conv'), 'lpfreq', 18000.0);
Ndef('master').play;
);
Ndef('noise', { | pan = 0, width = 1, pos = [ 0, 1 ], stretch = 50, stretchMultiplier = 1, noteShift = 0, wipe = 0, lpfreq = 18000, hpfreq = 30 | "open Function" });
(
Ndef('noise_balancer', {
    var ins, sigs, period;
    
    period = \period.kr(10);

    ins = [
        \in1.ar([0, 0]) * \amp1.kr(1),
        \in2.ar([0, 0]) * \amp2.kr(1),
        \in3.ar([0, 0]) * \amp3.kr(1),
        \in4.ar([0, 0]) * \amp4.kr(1),
    ];

    sigs = ins.collect({ |item|
        EnvGen.kr(
            Env.perc(
                period * Rand(0.3, 0.5),
                period * Rand(0.5, 0.7),
            ),
            gate: Dust.kr(Rand(0.8, 1.2) / period),
        ).range(\low.kr(0), 1) * item
    });

    LeakDC.ar(Mix(sigs))
});
Ndef('noise_balancer').set('amp4', 2.2222222222222, 'amp1', 0.36840314986404, 'in1', Ndef('noise_eq'), 'amp3', 0.88888888888889, 'in2', Ndef('pn_eq'), 'in3', Ndef('asmr'), 'in4', Ndef('sing'), 'amp2', 0.24787311240906);
Ndef('noise_balancer').play(
	out: 12
);
);
