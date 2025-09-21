-- init_safe.lua
local function wait_ports_ready(timeout_s)
  local start = os.time()
  while true do
    local ready = true
    local ports = pktgen.portList()
    for _, p in ipairs(ports) do
      local status = pktgen.portLinkStatus(p)
      if status.link == false then
        ready = false
        break
      end
    end
    if ready then
      print("All ports ready")
      return true
    end
    if os.time() - start > timeout_s then
      print("Timeout waiting ports")
      return false
    end
    pktgen.pause(100) -- sleep 100ms
  end
end

-- minimal safe init
pktgen.set("all", "count", 0)
pktgen.set("all", "rate", 10)
pktgen.set("all", "size", 512)

-- wait up to 5s for ports
wait_ports_ready(5)

-- start transmit on all ports
pktgen.start("all")

