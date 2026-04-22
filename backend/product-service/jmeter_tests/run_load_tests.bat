@echo off
REM Load Testing Script for Product Service - Windows Version
REM This script runs all JMeter load tests and generates reports

setlocal enabledelayedexpansion

REM Configuration
set JMETER_HOME=C:\path\to\jmeter
set TEST_PLANS_DIR=jmeter_tests
set RESULTS_DIR=load_test_results
set BASE_URL=http://localhost:8080

REM Create results directory
if not exist "%RESULTS_DIR%" mkdir "%RESULTS_DIR%"

REM Generate timestamp
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set TIMESTAMP=%mydate%_%mytime%

echo.
echo ========================================
echo Product Service Load Testing Script
echo ========================================
echo.

REM Test 1: Category Load Test (Light Load)
echo --- Test 1: Category Endpoints (Light Load) ---
echo Running test with 50 threads, 30 second ramp-up, 300 second duration
"%JMETER_HOME%\bin\jmeter.bat" -n ^
    -t "%TEST_PLANS_DIR%\CategoryLoadTest.jmx" ^
    -l "%RESULTS_DIR%\CategoryLoadTest_%TIMESTAMP%.jtl" ^
    -j "%RESULTS_DIR%\CategoryLoadTest_%TIMESTAMP%.log" ^
    -Jthreads=50 ^
    -Jrampup=30 ^
    -Jduration=300 ^
    -Jbase_url=%BASE_URL% ^
    -e -o "%RESULTS_DIR%\CategoryLoadTest_%TIMESTAMP%_report"
if errorlevel 1 (
    echo FAILED: Category Load Test
    exit /b 1
)
echo Completed: Category Load Test
echo Report: %RESULTS_DIR%\CategoryLoadTest_%TIMESTAMP%_report\index.html
echo.

REM Test 2: Product Load Test (Medium Load)
echo --- Test 2: Product Endpoints (Medium Load) ---
echo Running test with 100 threads, 60 second ramp-up, 600 second duration
"%JMETER_HOME%\bin\jmeter.bat" -n ^
    -t "%TEST_PLANS_DIR%\ProductLoadTest.jmx" ^
    -l "%RESULTS_DIR%\ProductLoadTest_%TIMESTAMP%.jtl" ^
    -j "%RESULTS_DIR%\ProductLoadTest_%TIMESTAMP%.log" ^
    -Jthreads=100 ^
    -Jrampup=60 ^
    -Jduration=600 ^
    -Jbase_url=%BASE_URL% ^
    -e -o "%RESULTS_DIR%\ProductLoadTest_%TIMESTAMP%_report"
if errorlevel 1 (
    echo FAILED: Product Load Test
    exit /b 1
)
echo Completed: Product Load Test
echo Report: %RESULTS_DIR%\ProductLoadTest_%TIMESTAMP%_report\index.html
echo.

REM Test 3: Cart Load Test (Higher Load)
echo --- Test 3: Cart Workflow (Higher Load) ---
echo Running test with 200 threads, 120 second ramp-up, 600 second duration
"%JMETER_HOME%\bin\jmeter.bat" -n ^
    -t "%TEST_PLANS_DIR%\CartLoadTest.jmx" ^
    -l "%RESULTS_DIR%\CartLoadTest_%TIMESTAMP%.jtl" ^
    -j "%RESULTS_DIR%\CartLoadTest_%TIMESTAMP%.log" ^
    -Jthreads=200 ^
    -Jrampup=120 ^
    -Jduration=600 ^
    -Jbase_url=%BASE_URL% ^
    -e -o "%RESULTS_DIR%\CartLoadTest_%TIMESTAMP%_report"
if errorlevel 1 (
    echo FAILED: Cart Load Test
    exit /b 1
)
echo Completed: Cart Load Test
echo Report: %RESULTS_DIR%\CartLoadTest_%TIMESTAMP%_report\index.html
echo.

REM Test 4: Spike Test (Sudden Load)
echo --- Test 4: Spike Test (Sudden Load) ---
echo Running spike test with 500 threads, 10 second ramp-up, 120 second duration
"%JMETER_HOME%\bin\jmeter.bat" -n ^
    -t "%TEST_PLANS_DIR%\CartLoadTest.jmx" ^
    -l "%RESULTS_DIR%\SpikeTest_%TIMESTAMP%.jtl" ^
    -j "%RESULTS_DIR%\SpikeTest_%TIMESTAMP%.log" ^
    -Jthreads=500 ^
    -Jrampup=10 ^
    -Jduration=120 ^
    -Jbase_url=%BASE_URL% ^
    -e -o "%RESULTS_DIR%\SpikeTest_%TIMESTAMP%_report"
if errorlevel 1 (
    echo FAILED: Spike Test
    exit /b 1
)
echo Completed: Spike Test
echo Report: %RESULTS_DIR%\SpikeTest_%TIMESTAMP%_report\index.html
echo.

REM Generate summary report
echo ========================================
echo Generating Summary Report
echo ========================================
echo.

(
    echo Load Test Summary Report
    echo Generated: %DATE% %TIME%
    echo Product Service URL: %BASE_URL%
    echo.
    echo Test 1: Category Load Test
    echo   - Threads: 50
    echo   - Ramp-up: 30 seconds
    echo   - Duration: 300 seconds ^(5 minutes^)
    echo   - Report: CategoryLoadTest_%TIMESTAMP%_report/index.html
    echo.
    echo Test 2: Product Load Test
    echo   - Threads: 100
    echo   - Ramp-up: 60 seconds
    echo   - Duration: 600 seconds ^(10 minutes^)
    echo   - Report: ProductLoadTest_%TIMESTAMP%_report/index.html
    echo.
    echo Test 3: Cart Workflow Test
    echo   - Threads: 200
    echo   - Ramp-up: 120 seconds
    echo   - Duration: 600 seconds ^(10 minutes^)
    echo   - Report: CartLoadTest_%TIMESTAMP%_report/index.html
    echo.
    echo Test 4: Spike Test
    echo   - Threads: 500
    echo   - Ramp-up: 10 seconds ^(sudden load^)
    echo   - Duration: 120 seconds ^(2 minutes^)
    echo   - Report: SpikeTest_%TIMESTAMP%_report/index.html
    echo.
    echo Expected Performance Targets:
    echo - Average Response Time: less than 200ms
    echo - 95th Percentile: less than 500ms
    echo - Error Rate: less than 1 percent
    echo - Throughput: greater than 100 req/sec
    echo.
    echo Results Location: %CD%\%RESULTS_DIR%\
) > "%RESULTS_DIR%\LOAD_TEST_SUMMARY_%TIMESTAMP%.txt"

echo Summary report created: %RESULTS_DIR%\LOAD_TEST_SUMMARY_%TIMESTAMP%.txt
echo.
echo ========================================
echo All Load Tests Completed!
echo ========================================
echo Results saved to: %RESULTS_DIR%
echo View detailed reports in: %RESULTS_DIR%\*_report\index.html
echo.

endlocal

