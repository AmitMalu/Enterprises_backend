$ErrorActionPreference = "Stop"

# ============================================================
# LOAD PROJECT CONFIGURATION
# ============================================================

$ConfigFile = Join-Path $PSScriptRoot "deploy-config.ps1"

if (-not (Test-Path $ConfigFile)) {
    throw "deploy-config.ps1 not found: $ConfigFile"
}

. $ConfigFile

# ============================================================
# VALIDATE CONFIGURATION
# ============================================================

if (-not (Test-Path $SshKeyPath)) {
    throw "SSH private key not found: $SshKeyPath"
}

$CommonConnectionOptions = @(
    "-i", $SshKeyPath
    "-o", "IdentitiesOnly=yes"
    "-o", "BatchMode=yes"
    "-o", "ConnectTimeout=15"
    "-o", "ServerAliveInterval=30"
    "-o", "ServerAliveCountMax=3"
    "-o", "StrictHostKeyChecking=accept-new"
)

$SshOptions = @(
    $CommonConnectionOptions
    "-p", "$ServerPort"
)

$ScpOptions = @(
    $CommonConnectionOptions
    "-P", "$ServerPort"
)

# ============================================================
# HELPER FUNCTIONS
# ============================================================

function Write-Step {
    param (
        [string]$Message
    )

    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Message
    Write-Host "============================================================"
}

function Remove-TemporaryWorktree {
    Write-Step "Cleaning temporary deployment folder"

    Set-Location $ProjectDirectory

    git worktree remove --force $TemporaryWorktree 2>$null

    if (Test-Path $TemporaryWorktree) {
        Remove-Item -Path $TemporaryWorktree -Recurse -Force
    }
}

# ============================================================
# START
# ============================================================

try {

    Write-Step "Starting backend deployment"

    if (-not (Test-Path $ProjectDirectory)) {
        throw "Project directory not found: $ProjectDirectory"
    }

    Set-Location $ProjectDirectory

    # ========================================================
    # CHECK VPN / SERVER CONNECTION
    # ========================================================

    Write-Step "Checking backend server connection"

    & ssh @SshOptions `
    "$ServerUser@$ServerHost" `
    "echo 'Backend server connection successful'"

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to connect to backend server. Connect the VPN and try again."
    }

    # ========================================================
    # FETCH LATEST MASTER
    # ========================================================

    Write-Step "Fetching latest origin/master"

    git fetch $GitRemote $GitBranch

    if ($LASTEXITCODE -ne 0) {
        throw "Git fetch failed."
    }

    $DeploymentCommit = git rev-parse "$GitRemote/$GitBranch"

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to resolve deployment commit."
    }

    $DeploymentCommitShort = git rev-parse --short "$GitRemote/$GitBranch"

    Write-Host "Deployment commit: $DeploymentCommitShort"

    # ========================================================
    # REMOVE OLD TEMPORARY WORKTREE
    # ========================================================

    if (Test-Path $TemporaryWorktree) {
        Write-Step "Removing old temporary deployment folder"

        git worktree remove --force $TemporaryWorktree 2>$null

        if (Test-Path $TemporaryWorktree) {
            Remove-Item -Path $TemporaryWorktree -Recurse -Force
        }
    }

    git worktree prune

    # ========================================================
    # CREATE CLEAN WORKTREE FROM MASTER
    # ========================================================

    Write-Step "Creating clean copy of origin/master"

    git worktree add `
        --detach `
        $TemporaryWorktree `
        "$GitRemote/$GitBranch"

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create temporary Git worktree."
    }

    # ========================================================
    # BUILD WAR
    # ========================================================

    Write-Step "Running Maven clean install"

    Set-Location $TemporaryWorktree

    & ".\mvnw.cmd" clean install -DskipTests

    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }

    $LocalWarPath = Join-Path `
        $TemporaryWorktree `
        "target\$GeneratedWarName"

    if (-not (Test-Path $LocalWarPath)) {
        throw "Generated WAR not found: $LocalWarPath"
    }

    $WarFile = Get-Item $LocalWarPath

    Write-Host ""
    Write-Host "WAR generated successfully"
    Write-Host "Path: $($WarFile.FullName)"
    Write-Host "Size: $([math]::Round($WarFile.Length / 1MB, 2)) MB"

    # ========================================================
    # UPLOAD WAR
    # ========================================================

    Write-Step "Uploading WAR to backend server"

    & scp @ScpOptions `
    $LocalWarPath `
    "${ServerUser}@${ServerHost}:${RemoteTemporaryWar}"

    if ($LASTEXITCODE -ne 0) {
        throw "WAR upload failed."
    }

    # ========================================================
    # DEPLOY AND RESTART TOMCAT
    # ========================================================

    Write-Step "Backing up and deploying WAR"

    $RemoteCommands = @"
set -e

TOMCAT_HOME='$TomcatHome'
APP_NAME='$ApplicationName'
TEMP_WAR='$RemoteTemporaryWar'
DEPLOY_WAR='$RemoteDeploymentWar'
EXPLODED_DIR='$RemoteExpandedDirectory'
BACKUP_DIR='$RemoteBackupDirectory'

TIMESTAMP=`$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="`$BACKUP_DIR/`${APP_NAME}_`${TIMESTAMP}.war"

