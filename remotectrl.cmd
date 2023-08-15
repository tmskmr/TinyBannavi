@ECHO OFF

CD /D %~dp0

IF NOT EXIST .\bin\remoteCtrl\Viewer.class GOTO ERROREXIT

REM SET JAVA_OPTS=-Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
SET JAVA_OPTS=-Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
SET JPATH=%PATH%;C:\Program Files\Java\jre6\bin;C:\Program Files (x86)\Java\jre6\bin;C:\Program Files\Java\jre7\bin;C:\Program Files (x86)\Java\jre7\bin
SET JEXE=javaw.exe
SET JAVA=
IF EXIST JRE6 (
	SET JAVA=jre6\bin\javaw.exe
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

START "RemoCtrl" "%JAVA%" -cp bin -Dfile.encoding=UTF-8 %JAVA_OPTS% remoteCtrl.Viewer
GOTO :EOF

:ERROREXIT
ECHO ���s�ł��܂���.
PAUSE
