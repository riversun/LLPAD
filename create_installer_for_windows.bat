REM create windows installer package(runtime included) for LLPAD
REM In order to execute this batch file, java 8 or later needs to be installed
REM In case creating exe installer, Download Inno Setup 5 or later from http://www.jrsoftware.org and add it to the PATH.
REM In case creating msi installer, Download WiX 3.0 or later from http://wix.sf.net and add it to the PATH.

echo off

set APP_VERSION=0.5.0


IF "%JAVA_HOME%" == "" (
    echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation.
    exit /B
)

REM change dir to project dir where this bat is placed.
cd %~dp0

set JAR_NAME=llpad-%APP_VERSION%-jar-with-dependencies.jar
set JAR_FULLPATH=%CD%\target\%JAR_NAME%
set PACKAGE_PATH=%CD%\package
set PACKAGE_RESOURCE_PATH=%CD%\package_resource
set INFILE_PATH=%PACKAGE_PATH%\infiles
set OUTFILE_PATH=%PACKAGE_PATH%\outfiles
set PACKAGE_FOR_WINDOWS_PATH=%PACKAGE_PATH%\windows

IF NOT EXIST "%JAR_FULLPATH%" (
echo The jar file "%JAR_FULLPATH%" not found.Please run "mvn install" first. Or APP_VERSION %APP_VERSION% may be incorrect.
echo The version code must be same as project/version in the POM.xml
    exit /B
) 


rmdir /s /q %PACKAGE_PATH%
mkdir %PACKAGE_PATH%

rmdir %INFILE_PATH%
mkdir %INFILE_PATH%

rmdir %OUTFILE_PATH%
mkdir %OUTFILE_PATH%

rmdir %PACKAGE_FOR_WINDOWS_PATH%
mkdir %PACKAGE_FOR_WINDOWS_PATH%

copy %JAR_FULLPATH% %INFILE_PATH%
copy %PACKAGE_RESOURCE_PATH%\license.txt %INFILE_PATH%

REM Create a directory "[PJ_ROOT]/package/windows" and put app icon named "[APPNAME].ico" it's a rule of javapackager.
copy %PACKAGE_RESOURCE_PATH%\LLPAD.ico %PACKAGE_FOR_WINDOWS_PATH%


"%JAVA_HOME%\bin\javapackager" ^
-deploy ^
-native exe ^
-BappVersion=%APP_VERSION% ^
-BshortcutHint=true ^
-Bvendor="Riversun" ^
-Bwin.menuGroup="LLPAD" ^
-BsystemWide=true ^
-srcdir package/infiles ^
-outdir package/outfiles ^
-outfile LLPAD_OUT ^
-appclass org.riversun.llpad.AppMain ^
-BlicenseFile=license.txt ^
-name "LLPAD" ^
-title "LLPAD" ^
-Bruntime="%JAVA_HOME%\jre" ^
-srcfiles %JAR_NAME%;license.txt ^
-v

echo package creation finished.

pause


