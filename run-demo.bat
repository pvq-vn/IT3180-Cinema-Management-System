@echo off
chcp 65001 >nul
echo ============================================================================
echo   CINEHUB CMS - HỆ THỐNG WEBSITE QUẢN LÝ VA ĐẶT VÉ RẠP CHIẾU PHIM (IT3180)
echo   Nhóm 3 (HUST) - Giảng viên hướng dẫn: TS. Nguyễn Nhất Hải
echo ============================================================================

set H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\1.4.200\h2-1.4.200.jar
if not exist out mkdir out

echo [1/3] Đang biên dịch mã nguồn Java 21 (UTF-8)...
javac -encoding UTF-8 -cp "%H2_JAR%" -d out src\main\java\com\cinehub\models\*.java src\main\java\com\cinehub\utils\*.java src\main\java\com\cinehub\services\*.java src\main\java\com\cinehub\controllers\*.java src\main\java\com\cinehub\CineHubApplication.java test\com\cinehub\CineHubTestRunner.java
if errorlevel 1 (
    echo [LỖI] Biên dịch thất bại!
    exit /b 1
)

echo [2/3] Đang thực thi bộ Kiểm thử tự động (White-box CFG, Black-box & Concurrency R01)...
java -Dfile.encoding=UTF-8 -cp "out;%H2_JAR%" com.cinehub.CineHubTestRunner

echo [3/3] Đang khởi động Máy chủ Web MVC tại http://localhost:8080 ...
java -Dfile.encoding=UTF-8 -cp "out;%H2_JAR%" com.cinehub.CineHubApplication

