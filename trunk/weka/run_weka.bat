@echo off
REM Chuyển dictionary làm việc về thư mục chứa file .bat hiện tại
cd /d "%~dp0"

REM Bật tính năng nối chuỗi biến trong vòng lặp (Delayed Expansion)
setlocal EnableDelayedExpansion

REM Khởi tạo Classpath với file JAR chính của Weka
set CP=dist\weka-dev-3.9.7-SNAPSHOT.jar

REM Quét toàn bộ file .jar trong thư mục lib và đưa vào Classpath
for %%f in (lib\*.jar) do (
    set CP=!CP!;lib\%%~nxf
)

REM Chạy Weka GUIChooser với Classpath đã cấu hình
java -cp "!CP!" weka.gui.GUIChooser

pause