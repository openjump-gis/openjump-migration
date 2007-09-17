@echo off
set OLD_DIR=%CD%
set JUMP_HOME=%~dp0..%
set JAVA_OPTS=-Xms256M -Xmx256M -Dlog4j.configuration=file:conf\log4j.xml "-Djump.home=%JUMP_HOME%"

cd %JUMP_HOME%
set LIB=lib

set CLASSPATH=.
set CLASSPATH=conf;%CLASSPATH%
set CLASSPATH=lib\ext;%CLASSPATH%

for %%i in ("lib\*.jar") do call "%JUMP_HOME%\bin\lcp.bat" %%i
for %%i in ("lib\*.zip") do call "%JUMP_HOME%\bin\lcp.bat" %%i

set PATH=%PATH%;%LIB%\ext

set JUMP_OPTS=-properties bin\workbench-properties.xml -plug-in-directory "%LIB%\ext"
start javaw -cp "%CLASSPATH%" %JAVA_OPTS% com.vividsolutions.jump.workbench.JUMPWorkbench %JUMP_OPTS%

cd %OLD_DIR%
