$services = @(
    @{
        Name = "auth-service"
        AppFile = "auth-service\src\main\java\com\chatsever\auth\AuthApplication.java"
        ExFile = "auth-service\src\main\java\com\chatsever\auth\exception\GlobalExceptionHandler.java"
    },
    @{
        Name = "user-profile-service"
        AppFile = "user-profile-service\src\main\java\com\chatsever\profile\UserProfileApplication.java"
        ExFile = "user-profile-service\src\main\java\com\chatsever\profile\exception\GlobalExceptionHandler.java"
    },
    @{
        Name = "presence-service"
        AppFile = "presence-service\src\main\java\com\chatsever\presence\PresenceApplication.java"
        ExFile = "presence-service\src\main\java\com\chatsever\presence\exception\GlobalExceptionHandler.java"
    },
    @{
        Name = "file-service"
        AppFile = "file-service\src\main\java\com\chatsever\file\FileApplication.java"
        ExFile = "file-service\src\main\java\com\chatsever\file\exception\GlobalExceptionHandler.java"
    },
    @{
        Name = "log-service"
        AppFile = "log-service\src\main\java\com\chatsever\log\LogApplication.java"
        ExFile = "log-service\src\main\java\com\chatsever\log\exception\GlobalExceptionHandler.java"
    }
)

foreach ($svc in $services) {
    if (Test-Path $svc.ExFile) {
        Remove-Item -Path $svc.ExFile -Force
        Write-Host "Removed $($svc.ExFile)"
    }

    if (Test-Path $svc.AppFile) {
        $content = Get-Content $svc.AppFile -Raw
        if ($content -notmatch "com.chatsever.common.exception.GlobalExceptionHandler") {
            $content = $content -replace "@SpringBootApplication", "@SpringBootApplication`n@org.springframework.context.annotation.Import(com.chatsever.common.exception.GlobalExceptionHandler.class)"
            Set-Content -Path $svc.AppFile -Value $content -NoNewline
            Write-Host "Updated $($svc.AppFile)"
        }
    }
}
