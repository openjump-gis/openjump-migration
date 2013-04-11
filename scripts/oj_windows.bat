@echo off & setlocal
rem call :init

rem -- Save current working dir and OJ home --
set OLD_DIR=%CD%
set JUMP_HOME=%~dp0..%

rem -- uncomment to save settings and log to user profile ---
rem -- defaults to 'JUMP_HOME', if former is not writable 'userprofile/.openjump' --
rem set SETTINGS_HOME="%HOMEDRIVE%%HOMEPATH%"\.openjump

rem -- uncomment to manually set java home --
rem set JAVA_HOME=G:\path\to\a\specific\<jre|jdk>-1.<5|6>

rem -- uncomment to use 'java' for console output, if unset defaults to 'javaw' for background jre  --
rem set JAVA_BIN=java

rem -- set some java runtime options here, initialize empty --
set JAVA_OPTS=

rem --- uncomment and change your language/country here to overwrite OS locale setting ---
rem set JAVA_OPTS=%JAVA_OPTS% -Duser.language=de -Duser.country=DE

rem --- enforce a memory configuration here, default value is the ---
rem --- size of ram or 1GB for 32bit jre's, whichever is bigger   ---
rem --- Xms is initial size, Xmx is maximum size, values          ---
rem --- are ##M for ## Megabytes, ##G for ## Gigabytes            ---
rem set JAVA_MEM=-Xms64M -Xmx512M

rem --- uncomment and change your http proxy settings here
rem set JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyHost=myproxyserver.com -Dhttp.proxyPort=80 -Dhttp.nonProxyHosts="localhost|host.mydomain.com"

rem --- if the proxy server requires authentication uncomment and edit also these
rem set JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyUser=username -Dhttp.proxyPass=password

rem -- dequote path entries, just to be sure --
call :dequote %PATH%
set "PATH=%unquoted%"

rem -- dequote java_home, later on we assume it's unquoted --
call :dequote %JAVA_HOME%
set "JAVA_HOME=%unquoted%"

rem -- find java runtime --
  set JAVA=
  rem --- default to javaw ---
  if "%JAVA_BIN%"=="" set JAVA_BIN=javaw

  rem --- search binary in path ---
  @for %%i in (%JAVA_BIN%.exe) do @if NOT "%%~$PATH:i"=="" set JAVA=%%~$PATH:i

  rem --- we might be on amd64 having only x86 jre installed ---
  if "%JAVA%"=="" if DEFINED ProgramFiles(x86) if NOT "%PROCESSOR_ARCHITECTURE%"=="x86" (
    rem --- restart the batch in x86 mode---
    echo Warning: No java interpreter found in path.
    echo Retry using Wow64 filesystem [32bit environment] redirection.
    %SystemRoot%\SysWOW64\cmd.exe /c %0 %*
    exit /b %ERRORLEVEL%
  )

  rem --- if unset fall back to plain bin name, just in case ---
  if "%JAVA%"=="" set JAVA=%JAVA_BIN%
  
  rem --- java home definition overwrites all ---
  if NOT "%JAVA_HOME%"=="" set "JAVA=%JAVA_HOME%\bin\%JAVA_BIN%"

rem -- show java version (for debugging) --
for %%F in ("%JAVA%") do set "dirname=%%~dpF"
echo Using '%JAVA_BIN%' found in '%dirname%'
rem "%dirname%java" -version
SET concat=
for /f "tokens=* delims=" %%i in ('"%dirname%java" -version 2^>^&1') do call :concat "; " %%i
set "JAVA_VERSIONSTRING=%concat%"
echo %JAVA_VERSIONSTRING%

rem -- detect if java is 64bit --
for /f "delims=" %%v in ('echo "%JAVA_VERSIONSTRING%"^|findstr /I "64-Bit"') do (
  set JAVA_X64=64
)

rem -- Change to jump home dir --
rem -- NOTE: mount UNC paths to a local drive for this --
cd /D "%JUMP_HOME%"

