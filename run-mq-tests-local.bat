@echo off
REM ================================================================
REM IBM MQ Test Runner - Local Environment (Windows)
REM ================================================================

echo ================================================
echo IBM MQ Test Runner - Local Environment
echo ================================================

REM Set environment variables for local testing
set MQ_HOST=localhost
set MQ_PORT=1414
set MQ_QUEUE_MANAGER=QM1
set MQ_CHANNEL=DEV.ADMIN.SVRCONN
set MQ_USER=admin
set MQ_PASSWORD=passw0rd
set MQ_CLIENT_ID=LOCAL_TEST_CLIENT
set MQ_REQUEST_QUEUE=DEV.QUEUE.1
set MQ_RESPONSE_QUEUE=DEV.QUEUE.2
set MQ_DLQ=DEV.DEAD.LETTER.QUEUE
set MQ_TEST_QUEUE=TEST.QUEUE

echo Building executable JAR...
call mvn clean package -DskipTests

if not exist "mq-test-utils\target\mq-test-utils-1.0.0-executable.jar" (
    echo ERROR: JAR file not found
    pause
    exit /b 1
)

echo ================================================
echo Running MQ Tests...
echo ================================================
echo MQ Host: %MQ_HOST%:%MQ_PORT%
echo Queue Manager: %MQ_QUEUE_MANAGER%
echo Channel: %MQ_CHANNEL%
echo Test Queue: %MQ_TEST_QUEUE%
echo ================================================

java -jar "mq-test-utils\target\mq-test-utils-1.0.0-executable.jar" --spring.profiles.active=tandem

if %ERRORLEVEL% EQU 0 (
    echo ================================================
    echo SUCCESS: MQ Tests completed successfully!
    echo ================================================
) else (
    echo ================================================
    echo ERROR: MQ Tests failed with exit code: %ERRORLEVEL%
    echo ================================================
)

pause