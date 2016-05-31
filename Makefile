#
# BobTheBuilder Makefile
#

ROBO = java -Xmx512M -cp rc/libs/robocode.jar robocode.Robocode -cwd rc/

time := $(shell date +%s)

BobTheBuilder.jar: bobthebuilder/*.java bobthebuilder/manifest.txt bobthebuilder/*.properties rc/libs/robocode.jar
	javac -cp rc/libs/robocode.jar bobthebuilder/*.java
	jar -cfm BobTheBuilder.jar bobthebuilder/manifest.txt bobthebuilder/

rc/robots/BobTheBuilder.jar: BobTheBuilder.jar
	cp BobTheBuilder.jar rc/robots/BobTheBuilder.jar

depends:
	mkdir -p rc/
	wget http://iweb.dl.sourceforge.net/project/robocode/robocode/1.9.2.5/robocode-1.9.2.5-setup.jar \
		-O rc/rc.jar
	sha256sum rc/rc.jar | grep -q 4a899ed17718ee6511cf78e94ed3a9b247271d0d324aa9b154da32aadf0a59b8
	unzip -d rc/ rc/rc.jar

cleandepends:
	rm -rf rc/
	rm -rf config/
cleanbattles:
	rm -rf tests/
clean:
	rm -f bobthebuilder/*.class
	rm -f BobTheBuilder.jar
cleanall: cleandepends cleanbattles clean

test: rc/robots/BobTheBuilder.jar rc/libs/robocode.jar
	mkdir -p tests/
	$(ROBO) -nodisplay -battle battles/test1.battle \
		-recordxml tests/B$(time).xml -results tests/R$(time).txt
	echo " == Results /battles/test1.battle $(time)  == " && \
		cat tests/R$(time).txt
	awk 'BEGIN {FS=" "};NR==3{print $$2}' tests/R$(time).txt | \
		grep -q "bobthebuilder.BobTheBuilder"

practicegui: rc/libs/robocode.jar rc/robots/BobTheBuilder.jar
	cp BobTheBuilder.jar rc/robots/BobTheBuilder.jar
	$(ROBO)

build: BobTheBuilder.jar

cleanall: clean cleandepends
