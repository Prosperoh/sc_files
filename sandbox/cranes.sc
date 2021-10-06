z = NdefMixer(s)
s.makeGui;
s.plotTree;
s.meter;

(
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
//m.gui;
)



// trying some GUI stuff, let's try to remake cranes:
// 1- record something from input bus, put it into a left and a right buffer
// 2- loop it
// 3- make some options to reposition the looping

// buffer
b.free;

(
var dur = 10;
b = Buffer.alloc(s, s.sampleRate * dur, 1);
)

// SynthDefs
(
SynthDef(\sine, {
    arg out = 0, amp = 0.2, freq = 440,
        atk = 0.01, rel = 0.5;

    var sig, env;

    env = EnvGen.kr(
        Env.perc(atk, rel),
        doneAction: 2,
    );

    sig = SinOsc.ar(freq);

    sig = sig * env * amp;

    sig = Pan2.ar(sig);

    Out.ar(out, sig);
}).add;

SynthDef(\playIn, {
    arg in, out = 0, pan = 0;

    var sig;

    sig = In.ar(in, 1);
    sig = Pan2.ar(sig, pan);

    Out.ar(out, sig);
}).add;

SynthDef(\recorder, {
    arg in, buf, out=0;
    var input;

    input = In.ar(in, 1);
    
    RecordBuf.ar(
        inputArray: input,
        bufnum: buf,
        loop: 0.0,
    );
}).add;

SynthDef(\playback, {
    arg out = 0, buf, rate = 1, gate = 1,
        start = 0.0, end = 1.0;

    var sig;

    sig = LoopBuf.ar(
        numChannels: 1,
        bufnum: buf,
        rate: rate * BufRateScale.kr(buf),
        gate: gate,
        startPos: start * BufSampleRate.kr(buf),
        startLoop: start * BufSampleRate.kr(buf),
        endLoop: end * BufSampleRate.kr(buf),
        interpolation: 2);

    // TODO: different loopers for left and right
    sig = Pan2.ar(sig);

    Out.ar(out, sig);
}).add;
)

~recordBus = Bus.audio(s, 2);
r = Synth(\playIn, [\in, ~recordBus, \out, 0]);
r.free;

/*
(
Pdef(\sine, Pbind(
    \instrument, \sine,
    \dur, 1,
    \out, ~recordBus,
)).stop;
)
*/


// a- MIDI
(
~getButtonElAt = { arg cat, i, j;
    switch (cat,
        \bt, { m.elAt(\bt, i.asSymbol, j.asSymbol) },
        \solo, { m.elAt(\solo) },
        \bankRight, { m.elAt(\bankRight) },
        \bankLeft, { m.elAt(\bankLeft) },
        { "unknown button category".warn; })
};

~getLightElAt = { arg cat, i, j;
    ~getButtonElAt.value(cat, i, j).elAt('on')
};

~lightOnAt = { arg button, i = -1, j = -1;
    ~getLightElAt.value(button, i, j).value_(1);
};

~lightOffAt = { arg button, i = -1, j = -1;
    ~getLightElAt.value(button, i, j).value_(0);
};
)

(
m.elAt(\bt, '2', '1', 'on').action_({ |el|
    ~lightOn.value(\bt, '2', '1');
    x = Synth(\recorder, [
        \buf, b,
        \in, s.inputBus,    // for recording standard input
    ]);
});

m.elAt(\bt, '2', '1', 'off').action_({ |el|
    ~lightOff.value(\bt, '2', '1');
    x.free;
    // TODO: should not interfere with the audio engine
    Routine { b.plot; }.play(AppClock);
});
)


~cranesSynth = Synth(\playback, [\out, 0, \buf, b, \start, 0.0, \end, 20.0]);
~cranesSynth.free;




// b- GUI
~netAddr = NetAddr("127.0.0.1", NetAddr.langPort); // local machine


