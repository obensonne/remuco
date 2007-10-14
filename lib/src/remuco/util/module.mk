CDIR := remuco/util

SRC_FILES_LOCAL := 

# id3 tag support (optional)
ifeq "$(strip $(BUILD_TAGS))" "yes"
SRC_FILES_LOCAL += rem-tags.c
PKG_CONFIG_REQ += id3tag
endif

# image (album art) support:
SRC_FILES_LOCAL += rem-img.c
PKG_CONFIG_REQ += Wand

SRC_FILES += $(addprefix $(CDIR)/,$(SRC_FILES_LOCAL))