echo "Checking uploaded WAR..."

if [ ! -f "`$TEMP_WAR" ]; then
    echo "Uploaded WAR not found: `$TEMP_WAR"
    exit 1
fi

ls -lh "`$TEMP_WAR"

echo "Creating backup directory..."
sudo -n mkdir -p "`$BACKUP_DIR"

if [ -f "`$DEPLOY_WAR" ]; then
    echo "Backing up current WAR..."
    sudo -n cp "`$DEPLOY_WAR" "`$BACKUP_FILE"
    echo "Backup created: `$BACKUP_FILE"
else
    echo "Existing WAR not found. Backup skipped."
fi

echo "Stopping Tomcat..."
    sudo -n "`$TOMCAT_HOME/bin/shutdown.sh" || true

for i in {1..30}; do
    TOMCAT_COUNT=`$(pgrep -f -c 'org.apache.catalina.startup.Bootstrap' || true)

    if [ "`$TOMCAT_COUNT" -eq 0 ]; then
        echo "Tomcat stopped successfully."
        break
    fi

    echo "Waiting for Tomcat to stop. Running processes: `$TOMCAT_COUNT"
    sleep 1
done

TOMCAT_COUNT=`$(pgrep -f -c 'org.apache.catalina.startup.Bootstrap' || true)

if [ "`$TOMCAT_COUNT" -ne 0 ]; then
    echo "Tomcat is still running after shutdown command."
    echo "Deployment stopped to avoid starting another Tomcat process."
    exit 1
fi

echo "Removing previous application..."
sudo -n rm -f "`$DEPLOY_WAR"
sudo -n rm -rf "`$EXPLODED_DIR"

echo "Deploying new WAR..."
sudo -n mv "`$TEMP_WAR" "`$DEPLOY_WAR"
sudo -n chmod 664 "`$DEPLOY_WAR"

echo "Starting Tomcat..."
    sudo -n "`$TOMCAT_HOME/bin/startup.sh"

sleep 10

TOMCAT_COUNT=`$(pgrep -f -c 'org.apache.catalina.startup.Bootstrap' || true)

if [ "`$TOMCAT_COUNT" -eq 0 ]; then
    echo "Tomcat failed to start."
    tail -n 100 "`$TOMCAT_HOME/logs/catalina.out"
    exit 1
fi

if [ "`$TOMCAT_COUNT" -gt 1 ]; then
    echo "WARNING: More than one Tomcat process is running."
    echo "Running Tomcat processes: `$TOMCAT_COUNT"
    ps -ef | grep '[o]rg.apache.catalina.startup.Bootstrap'
else
    echo "Tomcat started successfully."
fi

echo "Waiting for WAR deployment..."

for i in {1..30}; do
    if [ -d "`$EXPLODED_DIR" ]; then
        echo "Application folder created successfully."
        break
    fi

    echo "Waiting for enterprises deployment... `$i/30"
    sleep 2
done

if [ ! -d "`$EXPLODED_DIR" ]; then
    echo "Application deployment folder was not created."
    tail -n 100 "`$TOMCAT_HOME/logs/catalina.out"
    exit 1
fi

echo ""
echo "Deployment completed successfully."
echo "WAR: `$DEPLOY_WAR"
echo "Backup: `$BACKUP_FILE"
echo "Tomcat processes: `$TOMCAT_COUNT"
"@

    # Convert Windows CRLF line endings to Linux LF line endings.
    $NormalizedRemoteCommands = $RemoteCommands `
    -replace "`r`n", "`n" `
    -replace "`r", "`n"

    # Encode the script so PowerShell/SSH cannot change its line endings.
    $RemoteScriptBytes = [System.Text.Encoding]::UTF8.GetBytes(
            $NormalizedRemoteCommands
    )

    $RemoteScriptBase64 = [System.Convert]::ToBase64String(
            $RemoteScriptBytes
    )

    $RemoteExecutionCommand =
    "printf '%s' '$RemoteScriptBase64' | base64 --decode | bash"

    & ssh @SshOptions `
    "$ServerUser@$ServerHost" `
    $RemoteExecutionCommand

    if ($LASTEXITCODE -ne 0) {
        throw "Remote deployment failed."
    }

    # ========================================================
    # SHOW LATEST LOGS
    # ========================================================

    Write-Step "Showing latest Tomcat logs"

    & ssh @SshOptions `
    "$ServerUser@$ServerHost" `
    "tail -n 80 '$TomcatHome/logs/catalina.out'"

    # ========================================================
    # CLEAN TEMPORARY WORKTREE
    # ========================================================

    Remove-TemporaryWorktree

    Write-Step "DEPLOYMENT COMPLETED SUCCESSFULLY"

    Write-Host "Branch : $GitRemote/$GitBranch"
    Write-Host "Commit : $DeploymentCommitShort"
    Write-Host "Server : $ServerUser@$ServerHost"
    Write-Host "WAR    : $RemoteDeploymentWar"
}
catch {

    Write-Host ""
    Write-Host "============================================================"
    Write-Host "DEPLOYMENT FAILED"
    Write-Host "============================================================"
    Write-Host $_.Exception.Message

    try {
        Remove-TemporaryWorktree
    }
    catch {
        Write-Host "Temporary folder cleanup also failed."
    }

    exit 1
}