CDIR := data/basic

SRC_FILES_LOCAL := rem-il.c rem-bin.c rem-sl.c

SRC_FILES += $(addprefix $(CDIR)/,$(SRC_FILES_LOCAL))

