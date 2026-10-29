# ============================================================
# PROJECT CONFIGURATION
# ============================================================

# Local project

$ProjectDirectory = "D:\Smart Credit Solution_Enterprises_Portal\admin"
$TemporaryWorktree = "D:\Smart Credit Solution_Deploy_Temp"

# Git

$GitRemote = "origin"
$GitBranch = "master"

# Generated WAR

$GeneratedWarName = "enterprises.war"

# SSH

$ServerUser = "ciuser"
$ServerHost = "10.1.3.5"
$ServerPort = 22

# SSH Key

$SshKeyPath = Join-Path `
    $env:USERPROFILE `
    ".ssh\ScsGateway_deploy"

# Tomcat
$TomcatHome = "/opt/tomcat-10.1.34"

# Application
$ApplicationName = "enterprises"

# Remote Paths
$RemoteTemporaryWar = "/tmp/$ApplicationName.war"

$RemoteDeploymentWar = "$TomcatHome/webapps/$ApplicationName.war"

$RemoteExpandedDirectory = "$TomcatHome/webapps/$ApplicationName"

$RemoteBackupDirectory = "$TomcatHome/backups"