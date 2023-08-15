CD /D %~dp0
SET JAVA=javaw
SET CLASSPATH=bin
IF EXIST JRE6 SET JAVA=JRE6\BIN\JAVAW
START %JAVA% -Dfile.encoding=UTF-8 taiSync.Viewer %*
