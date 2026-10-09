param([string]$Apk = 'releases/RobotFlow-v0.5.0-native-3d-debug.apk')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Apk))
try {
    $levels = @($archive.Entries | Where-Object FullName -Match '^assets/levels/level-\d+\.json$').Count
    $webAssets = @($archive.Entries | Where-Object FullName -Match '^assets/.*\.(html|js|css)$').Count
    $hasWebView = $false
    $hasScene = $false
    $hasEngine = $false
    foreach ($entry in @($archive.Entries | Where-Object FullName -Match '^classes.*\.dex$')) {
        $stream = $entry.Open()
        $memory = [System.IO.MemoryStream]::new()
        try {
            $stream.CopyTo($memory)
            $content = [System.Text.Encoding]::ASCII.GetString($memory.ToArray())
            $hasWebView = $hasWebView -or $content.Contains('Landroid/webkit/WebView;')
            $hasScene = $hasScene -or $content.Contains('Lcom/bossxor/robotflow/NativeScene;')
            $hasEngine = $hasEngine -or $content.Contains('Lcom/bossxor/robotflow/NativeGame;')
        } finally { $stream.Dispose(); $memory.Dispose() }
    }
    if ($levels -ne 200 -or $webAssets -ne 0 -or $hasWebView -or !$hasScene -or !$hasEngine) {
        throw "Invalid native APK: levels=$levels webAssets=$webAssets WebView=$hasWebView NativeScene=$hasScene NativeGame=$hasEngine"
    }
    Write-Output "Verified native APK: 200 JSON levels, native Java engine, OpenGL scene, no WebView or web assets."
} finally { $archive.Dispose() }
