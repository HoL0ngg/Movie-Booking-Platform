# Run once shortly before class. Free services are allowed to sleep afterward.
$demoServices = @('api-gateway', 'auth-service', 'movie-service', 'cinema-service',
    'booking-service', 'payment-service', 'notification-service', 'ai-service')
$demoJobs = foreach ($demoService in $demoServices) {
    Start-Job -ArgumentList $demoService -ScriptBlock {
        param($serviceName)
        $serviceUrl = "https://movie-booking-$serviceName.onrender.com/actuator/health"
        $deadline = [DateTime]::UtcNow.AddSeconds(240)
        $ready = $false
        $detail = 'Startup not ready'
        do {
            try {
                $remaining = [Math]::Max(1, [Math]::Ceiling(($deadline - [DateTime]::UtcNow).TotalSeconds))
                $health = Invoke-RestMethod -Uri $serviceUrl -TimeoutSec ([Math]::Min(30, $remaining))
                if ($health.status -eq 'UP') { $ready = $true; $detail = 'UP'; break }
                $detail = 'Startup not ready'
            } catch { $detail = $_.Exception.Message }
            if ([DateTime]::UtcNow -lt $deadline) { Start-Sleep -Seconds 3 }
        } while ([DateTime]::UtcNow -lt $deadline)
        [pscustomobject]@{ Service = $serviceName; Ready = $ready; Detail = $detail }
    }
}
$demoResults = $demoJobs | Receive-Job -Wait -AutoRemoveJob
$demoResults | Select-Object Service, Ready, Detail | Format-Table -AutoSize
if ($demoResults.Count -ne $demoServices.Count -or $demoResults.Ready -contains $false) { exit 1 }
