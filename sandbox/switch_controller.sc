MKtl.find();
k = MKtl(\swp, "*switch-pro");
c = MKtl(\proco, 'hid_0_pro_controller');
MKtl.clear;

MKtl(\proco).rebuild(MKtlDesc.at('nintendo-switch-pro'));

HIDFunc.trace(true);

MKtlDesc.at('nintendo-switch-pro');

c.trace;

HID.postAvailable;
~proco = HID.open(1406, 8201);
HIDFunc.trace(false);
HIDFunc.trace(true);

~proco.postElements;
~proco.postInputElements;

p = MKtl(\cont, 'hid_0_pro_controller');
MKtl('cont').explore(false);
MKtl('cont').createDescriptionFile;
p.createDescriptionFile;

MKtl.find('hid');

MKtl.descFolders;
MKtl.postLoadedDescs;

o = MKtl(\sony, 'hid_0_wireless_controller');
o.explore(false);

p.explore(false);
p.gui;

c.createDescriptionFile;









// AKAI MIDIMix
MKtl.find('midi');
m = MKtl(\akai, "akai-midimix");
m.explore(true);
m.explore(false);
m.gui;
m.postElements
m.elementGroup.keys

m.elAt(\kn, 2, 1);
