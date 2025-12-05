# Run Tomasulo GUI Simulator
Write-Host "Building project..." -ForegroundColor Green
mvn clean compile

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Starting GUI..." -ForegroundColor Green
mvn javafx:run
