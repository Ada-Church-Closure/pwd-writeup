from scapy.all import *
import time

# 配置接口和MAC地址
iface = "veth0"
dst_mac = "9a:f1:fe:7e:e1:4f"  # 目标MAC（需与DPDK配置一致）
src_mac = "02:00:00:00:00:00"  # 源MAC（避免与DPDK冲突）

# 构造自定义以太网帧
pkt = Ether(src=src_mac, dst=dst_mac)/Raw(load=b"TEST_PAYLOAD_1234")

# 发送并监听响应（需在另一终端抓包）
sendp(pkt, iface=iface, loop=1, inter=0.1, verbose=True)