(
~getSoundFilePathForBuf = { |bufnum| 
    ("./tmp/bufnum_" ++ bufnum.asSymbol ++ ".aiff").standardizePath
};

~cursorPos = 0;

~loadWindow = {
    arg msg;

    var window, soundFile, soundFileView, flowLayout,
        windowWidth, windowHeight,
        width = 600,
        soundFileHeight = 200,
        midiHeight = 200,
        margin = 20,
        gap = 5,
        bufnum, soundFilePath;

    bufnum = msg[1]; // msg[0] is the command name
    soundFilePath = ~getSoundFilePathForBuf.value(bufnum);

    soundFilePath.postln;

    // prepare the window
    windowWidth = width + (2 * margin);
    windowHeight = (soundFileHeight + midiHeight) + gap + (2 * margin);

    window = Window.new(
        "cranes mimic",
        Rect(50, 50, windowWidth, windowHeight),
        false
    ).front;
    window.background = Color.grey(0.9, 0.9);

    flowLayout = window.addFlowLayout(margin@margin, gap@gap);

    // create the sound file view
    soundFileView = SoundFileView.new(
        window,
        Rect(0, 0, width, soundFileHeight)
    );
    
    soundFileView.gridOn = false;
    soundFileView.timeCursorOn = true;
    soundFileView.timeCursorPosition = 240000;
    soundFileView.timeCursorColor = Color.red;

    soundFile = SoundFile.new();
    soundFile.openRead(soundFilePath); // using the path to which the buffer was written

    soundFileView.soundfile_(soundFile);
    soundFileView.read(0, soundFile.numFrames);

    soundFileView.mouseUpAction = { |view|
        var loFrames, hiFrames;
        loFrames = view.selection(0)[0];
        hiFrames = (view.selection(0)[1] + loFrames);
        ~cranesSynth.set(
            \start, loFrames / soundFile.sampleRate,
            \end, hiFrames / soundFile.sampleRate
        );

        // resetting the looper to start position
        ~cranesSynth.set(\gate, -1);
        ~cranesSynth.set(\gate, 1);

        Tdef(\test).reset;
        Tdef(\test).play;
    };

    Tdef(\test, {
        var delta = (1/60);
        var selection = soundFileView.selection(0);
        var selectionStart = selection[0];
        var selectionSize = selection[1];
        var selectionEnd = selectionStart + selectionSize;

        ~cursorPos = selection[0];

        loop {
            delta.wait();
            ~cursorPos = ~cursorPos + (delta * soundFile.sampleRate);
            if (~cursorPos >= selectionEnd,
                { 
                    ~cursorPos = ~cursorPos - selectionSize; },
                { });

            soundFileView.timeCursorPosition = ~cursorPos;
        }
    }).play(AppClock);


    // TODO: insert MIDI widgets here
};

OSCdef(\loadWindow, 
    { |msg| Routine { 
        ~loadWindow.value(msg);
    }.play(AppClock); },
    '/tmpBufferWritten');

~loadGui = {
    arg buf, numFrames = -1;

    buf.write(~getSoundFilePathForBuf.value(buf.bufnum),
        numFrames: numFrames,
        completionMessage: { ~netAddr.sendMsg('/tmpBufferWritten', buf.bufnum); });
};
)


~loadGui.value(b);

b.plot;

~netAddr.sendMsg('chat');

Tdef(\test).clear;
Tdef(\test).reset;



s.freeAll;





// playground
x = Synth(\recorder, [\in, ~recordBus, \buf, b]);
x.free;

b.plot;

y = Synth(\playback, [\out, 0, \buf, b, \start, 0.0, \end, 20.0]);
y.set(\rate, 1.5, \gate, 1);
y.free;

s.freeAll;

b.plot;
b.plot;


m.gui;
m.trace;


~lightOn.value(\bt, 2, 8);
~lightOff.value(\bt, 2, 8);

m.elAt(\bt).elAt('1').elAt('1', 'on');

(
m.elAt(\bt, 1, 1, 'on').action_({ |el|
    m.elAt(\bt, 1, 1, 'on').value_(1);
});
m.elAt(\bt, 1, 1, 'off').action_({ |el|
    m.elAt(\bt, 1, 1, 'on').value_(0);
});
)

m.elAt()

