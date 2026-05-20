# Job Queue System Startup Script


Write-Host "Starting Job Queue System..." -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan


if (Test-Path ".\apache-maven-3.9.9\bin\mvn.cmd") {
    Write-Host "Using local Maven installation..." -ForegroundColor Green
    & ".\apache-maven-3.9.9\bin\mvn.cmd" clean javafx:run
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Using system Maven..." -ForegroundColor Green
    mvn clean javafx:run
} else {
    Write-Host "ERROR: Maven not found!" -ForegroundColor Red
    Write-Host "Please ensure Maven is installed or available in the apache-maven-3.9.9 directory." -ForegroundColor Yellow
    exit 1
}
