#!/bin/bash
# DPDK L2转发性能测试分析脚本

echo "=== DPDK L2转发性能测试 ==="

# 检查系统负载
echo "1. 系统负载情况:"
top -bn1 | grep "Cpu(s)"
free -h | grep "Mem"

# 检查网络接口统计
echo -e "\n2. 网络接口统计:"
cat /proc/net/dev | grep -E "(veth0|veth1)" | awk '{print "接口: " $1 " RX包: " $2 " TX包: " $10}'

# 检查DPDK进程
echo -e "\n3. DPDK进程状态:"
ps aux | grep -E "(l2fwd|dpdk)" | grep -v grep

# 检查CPU绑定
echo -e "\n4. CPU核心使用情况:"
for pid in $(pgrep l2fwd); do
    echo "进程 $pid CPU亲和性: $(taskset -p $pid 2>/dev/null | cut -d: -f2)"
done

# 检查hugepage使用
echo -e "\n5. Hugepage使用情况:"
cat /proc/meminfo | grep -i huge

echo -e "\n测试建议:"
echo "- 对于小包(64-256B): 关注PPS性能"
echo "- 对于大包(1024B+): 关注带宽利用率"
echo "- 监控CPU使用率是否均衡"
echo "- 检查是否有包丢失"