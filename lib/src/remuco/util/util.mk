CDIR := remuco/util

OBJ_FILES_LOCAL := 

# for player proxies reading music meta data from music files:
ifeq "$(strip $(BUILD_TAGS))" "yes"
OBJ_FILES_LOCAL += rem-tags.o
CFLAGS += $(shell pkg-config --cflags id3tag)
LFLAGS += $(shell pkg-config --libs id3tag)
PKG_CONFIG_REQ += id3tag
endif

# for player proxies providing cover art:
#ifeq "$(strip $(BUILD_IMG))" "yes"
OBJ_FILES_LOCAL += rem-img.o
CFLAGS += $(shell pkg-config --cflags Wand)
LFLAGS += $(shell pkg-config --libs Wand)
PKG_CONFIG_REQ += Wand
#endif


OBJ_FILES += $(patsubst %,$(CDIR)/%,$(OBJ_FILES_LOCAL))

