CD /D %~dp0
SET JAVA=javaw
IF EXIST JRE6 SET JAVA=JRE6\BIN\JAVA
START %JAVA% -cp bin -Dfile.encoding=UTF-8 statusView.Viewer
