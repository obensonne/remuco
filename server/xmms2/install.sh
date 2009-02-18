mkdir -p tmp

rm -rf tmp/xmms2

mkdir tmp/xmms2

cp -r remuco tmp/xmms2/
cp xmms2/xmms2.py tmp/xmms2/

find tmp/xmms2 -type d -name ".svn" | xargs rm -rf

echo "INSTALL OK. Start xmms2 adapter with 'python tmp/xmms2/xmms2.py'"