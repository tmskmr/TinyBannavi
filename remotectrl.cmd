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
	ECHO Javaがみつかりません。以下を確認してください。
	ECHO.
	ECHO ①Javaがインストールされているか。
	ECHO ②%JEXE%がPATH環境変数で指定される場所に存在しているか。
	ECHO.
	PAUSE
	GOTO :EOF
)
ECHO 次のJavaが使用されます："%JAVA%"

START "RemoCtrl" "%JAVA%" -cp bin -Dfile.encoding=UTF-8 %JAVA_OPTS% remoteCtrl.Viewer
GOTO :EOF

:ERROREXIT
ECHO 実行できません.
PAUSE
