# Systems & Network Security Portfolio (PWN, eBPF/XDP, DPDK)

This repository collects my hands-on work in systems and network security:
- Memory exploitation practice (stack overflows, format strings, heap) with write-ups and exploit scripts
- Linux kernel observability and packet processing with eBPF/XDP
- High-performance networking experiments with DPDK (I/O vs crypto pipeline design, rings, multi-core)
- Control-plane labs with OSPF/BGP in containerized topologies and Linux virtual networking
- Supporting notes from coursework and side projects

I maintain this repo as a living portfolio to document practical skills and experiments relevant to systems and network security research.

## Highlights
- PWN practice: solved and documented challenges from pwn.college and pwnable.kr; used gdb/gef, pwntools, static/dynamic analysis; worked with mitigations (NX, PIE, canaries, partial RELRO)
- eBPF/XDP: built kprobe-based tracers with BCC; inspected programs/maps with bpftool; implemented basic XDP packet filtering at the earliest hook for fast drop/redirect
- DPDK: extended l2fwd with a decoupled I/O to crypto pipeline using rte_ring and TAP/veth; compared single-thread vs two-thread vs multi-worker crypto designs across payload sizes; generated traffic via testpmd; used Intel VTune for hotspots; built DPDK from source to resolve ABI/version issues
- Control plane & container networking: composed OSPF/BGP labs with FRRouting and Open vSwitch; manipulated namespaces, veth pairs, and Linux bridges with iproute2

## Repository Map
- pwnColledge/pwnColledgeNotes: notes and scripts from pwn.college modules
- pwnable: write-ups and code for pwnable.kr challenges
- NetworkLayer/ebpf: eBPF/XDP notes, BCC examples, bpftool usage (maps/programs)
- NetworkLayer/dpdk-lab:
  - multi_thread: l2fwd variants, decoupled I/O–crypto pipeline with rte_ring, performance notes
  - flow_filtering: flow rules, TAP PMD usage, notes on DPDK versions/builds
  - rxtx_callbacks, multi_process: additional experiments
- NetworkLayer/ospf&bgp: mini-project and notes on OSPF/BGP
- NetworkNotes: layered network notes (Transport, Network, Application intro)

## Reproducible Examples

### eBPF/BCC quick start (from NetworkLayer/ebpf)
- List running programs and maps:
  - `bpftool prog list`
  - `bpftool map list`
- Dump a program:
  - `bpftool prog dump xlated id <ID> linum`

Minimal BCC example (kprobe on execve):
```python
from bcc import BPF
program = r"""
int hello(void *ctx) {
    bpf_trace_printk("Hello World!\n");
    return 0;
}
"""
b = BPF(text=program)
syscall = b.get_syscall_fnname("execve")
b.attach_kprobe(event=syscall, fn_name="hello")
b.trace_print()
```

### DPDK flow filtering (TAP PMD)
- Example run (from flow_filtering notes):
  - `sudo ./examples/dpdk-flow_filtering -l 0-1 -n 4 --vdev=net_tap0 -- --non-template`
- If system packages mismatch, build from source with Meson/Ninja and update:
  - `meson setup build --prefix=/usr -Dexamples=all && ninja -C build && sudo ninja -C build install && sudo ldconfig`

### DPDK multi-thread crypto pipeline (I/O to crypto decoupling)
- Prepare a veth pair:
  - `sudo ip link add veth0 type veth peer name veth1 && sudo ip link set veth0 up && sudo ip link set veth1 up`
- Build example (adjust path if needed):
  - `make -C NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread clean && make -C NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread`
- Start a traffic generator (testpmd):
```sh
sudo dpdk-testpmd -l 2-3 -n 4 \
  --vdev=net_pcap0,iface=veth0 \
  --file-prefix=testpmd \
  --proc-type=auto \
  -- --forward-mode=txonly \
  --txd=1024 --rxd=1024 \
  --port-topology=loop \
  --nb-ports=1 \
  -i
# Inside testpmd:
# stop; port stop all; port config all txq 1; port config all rxq 1; set fwd txonly; set txpkts 64; port start all; start tx_first
```
- Baseline (2 lcores, coupled I/O+crypto):
```sh
sudo NetworkLayer/dpdk-lab/multi_thread/l2fwd-single-thread/l2fwd -l 0-1 -n 4 \
  --vdev=net_pcap0,iface=veth0 \
  --vdev=net_pcap1,iface=veth1 \
  -- -p 0x3
```
- Decoupled (1 I/O + 1 crypto):
```sh
sudo NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread/l2fwd -l 0-1 -n 4 \
  --vdev=net_pcap0,iface=veth0 \
  --vdev=net_pcap1,iface=veth1 \
  -- -p 0x3 -q 2
```
- Scale crypto workers (example: 4 lcores = 1 I/O + 3 crypto):
```sh
sudo NetworkLayer/dpdk-lab/multi_thread/l2fwd-multi-thread/l2fwd -l 0-3 -n 4 \
  --vdev=net_pcap0,iface=veth0 \
  --vdev=net_pcap1,iface=veth1 \
  -- -p 0x3 -q 2
```

## Observations
- With small payloads, decoupling I/O and crypto and adding workers improves throughput until hitting I/O bottlenecks
- With larger payloads, a 2-thread decoupled design may underperform due to ring and synchronization overhead, while 4+ crypto workers begin to show gains
- VTune hotspot analysis and bpftool inspections were used to identify bottlenecks and validate behavior

## What I Am Exploring Next
- More XDP-based network function designs and a comparison with userspace DPDK paths
- Deeper kernel tracing (uprobes/kprobes, tracepoints) to study syscall and networking hotspots
- Automated perf/throughput/latency measurements and plotting
