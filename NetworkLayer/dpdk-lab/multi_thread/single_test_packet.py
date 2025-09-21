# from scapy.all import Ether, sendp
# # 发送一个以太网帧
# # 从veth0发送到veth1
# pkt = Ether(dst="da:c9:6a:67:8a:d6", src="9a:f1:fe:7e:e1:4f")/b'HELLO-DPDK'
# sendp(pkt, iface="veth0", count=1, verbose=1)


from scapy.all import Ether, sendp
# 发送一个以太网帧
# 从veth0发送到veth1
# pkt = Ether(dst="9a:f1:fe:7e:e1:4f", src="02:00:00:00:00:02")/b'HELLO-DPDK'

pkt = Ether(dst="02:70:63:61:70:00", src="02:00:00:00:00:02")/b'HELLO-DPDK'

for index in range(10):
    sendp(pkt, iface="veth0", count=1, verbose=1)

