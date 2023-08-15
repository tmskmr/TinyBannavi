@ECHO OFF

CD /D %~dp0

IF NOT EXIST .\bin\tainavi\Viewer.class GOTO ERROREXIT

REM JavaRuntime�̃o�[�W�����A�b�v

IF EXIST jre6 THEN REN jre6 jre
IF EXIST jre (
	IF EXIST jre.new (
		ECHO JRE���o�[�W�����A�b�v���܂�.
		REN jre jre.old
		REN jre.new jre
	)
)

REM �o�C�i���̃o�[�W�����A�b�v

IF EXIST bin.new (
	ECHO �o�C�i��[bin]�Ɛݒ�t�@�C��[env]���X�V���܂�.
	IF EXIST bin.old REN bin.old bin.old2
	REN bin bin.old
	REN bin.new bin
)

REM �g�p����Java����������

SET JPATH=%PATH%;C:\Program Files\Java\jre6\bin;C:\Program Files (x86)\Java\jre6\bin;C:\Program Files\Java\jre7\bin;C:\Program Files (x86)\Java\jre7\bin
SET JEXE=javaw.exe
SET JAVA=
SET CLASSPATH=bin;javamail\mail.jar;javamail\activation.jar;calendar\gdata-calendar-1.0.jar;calendar\gdata-client-1.0.jar;skin\*
IF EXIST jre (
	SET JAVA=jre\bin\javaw.exe
) ELSE (
	FOR %%I IN ( %JEXE% ) DO (
		SET JAVA=%%~$JPATH:I
	)
)
IF NOT EXIST "%JAVA%" (
	ECHO.
	ECHO Java���݂���܂���B�ȉ����m�F���Ă��������B
	ECHO.
	ECHO �@Java���C���X�g�[������Ă��邩�B
	ECHO �A%JEXE%��PATH���ϐ��Ŏw�肳���ꏊ�ɑ��݂��Ă��邩�B
	ECHO.
	PAUSE
	GOTO :EOF
)
ECHO ����Java���g�p����܂��F"%JAVA%"

REM �q�[�v�̊m�ۉ\�ȍő�T�C�Y�𒲂ׂ�

CALL :BASENAME "%JAVA%"
SET JAVACMD=%RESULT%java.exe
SET MAXHEAP=1024m
"%JAVACMD%" -Xmx%MAXHEAP% -version > NUL 2> NUL
IF NOT ERRORLEVEL 1 (
	GOTO :RUNTAI
)
SET MAXHEAP=768m
"%JAVACMD%" -Xmx%MAXHEAP% -version > NUL 2> NUL
IF NOT ERRORLEVEL 1 (
	GOTO :RUNTAI
)
SET MAXHEAP=512m
"%JAVACMD%" -Xmx%MAXHEAP% -version > NUL 2> NUL
IF NOT ERRORLEVEL 1 (
	GOTO :RUNTAI
)
SET MAXHEAP=256m
"%JAVACMD%" -Xmx%MAXHEAP% -version  > NUL 2> NUL
IF ERRORLEVEL 1 (
	ECHO �\��\�ȃq�[�v�T�C�Y��%MAXHEAP%�������܂���.
	ECHO ���������s�����Ă���悤�Ɏv���܂�.
	PAUSE
	GOTO :EOF
)

REM IF ERRORLEVEL x �́AERRORLEVEL==x�ł͂Ȃ���ERRORLEVEL>=x�Ȃ񂾂����ȁB�m����I������
REM "()"���̓��[�J���X�R�[�v�ɂȂ��Ă�݂����B�m���ȁ[�c

:RUNTAI
ECHO �������@TaiNavi.exe����̋N���ɕύX�����肢���܂��@������
ECHO �������@�E�B���X�`�F�b�N��Y�ꂸ�ɁI �@�@�@�@�@�@�@������
ECHO.
ECHO ���p�\�ȍő�q�[�v�T�C�Y�F%MAXHEAP%
ECHO.
ECHO �^�C�j�[�ԑg�i�r�Q�[�^���N�����ł�.�@�����̑��͂P�O�b�ŕ��܂�.
START "TaiNavi" "%JAVA%" -Xrs -Xms64m -Xmx%MAXHEAP% -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo tainavi.Viewer -l %*
PING localhost -n 10 > NUL
GOTO :EOF

REM �����̃p�X�݂̂��擾����T�u���[�`��

:BASENAME
SET RESULT=%~dp1
GOTO :EOF

:ERROREXIT
ECHO ���s�ł��܂���.
PAUSE
