SuperDirt.start;



(
OSCdef.new(
	\sampleLength,
	{
		arg msg, time, addr, port;
		var name, number, address, length;

		address = NetAddr.new("localhost", 6010);

		name = msg[1].asSymbol;

		number = msg[2];

		length = ~dirt.soundLibrary.buffers[name][number].duration.round(0.01);

		address.sendMsg("/ctrl", "sampleLength", length);
	},
	'/sampleLength'
  );
 )