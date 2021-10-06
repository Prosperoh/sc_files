s.boot;
s.quit;

~mixer = NdefMixer(s);

Ndef(\test).clear;

// can use tidal_longersounds -> erokia
l.value('test');
e.sb['pstretch'].value('test');
g.value('test');



Ndef(\conv) <<> Ndef(\test);
Ndef(\conv).gui;

Ndef(\conv).clear;


(
Ndef(\pos, {
    arg lfreq = 0.5,
        centerPos = #[0.2, 0.8],
        maxWidth = 0.2;

    var pos, width;

    pos = LFNoise1.kr(lfreq).range(centerPos[0], centerPos[1]);
    width = LFNoise1.kr(lfreq).range(0, maxWidth);
    [pos - width, pos + width]
});
ControlSpec.add(\maxWidth, [0, 1, \lin]);
)

Ndef(\test).set(\centerPos, Ndef(\pos));

Ndef(\pos).gui;

