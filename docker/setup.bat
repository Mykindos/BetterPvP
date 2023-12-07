@echo off

REM Attempt to start docker daemon. (Windows)
docker info > NUL 2>&1
if errorlevel 1 (
    echo Docker daemon is not running.
    echo Please attempt to start Docker Desktop for the daemon to run.
    echo Press any key to continue...
    pause > NUL
    exit /b
)

REM Print 'Setting up environment' to the console.
echo Setting up environment

REM Check if docker-compose.yml file exists.
if exist docker-compose.yml (
    REM Run the docker-compose.yml file.
    REM -d flag runs the containers in the background.
    docker-compose up -d
) else (
    echo docker-compose.yml file not found in this directory.
)

REM Wait for user to press any key to continue.
echo Press any key to continue...
pause > NUL