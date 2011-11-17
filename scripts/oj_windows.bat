@echo off
rem -- Detect current dir and OJ home --
set OLD_DIR=%CD%
set JUMP_HOME=%~dp0..%

rem -- uncomment to save settings and log to user profile, quote if env vars might contain spaces --
rem -- if unset defaults to JUMP_HOME/bin/ --
rem set SETTINGS_HOME="%HOMEDRIVE%%HOMEPATH%"\openjump

rem -- uncomment to manually set java home, don't use quotes --
rem set JAVA_HOME=G:\path\to\a\specific\<jre|jdk>-1.<5|6>

rem -- uncomment to use 'java' for console output, if unset defaults to 'javaw' for background jre  --
rem set JAVA_BIN=java

rem -- set some java runtime options here, initialize empty --
set JAVA_OPTS=

rem --- uncomment and change your language/country here to overwrite OS locale setting ---
rem set JAVA_OPTS=%JAVA_OPTS% -Duser.language=de -Duser.country=DE

rem --- change your memory configuration here - Xms is initial size, Xmx is maximum size, ---
rem --- values are ##M for ## Megabytes, ##G for ## Gigabytes ---
set JAVA_OPTS=%JAVA_OPTS% -Xms64M -Xmx512M

rem -- find java runtime --
  rem --- default to javaw ---
  if "%JAVA_BIN%"=="" set JAVA_BIN=javaw

  rem --- fallback to plain bin name, just in case ---
  set JAVA=%JAVA_BIN%

  rem --- if no java home & java bin in path, replace fallback entry ---
  if "%JAVA_HOME%"=="" (
      @for %%i in (%JAVA_BIN%.exe) do @if NOT "%%~$PATH:i"=="" set JAVA=%%~$PATH:i
  )

  rem --- java home definition overwrites all ---
  if NOT "%JAVA_HOME%"=="" set JAVA=%JAVA_HOME%\bin\%JAVA_BIN%

rem -- show java version (for debugging) --
for %%F in ("%JAVA%") do set dirname=%%~dpF
"%dirname%java" -version

rem -- Change to jump home dir --
rem -- NOTE: mount UNC paths to a local drive for this --
cd /D %JUMP_HOME%

set LIB=lib

rem -- setup native lib paths
set NATIVE=%LIB%\native
if DEFINED ProgramFiles(x86) set X64=64
rem --- XP Version 5.x ---
for /f "delims=" %%v in ('ver^|findstr /C:"Version 5"') do (
  set "ID=xp"
)
rem --- Vista Version 6.0 ---
for /f "delims=" %%v in ('ver^|findstr /C:"Version 6.0"') do (
  set "ID=vista"
)
rem --- 7 Version 6.1 ---
for /f "delims=" %%v in ('ver^|findstr /C:"Version 6.1"') do (
  set "ID=seven"
)
rem -- add native as fallthrough and lib\ext the legacy value --
set "NATIVEPATH=%NATIVE%\%ID%%X64%;%NATIVE%\%ID%;%NATIVE%"
set "PATH=%PATH%;%NATIVEPATH%;%LIB%\ext"

echo %PATH%

rem -- set classpath --
set CLASSPATH=.;bin;conf

for %%i in ("%LIB%\*.jar" "%LIB%\*.zip" "%NATIVE%\%ID%%X64%\*.jar" "%NATIVE%\%ID%\*.jar" "%NATIVE%\*.jar") do (
  set jarfile=%%i

  rem If we append to a variable inside the for, only the last entry will
  rem be kept. So append to the variable outside the for.
  rem See http://www.experts-exchange.com/Operating_Systems/MSDOS/Q_20561701.html.
  rem [Jon Aquino]

  call :setclass
)

echo %CLASSPATH%

rem -- set settings home if none given, use [] for if to survive quotes in env var --
if [%SETTINGS_HOME%]==[] set SETTINGS_HOME=.\bin

rem -- essential options, don't change unless you know what you're doing --
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j.configuration=%SETTINGS_HOME%\log4j.xml -Djump.home="%JUMP_HOME%"

rem -- set default app options --
set JUMP_OPTS=-default-plugins bin\default-plugins.xml -properties %SETTINGS_HOME%\workbench-properties.xml -plug-in-directory "%LIB%\ext"

rem -- disconnect javaw from console by using start --
rem -- note: title is needed or start won't accept quoted path to java binary (protect spaces in javapath) --
if /i "%JAVA_BIN%"=="javaw" ( set START=start "" ) else ( set START= )
%START% "%JAVA%" -cp "%CLASSPATH%" %JAVA_OPTS% com.vividsolutions.jump.workbench.JUMPWorkbench %JUMP_OPTS%

cd /D %OLD_DIR%

rem -- give user a chance to see console output if we are in console mode but the app finished already
if /i NOT "%JAVA_BIN%"=="javaw" pause

goto :eof

:setclass
set CLASSPATH=%CLASSPATH%;%jarfile%
set jarfile=
goto :eof

:eof
