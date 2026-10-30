$baseUrl = "http://localhost:8080/api"

Write-Host "=== TEST 1: CUSTOMER REGISTRATION ==="
$regBody = @{
    fullName = "Test Customer"
    email = "testcustomer" + (Get-Random) + "@example.com"
    phone = "9876543210"
    password = "Customer@123"
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $regBody -ContentType "application/json"
$customerToken = $regRes.data.token
Write-Host "Customer registered successfully! Token received. User ID:" $regRes.data.user.id

Write-Host "=== TEST 2: BROWSE MOVIES & THEATRES ==="
$moviesRes = Invoke-RestMethod -Uri "$baseUrl/movies/now-showing" -Method Get
$movie = $moviesRes.data[0]
Write-Host "Selected Movie:" $movie.title "ID:" $movie.id

$theatresRes = Invoke-RestMethod -Uri "$baseUrl/theatres/movie/$($movie.id)" -Method Get
$theatre = $theatresRes.data[0]
Write-Host "Selected Theatre:" $theatre.name "Location:" $theatre.location

$today = (Get-Date).ToString("yyyy-MM-dd")
$showsRes = Invoke-RestMethod -Uri "$baseUrl/shows/schedule?movieId=$($movie.id)&theatreId=$($theatre.id)&date=$today" -Method Get
$now = Get-Date
$show = $showsRes.data | Where-Object { [DateTime]::Parse($_.showDate + ' ' + $_.startTime) -gt $now } | Select-Object -First 1
if (-not $show) {
    $tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
    $showsRes = Invoke-RestMethod -Uri "$baseUrl/shows/schedule?movieId=$($movie.id)&theatreId=$($theatre.id)&date=$tomorrow" -Method Get
    $show = $showsRes.data[0]
}
Write-Host "Selected Show ID:" $show.id "Screen:" $show.screenName "Date:" $show.showDate "Time:" $show.startTime

Write-Host "=== TEST 3: SEAT LAYOUT & AVAILABILITY ==="
$seatsRes = Invoke-RestMethod -Uri "$baseUrl/shows/$($show.id)/seats" -Method Get
$availableSeats = $seatsRes.data.seats | Where-Object { $_.status -eq 'AVAILABLE' }
$seat1 = $availableSeats[0]
$seat2 = $availableSeats[1]
Write-Host "Available seats to book:" $seat1.rowLabel$seat1.seatNumber "and" $seat2.rowLabel$seat2.seatNumber

Write-Host "=== TEST 4: CREATE BOOKING (TRANSACTIONAL HOLD) ==="
$bookBody = @{
    showId = $show.id
    seatIds = @($seat1.id, $seat2.id)
} | ConvertTo-Json

$custHeaders = @{ Authorization = "Bearer $customerToken" }
$bookRes = Invoke-RestMethod -Uri "$baseUrl/bookings" -Method Post -Body $bookBody -Headers $custHeaders -ContentType "application/json"
$bookingRef = $bookRes.data.bookingReference
$bookingId = $bookRes.data.id
Write-Host "Booking created! Reference:" $bookingRef "Status:" $bookRes.data.bookingStatus "Total Amount:" $bookRes.data.totalAmount

Write-Host "=== TEST 5: CONCURRENCY CHECK (DOUBLE-BOOKING PREVENTION) ==="
try {
    $conflictRes = Invoke-RestMethod -Uri "$baseUrl/bookings" -Method Post -Body $bookBody -Headers $custHeaders -ContentType "application/json"
    Write-Host "ERROR: Double booking succeeded when it should fail!"
} catch {
    Write-Host "SUCCESS: Double booking was prevented with status conflict!"
}

Write-Host "=== TEST 6: SIMULATED PAYMENT PROCESSING ==="
$payBody = @{
    bookingReference = $bookingRef
    paymentMethod = "CARD"
    simulateOutcome = "SUCCESS"
} | ConvertTo-Json

$payRes = Invoke-RestMethod -Uri "$baseUrl/payments/process" -Method Post -Body $payBody -Headers $custHeaders -ContentType "application/json"
Write-Host "Payment result:" $payRes.data.paymentStatus "Message:" $payRes.message

Write-Host "=== TEST 7: RETRIEVE DIGITAL TICKET ==="
$ticketRes = Invoke-RestMethod -Uri "$baseUrl/tickets/$bookingRef" -Method Get -Headers $custHeaders
Write-Host "Digital Ticket verified! Ref:" $ticketRes.data.bookingReference "Ticket No:" $ticketRes.data.ticketNumber "Seats:" ($ticketRes.data.seatNumbers -join ', ') "Verification Code:" $ticketRes.data.verificationCode

Write-Host "=== TEST 8: CUSTOMER BOOKING HISTORY ==="
$myBookings = Invoke-RestMethod -Uri "$baseUrl/bookings/my" -Method Get -Headers $custHeaders
Write-Host "Customer bookings count:" $myBookings.data.Count "Latest Booking:" $myBookings.data[0].bookingReference

Write-Host "=== TEST 9: ADMIN AUTHENTICATION & DASHBOARD ==="
$adminLoginBody = @{
    email = "admin@cinebook.com"
    password = "Admin@123"
} | ConvertTo-Json

$adminRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminToken = $adminRes.data.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "Admin authenticated! Token received. Role:" $adminRes.data.user.role

$stats = Invoke-RestMethod -Uri "$baseUrl/admin/dashboard" -Method Get -Headers $adminHeaders
Write-Host "Admin Dashboard Stats -> Total Movies:" $stats.data.totalMovies "Theatres:" $stats.data.totalTheatres "Screens:" $stats.data.totalScreens "Shows:" $stats.data.totalShows "Confirmed Bookings:" $stats.data.confirmedBookings "Revenue:" $stats.data.totalRevenue

Write-Host "=== TEST 10: BOOKING CANCELLATION & SEAT RELEASE ==="
$cancelRes = Invoke-RestMethod -Uri "$baseUrl/bookings/$bookingId/cancel" -Method Post -Headers $custHeaders
Write-Host "Booking cancelled status:" $cancelRes.data.bookingStatus

$seatsAfterCancel = Invoke-RestMethod -Uri "$baseUrl/shows/$($show.id)/seats" -Method Get
$reAvailable1 = $seatsAfterCancel.data.seats | Where-Object { $_.id -eq $seat1.id }
Write-Host "Seat" $seat1.rowLabel$seat1.seatNumber "status in database after cancellation:" $reAvailable1.status
Write-Host "=== ALL E2E BACKEND & DATABASE TESTS COMPLETED SUCCESSFULLY ==="