rem -- Uninstall if asked nicely ---
if "%1"=="--uninstall" ( 
  "%JAVA%" -jar .\uninstall\uninstaller.jar
  goto:eof
)

set LIB=lib

rem -- setup native lib paths
set NATIVE=%LIB%\native
if DEFINED ProgramFiles(x86) set X64=64
rem command ver example outputs
rem  german win7 "Microsoft Windows [Version 6.1.7601]"
rem  finnish xp  "Microsoft Windows XP [versio 5.1.2600]"
rem  french  xp  "Microsoft Windows XP [version 5.1.2600]"
rem --- XP Version 5.x ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 5[0-9\.]*]"') do (
  set "ID=xp"
)
rem --- Vista Version 6.0 ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 6.0.[0-9\.]*]"') do (
  set "ID=vista"
)
rem --- 7 Version 6.1 ---
for /f "delims=" %%v in ('ver^|findstr /REC:" 6.1.[0-9\.]*]"') do (
  set "ID=seven"
)
rem -- add native as fallthrough and lib\ext the legacy value --
set "NATIVEPATH=%NATIVE%\%ID%%X64%;%NATIVE%\%ID%;%NATIVE%"
set "PATH=%NATIVEPATH%;%LIB%\ext;%PATH%"

rem -- debug info --
if /i NOT "%JAVA_BIN%"=="javaw" echo ---PATH--- & echo %PATH%

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

rem -- debug info --
if /i NOT "%JAVA_BIN%"=="javaw" echo ---CLASSPATH--- & echo %CLASSPATH%

rem -- set settings home/log dir if none given --
  rem --- dequote settings_home, later on we assume it's unquoted ---
  call :dequote %SETTINGS_HOME%
  set "SETTINGS_HOME=%unquoted%"

  rem --- set default or create missing folder ---
  rem --- ATTENTION: logdir requires a trailing backslash for concatenation in log4j.xml ---
  if NOT DEFINED SETTINGS_HOME (
    rem ---- check if jumphome is writable ----
    copy /Y NUL "%JUMP_HOME%\.writable" > NUL 2>&1 && set WRITEOK=1
    if DEFINED WRITEOK ( 
      rem ---- an absolute settings_home allows file:/// for log4j conf ----
      set "SETTINGS_HOME=%JUMP_HOME%"
     ) else (
      set "SETTINGS_HOME=%HOMEDRIVE%%HOMEPATH%\.openjump"
    )
  )
  set "LOG_DIR=%SETTINGS_HOME%/"
  rem -- debug info --
  if /i NOT "%JAVA_BIN%"=="javaw" echo ---Save logs ^& state to--- & echo %SETTINGS_HOME%
  rem --- create folder if not existing ---
  if NOT EXIST "%SETTINGS_HOME%" mkdir "%SETTINGS_HOME%"

rem -- look if we have a custom logging configuration in settings --
if EXIST "%SETTINGS_HOME%\log4j.xml" (
  rem --- log4j can't seem to find absolute path without the file:/// prefix ---
  set "LOG4J_CONF=file:///%SETTINGS_HOME%\log4j.xml"
) else (
  set LOG4J_CONF=.\bin\log4j.xml
)

rem -- add memory settings --
if NOT DEFINED JAVA_MEM (
  call :memory
)

rem -- essential options, don't change unless you know what you're doing --
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j.configuration="%LOG4J_CONF%" -Dlog.dir="%LOG_DIR%" -Djump.home="%JUMP_HOME%" %JAVA_MEM%

rem -- set default app options --
set JUMP_OPTS=-default-plugins bin\default-plugins.xml -state "%SETTINGS_HOME%" -plug-in-directory "%LIB%\ext"
rem --- workbench-properties.xml is used to manually load plugins (ISA uses this) ---
if EXIST "bin\workbench-properties.xml" set "JUMP_OPTS=%JUMP_OPTS% -properties bin\workbench-properties.xml"

