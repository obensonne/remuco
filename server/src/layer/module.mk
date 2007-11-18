CDIR := layer

SRC_FILES_LOCAL := rem-net-bt.c rem-net-io.c rem-server.c

SRC_FILES += $(addprefix $(CDIR)/,$(SRC_FILES_LOCAL))

PKG_CONFIG_REQ += bluez

