-- init.lua (very small)
pktgen.set("all", "size", 512)
pktgen.set("all", "rate", 100)   -- 100% 等价快速测试
pktgen.start("all")

