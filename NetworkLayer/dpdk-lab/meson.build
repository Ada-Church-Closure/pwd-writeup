project('dpdk_rx', 'c')

dpdk_dep = dependency('libdpdk', required : true)

executable('dpdk_rx_mac',
  'main.c',
  dependencies : dpdk_dep
)
