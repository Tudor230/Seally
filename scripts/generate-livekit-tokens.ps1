param(
    [Parameter(Mandatory = $true)]
    [string]$ApiKey,

    [Parameter(Mandatory = $true)]
    [string]$ApiSecret,

    [Parameter(Mandatory = $true)]
    [string]$Room,

    [int]$ExpiresInMinutes = 120
)

function Convert-ToBase64Url {
    param([byte[]]$Bytes)
    [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function New-LiveKitToken {
    param(
        [string]$Identity,
        [bool]$CanPublish,
        [bool]$CanSubscribe
    )

    $headerJson = '{"alg":"HS256","typ":"JWT"}'
    $exp = [DateTimeOffset]::UtcNow.AddMinutes($ExpiresInMinutes).ToUnixTimeSeconds()

    $payloadObject = [ordered]@{
        video = @{
            roomJoin = $true
            room = $Room
            canPublish = $CanPublish
            canSubscribe = $CanSubscribe
        }
        iss = $ApiKey
        exp = $exp
        nbf = 0
        sub = $Identity
    }

    $payloadJson = $payloadObject | ConvertTo-Json -Compress -Depth 5

    $headerPart = Convert-ToBase64Url([Text.Encoding]::UTF8.GetBytes($headerJson))
    $payloadPart = Convert-ToBase64Url([Text.Encoding]::UTF8.GetBytes($payloadJson))
    $message = "$headerPart.$payloadPart"

    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($ApiSecret))
    $signatureBytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($message))
    $signaturePart = Convert-ToBase64Url($signatureBytes)
    $hmac.Dispose()

    return "$message.$signaturePart"
}

$mobileToken = New-LiveKitToken -Identity "mobile-publisher" -CanPublish $true -CanSubscribe $true
$webToken = New-LiveKitToken -Identity "web-viewer" -CanPublish $false -CanSubscribe $true

Write-Output "MOBILE_TOKEN=$mobileToken"
Write-Output "WEB_TOKEN=$webToken"
Write-Output ""
Write-Output "Use in android\\gradle.properties:"
Write-Output "LIVEKIT_TOKEN=$mobileToken"
Write-Output ""
Write-Output "Use in tv-web\\.env:"
Write-Output "VITE_LIVEKIT_TOKEN=$webToken"
