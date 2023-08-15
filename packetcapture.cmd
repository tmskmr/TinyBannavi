SET JAVA=java
IF EXIST JRE6 SET JAVA=JRE6\BIN\JAVA
%JAVA% -cp bin -Dfile.encoding=UTF-8 httpDump.Viewer
