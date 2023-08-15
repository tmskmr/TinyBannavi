@ECHO OFF

CD /D %~dp0

IF NOT EXIST .\bin\tainavi\Viewer.class GOTO ERROREXIT

REM JavaRuntimeのバージョンアップ

IF EXIST jre6 THEN REN jre6 jre
IF EXIST jre (
	IF EXIST jre.new (
		ECHO JREをバージョンアップします.
		REN jre jre.old
		REN jre.new jre
	)
)

REM バイナリのバージョンアップ

IF EXIST bin.new (
	ECHO バイナリ[bin]と設定ファイル[env]を更新します.
	IF EXIST bin.old REN bin.old bin.old2
	REN bin bin.old
	REN bin.new bin
)

REM 使用するJavaを検索する

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
	ECHO Javaがみつかりません。以下を確認してください。
	ECHO.
	ECHO ①Javaがインストールされているか。
	ECHO ②%JEXE%がPATH環境変数で指定される場所に存在しているか。
	ECHO.
	PAUSE
	GOTO :EOF
)
ECHO 次のJavaが使用されます："%JAVA%"

REM ヒープの確保可能な最大サイズを調べる

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
	ECHO 予約可能なヒープサイズが%MAXHEAP%を下回りました.
	ECHO メモリが不足しているように思われます.
	PAUSE
	GOTO :EOF
)

REM IF ERRORLEVEL x は、ERRORLEVEL==xではなくてERRORLEVEL>=xなんだそうな。知らんわ！ｗｗｗ
REM "()"内はローカルスコープになってるみたい。知らんなー…

:RUNTAI
ECHO ★★★　TaiNavi.exeからの起動に変更をお願いします　★★★
ECHO ★★★　ウィルスチェックを忘れずに！ 　　　　　　　★★★
ECHO.
ECHO 利用可能な最大ヒープサイズ：%MAXHEAP%
ECHO.
ECHO タイニー番組ナビゲータを起動中です.　※この窓は１０秒で閉じます.
START "TaiNavi" "%JAVA%" -Xrs -Xms64m -Xmx%MAXHEAP% -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Tokyo tainavi.Viewer -l %*
PING localhost -n 10 > NUL
GOTO :EOF

REM 引数のパスのみを取得するサブルーチン

:BASENAME
SET RESULT=%~dp1
GOTO :EOF

:ERROREXIT
ECHO 実行できません.
PAUSE
