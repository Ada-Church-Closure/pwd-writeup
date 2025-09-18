#!/usr/bin/env python3
from scapy.all import Ether, sendp
import sys
import time
import random

# 先用1000个随机的frame做测试.
iface = sys.argv[1] if len(sys.argv)>1 else "veth0"
# dst = sys.argv[2] if len(sys.argv)>2 else "da:c9:6a:67:8a:d6"  # 目标 MAC
dst = sys.argv[2] if len(sys.argv)>2 else "02:70:63:61:70:00"  # 目标 MAC
count = int(sys.argv[3]) if len(sys.argv)>3 else 1000
payload_len = int(sys.argv[4]) if len(sys.argv)>4 else 128
interval = float(sys.argv[5]) if len(sys.argv)>5 else 0.0  # 秒，0 表示尽可能快

for i in range(count):
    payload = bytes([random.getrandbits(8) for _ in range(payload_len)])
    pkt = Ether(dst=dst)/payload
    sendp(pkt, iface=iface, verbose=False)
    if interval>0:
        time.sleep(interval)
print("done")