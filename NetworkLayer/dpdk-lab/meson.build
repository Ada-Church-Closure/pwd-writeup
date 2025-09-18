# a top build file
project('dpdk-lab', 'c')

dpdk_dep = dependency('libdpdk', required: true)
dpdk_dep = dependency('libdpdk', fallback: ['dpdk', 'dpdk_dep'])


# subdir('basic-fwd')
# subdir('rxtx_callbacks')
# subdir('flow_filtering')
# subdir('multi_process/simple_mp')
subdir('multi_thread/l2fwd')
# subdir('multi_thread/l2fwd-crypto')

