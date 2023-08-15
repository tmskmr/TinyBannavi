#!/bin/sh -e

WD=$PWD

cd `dirname $0`

if [ ! -f ../../bin/tainavi/Viewer.class ]; then
	echo "COULD NOT EXECUTE."
	exit 1
fi

echo "...INSTALL BINARIES."
if [ -d ../../bin.new ]; then
	rm -rf ../../bin.new
fi
mv bin ../../bin.new

echo "...INSTALL NEW ENVIRONMENT FILES."
find env ! -name env -prune -type f -exec cp -np {} ../../{} \;

echo "...INSTALL NEW ICON FILES."
find icon ! -name icon -prune -type f -exec cp -np {} ../../{} \;

if [ -f ../../javamail/PlugIn_RecGoogleCalendar.class ]; then
	rm -f ../../javamail/PlugIn_Rec*.class
fi
if [ -f ../../javamail/PlugIn_RecRD_MAIL.class ]; then
	rm -f ../../javamail/PlugIn_Rec*.class
fi

echo "...INSTALL SCRIPT FILES."
chmod +x *.sh
find . ! -name . -prune -type f -name \*\.cmd -exec cp -fp {} ../../{} \;
find . ! -name . -prune -type f -name \*\.sh -a ! -name _update.sh -exec cp -fp {} ../../{} \;
if [ ! -d "../../TaiNavi for Mac.app/" ]; then
	tar cf - "TaiNavi for Mac.app" | ( cd ../../; tar xf - )
	rm -f ../../tinybannavi.command
fi
rm -f ../../_update.cmd
rm -f ../../_update.sh
cp -fp tinybannavi.sh "../../TaiNavi for Mac.app/Contents/MacOS/tinybannavi.sh"

cp -fp TaiNavi.exe ../../TaiNavi.exe
if [ ! -f ../../TaiNavi.ini ]; then
	cp -fp TaiNavi.ini ../../
fi

echo "...INSTALL TEXT FILES."
rm -f ../../*.txt
find . ! -name . -prune -type f -name \*\.txt -exec cp -fp {} ../../{} \;

cd $WD
exit 0
