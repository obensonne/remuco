RB_PLUGIN_DIR="${HOME}/.gnome2/rhythmbox/plugins"

mkdir -p "$RB_PLUGIN_DIR"

rm -rf "$RB_PLUGIN_DIR/remythm"

mkdir "$RB_PLUGIN_DIR/remythm"

DEST="$RB_PLUGIN_DIR/remythm"

cp -r remuco "$DEST"
cp rhythmbox/__init__.py "$DEST"
cp rhythmbox/remuco.rb-plugin "$DEST"
cp rhythmbox/README "$DEST"

find "$DEST" -type d -name ".svn" | xargs rm -rf

echo "INSTALL OK. Installed player adapter as a Rhythmbox plugin in $DEST"