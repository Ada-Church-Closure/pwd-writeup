from scapy.all import sendpfast, Ether
pkt = Ether(dst="02:00:00:00:00:02")/b'A'*128
sendpfast([pkt]*1000, iface="veth0", pps=10000)   # 每秒 10000 pkt