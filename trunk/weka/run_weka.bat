@echo off
cd /d D:\Vs Code\Weka_SPADE_Demo\weka\trunk\weka

set CP=dist\weka-dev-3.9.7-SNAPSHOT.jar

for %%f in (lib\*.jar) do (
    set CP=!CP!;lib\%%f
)

REM Enable delayed expansion
setlocal EnableDelayedExpansion

set CP=dist\weka-dev-3.9.7-SNAPSHOT.jar
for %%f in (lib\*.jar) do (
    set CP=!CP!;lib\%%~nxf
)

java -cp "%CP%" weka.gui.GUIChooser

pause