e.sb = ();

(
e.sb.wt = Array.fill(4, {
    var numSegs = rrand(4, 20);
    Env(
        [0]
        ++ (({ rrand(0.0, 1.0) } ! (numSegs - 1)) * [-1, 1]).scramble 
        ++ [0],
        { exprand(1, 20) } ! numSegs,
        { rrand(-20, 20) } ! numSegs
    ).asSignal(1024).asWavetable;
});
)


e.sb.buf = Buffer.allocConsecutive(4, s, 2048);

(
e.sb.buf.do({
    arg buf, i;
    buf.loadCollection(e.sb.wt[i]);
});
)

e.sb.wt[2].plot;

(
SynthDef(\vosc, {
    arg out=0, buf=0, numBufs=2, freq=440, amp=1,
    atk=3, rel=3, cAtk=5, cRel=(-5);
    var sig, bufpos, detuneSig, env;

    env = EnvGen.kr(
        Env.new(
            [0, 1, 0],
            times: [atk, rel],
            curve: [cAtk, cRel]
        ),
        doneAction: Done.freeSelf
    );

    detuneSig = LFNoise1.kr(0.2!8).bipolar(0.2).midiratio;
    bufpos = buf + LFNoise1.kr(0.5).range(0, numBufs-1);
    sig = VOsc.ar(bufpos, freq * detuneSig);
    sig = Splay.ar(sig);
    sig = LeakDC.ar(sig) * env * amp * 0.2;
    Out.ar(out, sig);
}).add;
)

(
Pdef(\vosc, Pbind(
    \instrument, \vosc,
    \dur, 10,
    \numBufs, 4,
    \scale, Scale.harmonicMajor,
    \degree, Pseq([[0, 2, 4, 7]], inf),
    \ctranspose, -24,
    \amp, 0.5,
    \atk, Pkey(\dur) * 0.5,
    \rel, Pkey(\dur) * 0.5,
));
)

Pdef(\vosc).play;
Pdef(\vosc).stop;

s.scope;

Synth(\vosc, [\buf, e.sb.buf[0].bufnum, \numBufs, 4]);
s.freeAll;


e.sb.bufshaper.free;
e.sb.bufshaper = Buffer.alloc(s, 2048);

(
var base, zeroCentered;
base = {
    arg numSegs = 5;
    Env(
        [-1]
        ++ (({ rrand(-1.0, 1.0) } ! (numSegs - 1)) * [-1, 1]).scramble
        ++ [1],
        { exprand(1, 20) } ! numSegs,
        { rrand(-20, 20) } ! numSegs
    )
}.value(rrand(4, 20)).asSignal(1025);

zeroCentered = Env(
    [1, 0, 1],
    [1, 1],
    [4, -4]
).asSignal(1025);

e.sb.tf = base * zeroCentered;
// reset
//e.sb.tf = Env([-1, 1], [1]).asSignal(1025);
e.sb.tf.plot;

e.sb.bufshaper.loadCollection(e.sb.tf.asWavetableNoWrap);
)

e.sb.bufshaper.free;
e.sb.bufshaper.plot;



(
SynthDef(\shaper, {
    arg out=0, tfBuf=0, numBufs=2, freq=60, amp=1,
    atk=0.1, rel=3, cAtk=5, cRel=(-5);
    var sig, bufpos, detuneSig, env;

    env = EnvGen.kr(
        Env.new(
            [0, 1, 0],
            times: [atk, rel],
            curve: [cAtk, cRel]
        ),
        doneAction: Done.freeSelf
    );

    detuneSig = LFNoise1.kr(0.2!8).bipolar(0.2).midiratio;
    sig = SinOsc.ar(freq * detuneSig);
    sig = Shaper.ar(tfBuf, sig);
    sig = Splay.ar(sig);
    sig = LeakDC.ar(sig) * env * amp * 0.2;
    Out.ar(out, sig);
}).add;
)

(
Synth(\shaper, [
    \amp, 0.3,
    \tfBuf, e.sb.bufshaper,
    \freq, 440,
]);
)


