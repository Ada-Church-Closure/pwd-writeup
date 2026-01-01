#!/usr/bin/env python3
"""
DPDK L2转发压力测试脚本
支持多种包大小、发包速率控制、统计信息收集
"""

import time
import threading
import sys
import signal
from scapy.all import *
import argparse

class DPDKStressTester:
    def __init__(self, iface="veth0", dst_mac="02:70:63:61:70:00"):
        self.iface = iface
        self.dst_mac = dst_mac
        self.running = False
        self.stats = {
            'sent_packets': 0,
            'sent_bytes': 0,
            'start_time': 0,
            'end_time': 0
        }
    
    def signal_handler(self, signum, frame):
        print(f"\n收到信号 {signum}, 停止测试...")
        self.running = False
    
    def create_test_packet(self, size=64, payload_type='random'):
        """创建测试数据包"""
        # 以太网头占14字节，所以payload = size - 14
        payload_size = max(0, size - 14)
        
        if payload_type == 'random':
            payload = bytes([random.randint(0, 255) for _ in range(payload_size)])
        elif payload_type == 'pattern':
            payload = bytes([i % 256 for i in range(payload_size)])
        else:
            payload = b'A' * payload_size
            
        return Ether(dst=self.dst_mac) / payload
    
    def burst_test(self, packet_count=1000, packet_size=64, burst_size=32, interval=0.001):
        """突发测试模式"""
        print(f"突发测试: {packet_count}个包, 大小{packet_size}字节, 突发大小{burst_size}")
        
        pkt = self.create_test_packet(packet_size)
        self.stats['start_time'] = time.time()
        
        sent = 0
        while sent < packet_count and self.running:
            # 创建突发包
            burst_pkts = [pkt] * min(burst_size, packet_count - sent)
            
            try:
                sendp(burst_pkts, iface=self.iface, verbose=False)
                sent += len(burst_pkts)
                self.stats['sent_packets'] += len(burst_pkts)
                self.stats['sent_bytes'] += len(burst_pkts) * packet_size
                
                if interval > 0:
                    time.sleep(interval)
                    
            except Exception as e:
                print(f"发送错误: {e}")
                break
                
        self.stats['end_time'] = time.time()
        
    def rate_limited_test(self, duration=30, packet_size=64, pps=1000):
        """限速测试模式"""
        print(f"限速测试: {duration}秒, 大小{packet_size}字节, 速率{pps}pps")
        
        pkt = self.create_test_packet(packet_size)
        interval = 1.0 / pps if pps > 0 else 0
        
        self.stats['start_time'] = time.time()
        end_time = self.stats['start_time'] + duration
        
        while time.time() < end_time and self.running:
            try:
                sendp(pkt, iface=self.iface, verbose=False)
                self.stats['sent_packets'] += 1
                self.stats['sent_bytes'] += packet_size
                
                if interval > 0:
                    time.sleep(interval)
                    
            except Exception as e:
                print(f"发送错误: {e}")
                break
                
        self.stats['end_time'] = time.time()
    
    def multi_size_test(self, sizes=[64, 128, 256, 512, 1024, 1518], packets_per_size=1000):
        """多种包大小测试"""
        print(f"多包大小测试: {sizes}, 每种大小{packets_per_size}个包")
        
        self.stats['start_time'] = time.time()
        
        for size in sizes:
            if not self.running:
                break
                
            print(f"  测试包大小: {size}字节")
            pkt = self.create_test_packet(size)
            
            for i in range(packets_per_size):
                if not self.running:
                    break
                try:
                    sendp(pkt, iface=self.iface, verbose=False)
                    self.stats['sent_packets'] += 1
                    self.stats['sent_bytes'] += size
                except Exception as e:
                    print(f"发送错误: {e}")
                    break
                    
        self.stats['end_time'] = time.time()
    
    def print_stats(self):
        """打印统计信息"""
        duration = self.stats['end_time'] - self.stats['start_time']
        if duration <= 0:
            return
            
        pps = self.stats['sent_packets'] / duration
        bps = self.stats['sent_bytes'] * 8 / duration
        mbps = bps / (1024 * 1024)
        
        print(f"\n=== 测试统计 ===")
        print(f"测试时长:     {duration:.2f} 秒")
        print(f"发送包数:     {self.stats['sent_packets']}")
        print(f"发送字节:     {self.stats['sent_bytes']}")
        print(f"包速率:       {pps:.2f} pps")
        print(f"比特率:       {mbps:.2f} Mbps")
        print(f"平均包大小:   {self.stats['sent_bytes'] / max(1, self.stats['sent_packets']):.1f} 字节")

def main():
    parser = argparse.ArgumentParser(description='DPDK L2转发压力测试工具')
    parser.add_argument('-i', '--interface', default='veth0', help='发送接口')
    parser.add_argument('-d', '--dst-mac', default='02:70:63:61:70:00', help='目标MAC地址')
    parser.add_argument('-m', '--mode', choices=['burst', 'rate', 'multi'], default='burst', 
                       help='测试模式: burst(突发), rate(限速), multi(多包大小)')
    parser.add_argument('-c', '--count', type=int, default=10000, help='包数量(burst模式)')
    parser.add_argument('-s', '--size', type=int, default=64, help='包大小')
    parser.add_argument('-r', '--rate', type=int, default=1000, help='发包速率(pps)')
    parser.add_argument('-t', '--time', type=int, default=30, help='测试时长(秒)')
    parser.add_argument('-b', '--burst-size', type=int, default=32, help='突发大小')
    
    args = parser.parse_args()
    
    # 创建测试器
    tester = DPDKStressTester(args.interface, args.dst_mac)
    
    # 注册信号处理
    signal.signal(signal.SIGINT, tester.signal_handler)
    signal.signal(signal.SIGTERM, tester.signal_handler)
    
    tester.running = True
    
    try:
        if args.mode == 'burst':
            tester.burst_test(args.count, args.size, args.burst_size)
        elif args.mode == 'rate':
            tester.rate_limited_test(args.time, args.size, args.rate)
        elif args.mode == 'multi':
            tester.multi_size_test()
            
    except KeyboardInterrupt:
        print("\n用户中断测试")
    finally:
        tester.running = False
        tester.print_stats()

if __name__ == '__main__':
    main()