# run.ps1 - Ejecutar cliente desktop en Windows
Write-Host "🚀 Compilando y ejecutando Cliente Desktop..." -ForegroundColor Green
Write-Host "🔗 Conectando a: localhost:3000" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Gray

mvn clean compile exec:java -Dexec.mainClass="com.broadcast.client.SimpleClient"