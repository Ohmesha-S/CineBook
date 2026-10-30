Write-Host "Starting CineLuxe Java Console Client (TCP Socket)..." -ForegroundColor Cyan
& ".\mvnw.cmd" compile exec:java "-Dexec.mainClass=com.movieticket.client.CinemaClient"
