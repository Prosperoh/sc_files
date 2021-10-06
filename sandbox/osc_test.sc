(
OSCdef.new(
    \button1,
    {
        arg msg, time, addr, port;
        [msg, time, addr, port].postln;
    },
    '/button_1',
)
)

NetAddr.langPort;
