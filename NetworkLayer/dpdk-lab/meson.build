# a top build file
project('dpdk-lab', 'c')

dpdk_dep = dependency('libdpdk', required: true)
dpdk_dep = dependency('libdpdk', fallback: ['dpdk', 'dpdk_dep'])


# subdir('basic-fwd')
# subdir('rxtx_callbacks')
# subdir('flow_filtering')
subdir('multi_thread/simple_mp')


