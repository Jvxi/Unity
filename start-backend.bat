@echo off
setlocal
if "%CAT_TOOL_JWT_SECRET%"=="" (
  echo CAT_TOOL_JWT_SECRET is required before starting the backend.
  exit /b 1
)
cd /d "%~dp0spring-server"
mvn.cmd spring-boot:run -DskipTests
