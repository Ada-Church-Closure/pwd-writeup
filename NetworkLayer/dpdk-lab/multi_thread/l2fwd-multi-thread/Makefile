APP = l2fwd
SRCS = main.c

CFLAGS += $(shell pkg-config --cflags libdpdk openssl) -mssse3 -msse4.2
LDFLAGS += $(shell pkg-config --libs libdpdk openssl) -lrte_log -lrte_ethdev -lrte_mbuf

all:
	$(CC) $(SRCS) -o $(APP) $(CFLAGS) $(LDFLAGS)
