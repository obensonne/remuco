CDIR := data/complex

SRC_FILES_LOCAL := rem-cinfo.c rem-pinfo.c rem-sctrl.c rem-pstatus.c \
                   rem-plob.c rem-ploblist.c rem-library.c \
                   rem-string.c

SRC_FILES += $(addprefix $(CDIR)/,$(SRC_FILES_LOCAL))


