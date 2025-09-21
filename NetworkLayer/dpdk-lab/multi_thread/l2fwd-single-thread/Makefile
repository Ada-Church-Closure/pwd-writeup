APP = l2fwd
SRCS = main.c

CFLAGS += $(shell pkg-config --cflags libdpdk openssl)
LDFLAGS += $(shell pkg-config --libs libdpdk openssl)

all:
	$(CC) $(SRCS) -o $(APP) $(CFLAGS) $(LDFLAGS)
