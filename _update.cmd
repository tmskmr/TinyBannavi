@ECHO OFF

SET WD=%CD%

CD %~dp0

ECHO ...�A�b�v�f�[�g�悪���݂��邩�m�F���܂�.
IF NOT EXIST ..\..\bin\tainavi\Viewer.class GOTO ERROREXIT

@REM JavaRuntime���R�s�[
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
	ECHO ...JavaRuntime���R�s�[���܂�.
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
	ECHO ...�o�C�i�����R�s�[���܂�.
	MOVE /Y bin.new ..\..\
)

ECHO ...�V�K�ɒǉ����ꂽ�ݒ�t�@�C�����R�s�[���܂�.
IF EXIST ..\..\env.old2\ RD /S /Q ..\..\env.old2\
IF EXIST ..\..\env.old\ REN ..\..\env.old\ env.old2
MD ..\..\env.old\
XCOPY ..\..\env ..\..\env.old\ /S /E /Q /Y
FOR %%i IN (env\*.*) DO (
	IF NOT EXIST "..\..\env\%%~nxi" COPY "%%i" ..\..\env\
)

ECHO ...�V�K�ɒǉ����ꂽ�A�C�R�����R�s�[���܂�.
FOR %%i IN (icon\*.*) DO (
	IF NOT EXIST "..\..\icon\%%~nxi" COPY "%%i" ..\..\icon\
)

ECHO ...�X�N���v�g���R�s�[���܂�.
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

ECHO ...�e�L�X�g���R�s�[���܂�.
FOR %%i IN (*.txt) DO (
	COPY "%%i" ..\..\
)

CD %WD%
EXIT 0

:ERROREXIT
ECHO �A�b�v�f�[�g�Ɏ��s���܂���.
PAUSE

CD %WD%
EXIT 1
