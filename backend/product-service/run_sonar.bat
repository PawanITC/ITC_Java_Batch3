@echo off
cd "C:\ITC project\Funkart E-commerce-app\ITC_Java_Batch3\backend\product-service"
docker run --rm -v "C:\ITC project\Funkart E-commerce-app\ITC_Java_Batch3\backend\product-service:/usr/src" -w /usr/src sonarsource/sonar-scanner-cli
pause
