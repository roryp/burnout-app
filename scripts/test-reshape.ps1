param([string]$BaseUrl = "https://burnoutdemorpza-backend.yellowwave-d1b4ff3a.swedencentral.azurecontainerapps.io")

$pre = Invoke-RestMethod -Method POST "$BaseUrl/demo/api/checkin" `
    -ContentType 'application/json' `
    -Body '{"userId":"roryp","repo":"roryp/burnout-app"}'
Write-Host "PRE  stress=$($pre.stressScore) level=$($pre.stressLevel)"
Write-Host ("PRE  breakdown: " + ($pre.breakdown | ConvertTo-Json -Compress))

$rs = Invoke-RestMethod -Method POST "$BaseUrl/demo/api/reshape" `
    -ContentType 'application/json' `
    -Body '{"repo":"roryp/burnout-app","userId":"roryp"}'
Write-Host ""
Write-Host "RESHAPE actionsApplied=$($rs.actionsApplied) before=$($rs.beforeScore) after=$($rs.afterScore) level=$($rs.afterLevel) llmUsed=$($rs.llmUsed)"
Write-Host ("RESHAPE dayPlan: " + ($rs.dayPlan | ConvertTo-Json -Compress))
Write-Host "RESHAPE explanation: $($rs.explanation)"

$post = Invoke-RestMethod -Method POST "$BaseUrl/demo/api/checkin" `
    -ContentType 'application/json' `
    -Body '{"userId":"roryp","repo":"roryp/burnout-app"}'
Write-Host ""
Write-Host "POST stress=$($post.stressScore) level=$($post.stressLevel)"
Write-Host ("POST breakdown: " + ($post.breakdown | ConvertTo-Json -Compress))
Write-Host ("POST hints: " + ($post.breakdownHints | ConvertTo-Json -Compress))
