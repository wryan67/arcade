#!/bin/ksh



mvn clean install
[ $? != 0 ] && exit

#javafxpackager -createjar -srcdir out/production/arcade -outfile arcade.jar -appclass arcade.DisplayShelf
#javafxpackager -deploy -srcdir out/production/arcade -outfile arcade.exe -appclass arcade.DisplayShelf -outdir target -native exe
#javafxpackager -deploy -srcdir target/classes/arcade -outfile arcade.exe -appclass arcade.DisplayShelf -outdir target -native exe
javafxpackager -createjar -srcdir target/classes -outfile arcade.jar -appclass arcade.DisplayShelf
[ $? != 0 ] && exit

mkdir target/dep      
cd target/dep
jar xvf ../../lib/opencsv-3.4.jar
jar xvf ../../lib/commons-lang3-3.3.2.jar
rm -rf META-INF
jar uf ../../arcade.jar .
[ $? != 0 ] && exit

cd -
cp arcade.jar /cygdrive/c/games/

