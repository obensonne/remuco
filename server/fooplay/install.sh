mkdir -p tmp

rm -rf tmp/fooplay

mkdir tmp/fooplay

cp -r remuco tmp/fooplay/
cp fooplay/fooplay.py tmp/fooplay/

find tmp/fooplay -type d -name ".svn" | xargs rm -rf

echo "INSTALL OK. Start fooplay adapter with 'python tmp/fooplay/fooplay.py'"