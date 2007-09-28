CDIR := remuco/data/complex

OBJ_FILES_LOCAL := rem-cinfo.o rem-pinfo.o rem-sctrl.o rem-ps.o rem-plob.o \
		   rem-ploblist.o rem-library.o

OBJ_FILES += $(patsubst %,$(CDIR)/%,$(OBJ_FILES_LOCAL))

