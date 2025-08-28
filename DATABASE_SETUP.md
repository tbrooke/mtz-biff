# Database Setup Guide

## PostgreSQL Connection Setup

### Prerequisites
1. PostgreSQL server running and accessible
2. Database created for the church website
3. User with appropriate permissions

### Step 1: Configure Database Connection

1. Copy `config.template.env` to `config.env`:
   ```bash
   cp resources/config.template.env config.env
   ```

2. Edit `config.env` and update the PostgreSQL settings:
   ```env
   # Change from standalone to jdbc
   XTDB_TOPOLOGY=jdbc
   
   # Update with your actual PostgreSQL connection details
   XTDB_JDBC_URL=jdbc:postgresql://localhost:5432/church_db?user=your_username&password=your_password&sslmode=require
   ```

### Step 2: Generate Secrets
Run the following command to generate secure secrets:
```bash
clj -M:dev generate-secrets
```

### Step 3: Start the Application
```bash
clj -M:dev dev
```

## Database Connection Testing

### Access the Database Test Page
1. Start the application
2. Sign up/Sign in at `http://localhost:8080`
3. Navigate to `http://localhost:8080/database-test`
4. Use the test interface to:
   - Verify database connectivity
   - Create sample test data
   - Query existing data
   - Clear test data

### Test Operations
- **Connection Test**: Validates database connectivity
- **Create Test Data**: Adds sample users, sermons, and events
- **Query Data**: Shows current data counts and samples
- **Clear Test Data**: Removes test data

## Admin Interface

### Access the Admin Dashboard
1. Navigate to `http://localhost:8080/admin`
2. View dashboard with data statistics
3. Access management interfaces for:
   - Sermons (`/admin/sermons`)
   - Events (`/admin/events`)

### Sermon Management
- **Create New**: `/admin/sermons/new`
- **Edit Existing**: Click "Edit" from sermons list
- **Delete**: Click "Delete" from sermons list (with confirmation)

### Event Management  
- **Create New**: `/admin/events/new`
- **Edit Existing**: Click "Edit" from events list
- **Delete**: Click "Delete" from events list (with confirmation)

## Schema Overview

The application supports the following content types:

### Core Entities
- **Users**: Authentication and user management
- **Sermons**: Church sermons with metadata
- **Events**: Church events and activities
- **Blog Entries**: Blog posts and announcements
- **Homepage Features**: Homepage content management
- **Uploads**: File upload management

### Sermon Fields
- Title, Speaker, Date, Scripture Text (required)
- Topic, Series Name/Part, Summary
- Video URL, Audio URL, Duration
- Featured flag, SEO metadata

### Event Fields
- Title, Description, Start/End Date, Type (required)
- Contact information, Registration details
- Location, Cost, Attendee limits
- Featured flag, All-day flag

## Troubleshooting

### Common Issues
1. **Connection Failed**: Check PostgreSQL is running and credentials are correct
2. **Permission Denied**: Ensure database user has CREATE/READ/WRITE permissions
3. **SSL Errors**: Adjust `sslmode` parameter in JDBC URL
4. **Port Conflicts**: Change port in config if 8080 is in use

### Database Permissions
Your PostgreSQL user needs these permissions:
```sql
GRANT CREATE, SELECT, INSERT, UPDATE, DELETE ON DATABASE church_db TO your_username;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_username;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_username;
```

### Logs
Check application logs for detailed error information:
- Database connection errors
- Transaction failures
- Validation errors

### Support
- Check the XTDB documentation for JDBC configuration
- Verify PostgreSQL connection with standard tools first
- Use the database test page to diagnose issues