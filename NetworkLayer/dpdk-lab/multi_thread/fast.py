from scapy.all import sendpfast, Ether
pkt = Ether(dst="9a:f1:fe:7e:e1:4f", src="02:00:00:00:00:02",type=0x0800)/b'A'*128
sendpfast([pkt]*1000, iface="veth0", pps=1000000)   # 每秒 10000 pkt