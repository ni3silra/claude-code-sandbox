# Java 8 Test Utilities Framework

A comprehensive testing framework for legacy Java 8 applications that provides mocked resources for MQ, SFTP, and Database operations without requiring actual infrastructure.

## Features

- **Pure Java 8 compatibility** - No Spring framework dependencies
- **DatabaseTestUtility** - H2 in-memory database with SQL file loading
- **MQTestUtility** - Mock message queues with ActiveMQ embedded broker support
- **SFTPTestUtility** - Embedded SFTP server with file operations
- **Comprehensive test coverage** - JUnit 4 based test suite
- **Clean setup/teardown** - Proper resource management

## Project Structure

```
java8-test-utils/
├── src/main/java/com/example/testutils/
│   ├── DatabaseTestUtility.java       # H2 database operations
│   ├── MQTestUtility.java             # Message queue operations
│   ├── SFTPTestUtility.java           # SFTP file operations
│   └── IntegrationTestDemo.java       # Complete demo program
├── src/main/resources/test-data/
│   ├── init-database.sql              # Database initialization script
│   ├── test-message.txt               # Sample MQ message
│   └── test-file-content.txt          # Sample SFTP file content
└── src/test/java/com/example/testutils/
    └── AllUtilitiesTest.java          # Comprehensive test suite
```

## Quick Start

### Build and Test
```bash
cd java8-test-utils
mvn clean compile test
```

### Run Integration Demo
```bash
mvn compile exec:java -Dexec.mainClass="com.example.testutils.IntegrationTestDemo"
```

## Usage Examples

### Database Testing
```java
DatabaseTestUtility dbUtil = new DatabaseTestUtility();
dbUtil.setupFromSqlFile("path/to/init-database.sql");

// Insert data
Map<String, Object> data = new HashMap<>();
data.put("id", 1);
data.put("name", "Test User");
dbUtil.insertRecord("users", data);

// Query data
List<Map<String, Object>> results = dbUtil.executeQuery("SELECT * FROM users WHERE id = ?", 1);

// Check existence
boolean exists = dbUtil.recordExists("users", "id = ?", 1);

// Cleanup
dbUtil.teardown();
```

### MQ Testing
```java
MQTestUtility mqUtil = new MQTestUtility(true); // true = use mock queues
mqUtil.setup();

// Send message
mqUtil.createQueue("test.queue");
mqUtil.sendMessage("test.queue", "Hello World");
mqUtil.sendMessageFromFile("test.queue", "path/to/message.txt");

// Receive message
String message = mqUtil.receiveMessage("test.queue");
List<String> allMessages = mqUtil.receiveAllMessages("test.queue");

// Queue management
int count = mqUtil.getMessageCount("test.queue");
boolean empty = mqUtil.isQueueEmpty("test.queue");

// Cleanup
mqUtil.teardown();
```

### SFTP Testing
```java
SFTPTestUtility sftpUtil = new SFTPTestUtility();
sftpUtil.setup();

// Upload files
sftpUtil.uploadFileContent("File content", "/remote/file.txt");
sftpUtil.uploadFileFromTextFile("local/file.txt", "/remote/file.txt");

// Download files
String content = sftpUtil.downloadFileContent("/remote/file.txt");
sftpUtil.downloadFile("/remote/file.txt", "local/downloaded.txt");

// File operations
boolean exists = sftpUtil.fileExists("/remote/file.txt");
long size = sftpUtil.getFileSize("/remote/file.txt");
List<String> files = sftpUtil.listFiles("/remote/directory");

// Directory operations
sftpUtil.createDirectory("/remote/newdir");
sftpUtil.deleteFile("/remote/file.txt");

// Cleanup
sftpUtil.teardown();
```

## Integration Workflow Example

The `IntegrationTestDemo` class demonstrates a complete workflow:

1. **Database Setup** - Load initial data from SQL file
2. **MQ Operations** - Send message from file to queue
3. **SFTP Operations** - Upload file content to server
4. **File Verification** - Download and verify file contents
5. **Database Updates** - Process data and update records

## Dependencies

- **JUnit 4.13.2** - Testing framework
- **H2 2.1.214** - In-memory database
- **Apache SSHD 2.9.2** - Embedded SFTP server
- **ActiveMQ 5.17.6** - Message broker (optional)
- **JSch 0.1.55** - SFTP client
- **SLF4J 1.7.36** - Logging

## Key Features

### Database Utility
- H2 in-memory database with MySQL compatibility mode
- SQL file execution for schema and data initialization
- CRUD operations with prepared statements
- Record existence checking
- Automatic connection management

### MQ Utility
- Two modes: Mock queues (ConcurrentHashMap) or embedded ActiveMQ
- Message sending/receiving with timeout support
- File-based message loading
- Queue management operations
- Thread-safe implementation

### SFTP Utility
- Embedded SFTP server with configurable credentials
- File upload/download operations
- Directory management
- File existence and size checking
- Automatic cleanup of temporary files

## Thread Safety

All utilities are designed to be used in single-threaded test scenarios. The MQ utility uses thread-safe collections for mock queues.

## Error Handling

All methods throw meaningful exceptions with descriptive error messages. Proper cleanup is ensured even when exceptions occur.

## License

This project is provided as-is for educational and testing purposes.