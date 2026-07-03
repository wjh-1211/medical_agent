@ECHO OFF
SETLOCAL
SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
SET MAVEN_VERSION=3.9.11
SET DIST_NAME=apache-maven-%MAVEN_VERSION%
SET DIST_DIR=%WRAPPER_DIR%\%DIST_NAME%
SET ARCHIVE=%WRAPPER_DIR%\%DIST_NAME%-bin.zip
SET URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/%DIST_NAME%-bin.zip

IF NOT EXIST "%DIST_DIR%\bin\mvn.cmd" (
  IF NOT EXIST "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  IF NOT EXIST "%ARCHIVE%" powershell -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ARCHIVE%'"
  powershell -Command "Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%WRAPPER_DIR%' -Force"
)

CALL "%DIST_DIR%\bin\mvn.cmd" %*
