# Knowledge-Aware LLM Middleware - Scripts Directory

This directory contains PowerShell scripts to manage the Knowledge-Aware LLM Middleware application lifecycle and provide useful development and testing functions.

## 🚀 Quick Start

1. **Start the application**: `.\start-app.bat` or `.\start-app.ps1`
2. **Stop the application**: `.\stop-app.bat` or `.\stop-app.ps1`
3. **Check status**: `.\status.bat` or `.\status.ps1`
4. **View logs**: `.\logs.bat` or `.\logs.ps1`
5. **Reset database**: `.\db-reset.bat` or `.\db-reset.ps1`

## 📁 Scripts Overview

### Core Management Scripts
- **`start-app.ps1`** / **`start-app.bat`** - Start all services with proper dependency management
- **`stop-app.ps1`** / **`stop-app.bat`** - Stop all services gracefully
- **`status.ps1`** / **`status.bat`** - Display current status of all services
- **`logs.ps1`** / **`logs.bat`** - View logs for specific services
- **`db-reset.ps1`** / **`db-reset.bat`** - Reset database and run fresh migrations

### Development Scripts
- **`build-app.ps1`** - Build the application using Maven
- **`clean-build.ps1`** - Clean and rebuild the application
- **`shell.ps1`** - Open shell access to specific containers

### Testing Scripts
- **`test-api.ps1`** - Test basic API endpoints
- **`test-end-to-end.ps1`** - Run comprehensive end-to-end tests
- **`create-test-data.ps1`** - Create test data for development

### Database Management Scripts
- **`db-backup.ps1`** - Create database backup
- **`db-restore.ps1`** - Restore database from backup

### Utility Scripts
- **`health-check.ps1`** - Check health of all services
- **`cleanup.ps1`** - Clean up containers, images, and volumes
- **`update-deps.ps1`** - Update Docker images and dependencies

## 🔧 Prerequisites

- **Docker Desktop** - Must be running
- **PowerShell 5.1+** - For script execution
- **Windows Command Prompt** - For .bat file execution
- **Git Bash** - Alternative shell option (scripts provided)

## 📖 Usage Examples

### Starting the Application
```powershell
# Start all services (PowerShell)
.\scripts\start-app.ps1

# Start all services (Command Prompt)
.\scripts\start-app.bat

# Start specific services only
.\scripts\start-app.ps1 -Services postgres,redis,ollama,middleware

# Start with custom profile
.\scripts\start-app.ps1 -Profile local
```

### Checking Status
```powershell
# Check all services (PowerShell)
.\scripts\status.ps1

# Check all services (Command Prompt)
.\scripts\status.bat

# Check specific service
.\scripts\status.ps1 -Service middleware

# Detailed status with logs
.\scripts\status.ps1 -Verbose
```

### Viewing Logs
```powershell
# View middleware logs (PowerShell)
.\scripts\logs.ps1 -Service middleware

# View middleware logs (Command Prompt)
.\scripts\logs.bat -Service middleware

# View all services logs
.\scripts\logs.ps1 -Service all

# Follow logs in real-time
.\scripts\logs.ps1 -Service middleware -Follow

# Filter logs for errors
.\scripts\logs.ps1 -Service all -Filter "ERROR"
```

### Testing the Application
```powershell
# Basic API test
.\scripts\test-api.ps1

# End-to-end testing
.\scripts\test-end-to-end.ps1

# Create test data
.\scripts\create-test-data.ps1
```

### Database Operations
```powershell
# Reset database (clears all data) - PowerShell
.\scripts\db-reset.ps1

# Reset database (clears all data) - Command Prompt
.\scripts\db-reset.bat

# Reset without confirmation (DANGEROUS!)
.\scripts\db-reset.ps1 -Confirm

# Reset without test data
.\scripts\db-reset.ps1 -SkipTestData

# Backup database
.\scripts\db-backup.ps1 -OutputPath ./backups/

# Restore database
.\scripts\db-restore.ps1 -BackupPath ./backups/backup.sql
```

## 🚨 Troubleshooting

### Common Issues

1. **Services won't start**
   - Check Docker Desktop is running
   - Ensure ports 5432, 6379, 8080, 11434 are available
   - Run `.\scripts\cleanup.ps1` to clear conflicts

2. **Database connection issues**
   - Run `.\scripts\db-reset.ps1` to reset database
   - Check PostgreSQL logs: `.\scripts\logs.ps1 -Service postgres`

3. **Migration failures**
   - Run `.\scripts\db-reset.ps1` to clear migration history
   - Check migration files in `src/main/resources/db/migration/`

4. **Ollama service issues**
   - The Ollama service may take time to initialize
   - Check Ollama logs: `.\scripts\logs.ps1 -Service ollama`
   - Ensure Ollama models are downloaded: `docker exec knowledge-ollama ollama list`

### Getting Help

- **Check logs**: `.\scripts\logs.ps1 -Service <service-name>` or `.\scripts\logs.bat -Service <service-name>`
- **Health check**: `.\scripts\health-check.ps1`
- **Status**: `.\scripts\status.ps1 -Verbose` or `.\scripts\status.bat`

## 🔒 Security Notes

- Scripts use default credentials for local development
- Never use these scripts in production without proper security review
- Database passwords and API keys are for development only

## 📝 Script Development

All scripts follow these conventions:
- **Error handling**: Proper try-catch blocks with meaningful error messages
- **Parameter validation**: Input validation and helpful usage messages
- **Logging**: Consistent logging format with color coding
- **Documentation**: Inline comments explaining complex operations

## 🆘 Support

For issues with the scripts:
1. Check the troubleshooting section above
2. Review the script logs and error messages
3. Ensure Docker Desktop is running and healthy
4. Verify all prerequisites are installed

---

**Note**: These scripts are designed for local development and testing. For production deployment, use the appropriate deployment scripts and follow security best practices.
