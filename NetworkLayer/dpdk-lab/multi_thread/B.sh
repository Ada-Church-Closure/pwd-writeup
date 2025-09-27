#!/bin/bash
exec sudo /home/ada/pwd-writeup/NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread/l2fwd \
  -l 0-1 -n 4 \
  --vdev=net_pcap0,iface=veth0 \
  --vdev=net_pcap1,iface=veth1 \
  -- -p 0x3 --no-mac-updating

