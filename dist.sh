sbt assembly
rm -fr dist/*
cp target/scala-2.13/lauda-assembly-1.0.jar dist/lauda.jar
cp template/config.yml dist/config.yml
cp template/logback.xml dist/logback.xml
cp template/lauda.sh dist/lauda.sh
cp -R template/lib dist/lib
cd dist
tar -cvzf lauda-v1.0.tar.gz lauda.jar config.yml logback.xml lib
rm -f *.yml
rm -f *.jar
rm -f *.xml
rm -fr lib
rm -fr *.sh
cd ..