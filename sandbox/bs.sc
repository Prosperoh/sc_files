s.boot;
s.quit;

z = NdefMixer(s);


// moué pas tip top

// wavetables
Buffer.freeAll;
(
var numSegs = 8;
~wt_buffer.do({ |buf| buf.free; });
~wt_buffer = 4.collect({ |i|
    var env, sig, buffer;
    
    env = Env.new(
        [0] ++ (({rrand(0.0, 1.0)}!(numSegs - 1)) * [1, -1]).scramble ++ [0],
        {exprand(1, 20)}!numSegs,
        {rrand(-20, 20)}!numSegs
    );

    sig = env.asSignal(1024);
    buffer = Buffer.loadCollection(s, sig.asWavetable);
    buffer
});
)

~wt_buffer[0].plot;

(
Ndef(\synth).clear;
"hello".postln;
)

Buffer.freeAll;




(
Ndef(\synth, {
    arg velocity = 1, freeze = 1.0, fLow = 200.0, fHigh = 5000.0;

    var sig, nSigs = 4;

    velocity = LFNoise1.kr(velocity).range(velocity * 0.2, velocity * 5.0);

    sig = nSigs.collect({
        var freq, freqOsc, phase, amp, sig_, bufpos, midinote;

        bufpos = LFNoise1.kr(velocity)
            .range(~wt_buffer[0].bufnum, ~wt_buffer[0].bufnum + 3);

        freqOsc = LFNoise0.kr(velocity / 20.0);

        freq = LFNoise1.kr(velocity).range(
            fLow, //freqOsc.range(fLow / 2.0, fLow * 2.0),
            fHigh, //freqOsc.range(fHigh / 2.0, fHigh * 2.0)
        );

        // convert to chromatic scale
        //midinote = (69 + (12 * (freq / 440).log2));
        //midinote = midinote.round;
        //freq = (440 * 2.pow((midinote - 69) / 12)).poll;

        phase = LFNoise0.kr(velocity).range(0, pi);
        amp = LFNoise1.kr(velocity / 10.0).range(0, 1);

        sig_ = VOsc.ar(
            bufpos,
            freq,
            phase,
            amp,
        );

        sig_ = sig_.pow(0.8);

        sig_ = BRF.ar(sig_,
            freqOsc.range(500, 10000),
            0.5);
        
        sig_

    });

    sig = Splay.ar(sig);

    freeze = (LFNoise2.kr(velocity).range(0, 1).pow(freeze.reciprocal) * 2 - 1); 
    sig = sig.collect({ |sig_|
        var chain_;
        chain_ = FFT(LocalBuf(2048), sig_);
        chain_ = PV_Freeze(chain_, freeze);
        IFFT(chain_)
    });

    sig = LeakDC.ar(sig);


    sig
});
)

Ndef(\conv).gui;

StageLimiter.activate;

Ndef(\synth).play;
ControlSpec.add(\velocity, [0.1, 500.0, \exp]);
Ndef(\synth).gui;






