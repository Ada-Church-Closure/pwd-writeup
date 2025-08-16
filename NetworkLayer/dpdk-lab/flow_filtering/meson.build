flow_filtering_sources = [
  'main.c',
  'flow_skeleton.c',
  'snippets/snippet_match_ipv4.c',
  'snippets/snippet_match_gre.c',
  'snippets/snippet_match_mpls.c',
  'snippets/snippet_re_route_to_kernel.c'
]

# cflags += '-Wno-unused-function'

allow_experimental_apis = true

executable('flow_filtering',
  flow_filtering_sources,
  include_directories: include_directories('.'),
  dependencies: dpdk_dep,
  install: false
)

