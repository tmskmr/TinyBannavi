#!/bin/sh
WKDIR=`dirname "$0"`
if [ "`uname -s`" = "Darwin" ]; then
	cd "$WKDIR/../../../"
else
	cd "$WKDIR"
fi

if [ ! -f ./bin/tainavi/Viewer.class ]; then
	echo "NOT FOUND INSTALL DEST."
	exit 1
fi

if [ -d bin.new ]; then
	if [ -d bin.old ]; then
		rm -rf bin.old
	fi
	mv -f bin bin.old
	mv -f bin.new bin
	if [ -d env.old ]; then
		rm -rf env.old
	fi
	mkdir env.old
	( cd env; tar cf - . ) | ( cd env.old; tar xf - )
fi

MAXHEAP=1024m
java -Xmx$MAXHEAP -version
if [ "$?" -ne "0" ]; then
	MAXHEAP=768m
	java -Xmx$MAXHEAP -version
	if [ "$?" -ne "0" ]; then
		MAXHEAP=512m
		java -Xmx$MAXHEAP -version
		if [ "$?" -ne "0" ]; then
			MAXHEAP=256m
			java -Xmx$MAXHEAP -version
			if [ "$?" -ne "0" ]; then
				exit 1
			fi
		fi
	fi
fi

echo "MAXHEAP=$MAXHEAP"

GTKRC=env/_gtkrc-2.0
if [ -f "$GTKRC" ]; then
	export GTK2_RC_FILES="$GTKRC"
fi

CLASSPATH=bin:javamail/mail.jar:javamail/activation.jar:calendar/gdata-calendar-1.0.jar:calendar/gdata-client-1.0.jar
VMARGS="-Xms64m -Xmx$MAXHEAP -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo"
if [ "`uname -s`" = "Darwin" ]; then
	java $VMARGS -cp "$CLASSPATH" -Xdock:icon="$WKDIR/../Resources/tainavi.icns" -Xdock:name=TinyBangumiNavigator tainavi.Viewer -l $* &
else
	java $VMARGS -cp "$CLASSPATH" -Dawt.useSystemAAFontSettings=on tainavi.Viewer -l $*
fi
