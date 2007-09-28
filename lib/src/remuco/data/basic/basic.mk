CDIR := remuco/data/basic

OBJ_FILES_LOCAL := rem-iv.o rem-sv.o rem-bin.o

OBJ_FILES += $(patsubst %,$(CDIR)/%,$(OBJ_FILES_LOCAL))