rem -- disconnect javaw from console by using start --
rem -- note: title is needed or start won't accept quoted path to java binary (protect spaces in javapath) --
if /i "%JAVA_BIN%"=="javaw" ( set START=start "" ) else ( set START= )
if /i NOT "%JAVA_BIN%"=="javaw" echo ---Start OJ---
 %START% "%JAVA%" -cp "%CLASSPATH%" %JAVA_OPTS% com.vividsolutions.jump.workbench.JUMPWorkbench %JUMP_OPTS% %*

cd /D %OLD_DIR%

rem -- give user a chance to see console output if we are in console mode but the app finished already
if /i NOT "%JAVA_BIN%"=="javaw" pause

goto:eof

:init
rem --- reset variables ---
set JAVA=
set JAVA_MEM=
set JAVA_BIN=
set SETTINGS_HOME=
goto:eof

:setclass
set CLASSPATH=%CLASSPATH%;%jarfile%
set jarfile=
goto:eof

:dequote
SETLOCAL enabledelayedexpansion
set string=%*
if DEFINED string set string=!string:"=!
ENDLOCAL & set unquoted=%string%
goto:eof

:concat
set "glue=%~1"
shift
set string=
:loop
set glue2=
if DEFINED string set "glue2= "
if NOT [%1]==[] (
  set "string=%string%%glue2%%1"
  shift
  goto :loop
)
if NOT "%concat%" == "" (
  set "string=%glue%%string%"
)
set "concat=%concat%%string%"
goto:eof

:memory
if /i NOT "%JAVA_BIN%"=="javaw" echo ---Detect maximum memory limit---

rem --- detect ram size, values are in kB ---
for /f "delims=" %%l in ('wmic os get FreePhysicalMemory^,TotalVisibleMemorySize /format:list') do >nul 2>&1 set "OS_%%l"
if NOT DEFINED OS_TotalVisibleMemorySize call :mem_failed Couldn't determine ram size.
rem --- use 100% of ram as default limit (1.124 is a factor to make java64 really use that much) ---
set /a "JAVA_XMX=%OS_TotalVisibleMemorySize%/1000*1124"
set /a "JAVA_XMX_X86=1024*1024"
set /a "JAVA_RAM_QUARTER=%OS_TotalVisibleMemorySize%/4"
rem --- a. cap to 1GB for 32bit jre ---
rem --- b. use xmx value if it fits into free space ---
rem --- c. use freemem value if bigger than 1/4 ram (jre default) ---
rem --- d. don't set, use jre default (works even though less than 1/4 ram might be free) ---

if NOT DEFINED JAVA_X64 if %JAVA_XMX% GTR %JAVA_XMX_X86% goto :use_cap
goto use_max
rem if %OS_FreePhysicalMemory% GEQ %JAVA_XMX%
rem if %OS_FreePhysicalMemory% GTR %JAVA_RAM_QUARTER% goto :use_free
rem if /i NOT "%JAVA_BIN%"=="javaw" echo Less than 1/4 ram free. Use jre default.
goto:eof

:use_cap
  rem --- if x86 cap is bigger than actual ram, use ram size instead ---
  if %JAVA_XMX_X86% GTR %OS_TotalVisibleMemorySize% call :use_max
  call :xmx %JAVA_XMX_X86%
  if /i NOT "%JAVA_BIN%"=="javaw" echo set %JAVA_MEM_STRING% ^(32 bit jre maximum^)
  goto:eof
:use_max
  call :xmx %JAVA_XMX%
  if /i NOT "%JAVA_BIN%"=="javaw" echo set %JAVA_MEM_STRING% ^(ram maximum^)
  goto:eof
:use_free
  call :xmx %OS_FreePhysicalMemory%
  if /i NOT "%JAVA_BIN%"=="javaw" call echo set %JAVA_MEM_STRING% ^(free memory^)
  goto:eof
:mem_failed
  if /i NOT "%JAVA_BIN%"=="javaw" call echo skipped because: %*
  goto:eof

:xmx
set /a "value=%1/1024"
set JAVA_MEM=-Xmx%value%M
set "JAVA_MEM_STRING=Xmx to %value%M"
goto:eof


:eof
