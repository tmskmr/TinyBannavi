@ECHO OFF

SET WD=%CD%

CD %~dp0

ECHO ...アップデート先が存在するか確認します.
IF NOT EXIST ..\..\bin\tainavi\Viewer.class GOTO ERROREXIT

@REM JavaRuntimeをコピー
IF EXIST jre (
	IF EXIST ..\..\jre6 (
		IF EXIST ..\..\jre6.old\ RD /S /Q ..\..\jre6.old\
		REN jre jre.new
	)
	IF EXIST ..\..\jre (
		IF EXIST ..\..\jre.old\ RD /S /Q ..\..\jre.old\
		REN jre jre.new
	)
)
IF EXIST jre.new (
	ECHO ...JavaRuntimeをコピーします.
	MOVE /Y jre.new ..\..\
)

IF EXIST ..\..\calendar\PlugIn_RecGoogleCalendar.class (
	DEL /F ..\..\calendar\PlugIn_Rec*.class
)

IF EXIST ..\..\javamail\PlugIn_RecRD_MAIL.class (
	DEL /F ..\..\javamail\PlugIn_Rec*.class
)

IF EXIST bin (
	IF EXIST ..\..\bin.old2\ RD /S /Q ..\..\bin.old2\
	IF EXIST ..\..\bin.new\ RD /S /Q ..\..\bin.new\
	REN bin bin.new
)
IF EXIST bin.new (
	ECHO ...バイナリをコピーします.
	MOVE /Y bin.new ..\..\
)

ECHO ...新規に追加された設定ファイルをコピーします.
IF EXIST ..\..\env.old2\ RD /S /Q ..\..\env.old2\
IF EXIST ..\..\env.old\ REN ..\..\env.old\ env.old2
MD ..\..\env.old\
XCOPY ..\..\env ..\..\env.old\ /S /E /Q /Y
FOR %%i IN (env\*.*) DO (
	IF NOT EXIST "..\..\env\%%~nxi" COPY "%%i" ..\..\env\
)

ECHO ...新規に追加されたアイコンをコピーします.
FOR %%i IN (icon\*.*) DO (
	IF NOT EXIST "..\..\icon\%%~nxi" COPY "%%i" ..\..\icon\
)

ECHO ...スクリプトをコピーします.
FOR %%i IN (*.cmd) DO (
	IF NOT "%%i" == "_update.cmd" COPY "%%i" ..\..\
)
FOR %%i IN (*.sh) DO (
	COPY "%%i" ..\..\
)
DEL /F ..\..\_update.cmd
DEL /F ..\..\_update.sh
COPY tinybannavi.sh ..\..\tinybannavi.command

IF EXIST ..\..\TaiNavi.exe.old DEL /F ..\..\TaiNavi.exe.old
IF EXIST ..\..\TaiNavi.exe REN ..\..\TaiNavi.exe TaiNavi.exe.old
COPY /Y TaiNavi.exe ..\..\
IF NOT EXIST ..\..\TaiNavi.ini COPY TaiNavi.ini ..\..\

ECHO ...テキストをコピーします.
FOR %%i IN (*.txt) DO (
	COPY "%%i" ..\..\
)

CD %WD%
EXIT 0

:ERROREXIT
ECHO アップデートに失敗しました.
PAUSE

CD %WD%
EXIT 1
