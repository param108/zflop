@echo off

SET OPTS=-use-network=false

for /R . %%f in (*.mxml) do  ..\..\..\bin\mxmlc.exe %OPTS% "%%f"
