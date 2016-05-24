#
# BobTheBuilder Makefile
#

practice: rc/META-INF/MANIFEST.MF BobTheBuilder.jar
	cp BobTheBuilder.jar rc/robots/BobTheBuilder.jar
	java -Xmx512M -cp rc/libs/robocode.jar robocode.Robocode -cwd rc/

BobTheBuilder.jar: robots/bobthebuilder/*.java robots/bobthebuilder/BobTheBuilder.properties robots/bobthebuilder/manifest.txt rc/libs/robocode.jar
	javac -cp rc/libs/robocode.jar robots/bobthebuilder/*.java
	jar -cfm BobTheBuilder.jar robots/bobthebuilder/manifest.txt -C robots/ .

depends: # May not work on Windows
	wget http://iweb.dl.sourceforge.net/project/robocode/robocode/1.9.2.5/robocode-1.9.2.5-setup.jar \
		-O rc.jar
	sha256sum rc.jar | grep -q 4a899ed17718ee6511cf78e94ed3a9b247271d0d324aa9b154da32aadf0a59b8
	unzip -d rc/ rc.jar
cleandepends:
	rm -r rc.jar rc/
clean:
	rm robots/bobthebuilder/*.class

build: BobTheBuilder.jar

cleanall: clean cleandepends
