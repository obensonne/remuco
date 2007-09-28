CDIR := remuco/layer

OBJ_FILES_LOCAL := rem-net-bt.o rem-net-io.o rem-comm.o rem-server.o

OBJ_FILES += $(patsubst %,$(CDIR)/%,$(OBJ_FILES_LOCAL))

CFLAGS += $(shell pkg-config --cflags bluez)
LFLAGS += $(shell pkg-config --libs bluez)

PKG_CONFIG_REQ += bluez

