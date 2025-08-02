# IBM MQ Test Utils - Tandem Environment Deployment

This guide explains how to deploy and run IBM MQ tests in a real Tandem environment with live IBM MQ servers.

## 🏗️ Prerequisites

### On Development Machine:
- Maven 3.6+
- Java 17+
- SSH access to Tandem server

### On Tandem Server:
- Java 17+ installed
- Network access to IBM MQ server
- Appropriate MQ user credentials
- Required queue permissions

## 📦 Deployment Steps

### 1. Build and Deploy

```bash
# Make deployment script executable
chmod +x tandem-deploy.sh

# Set environment variables (optional)
export TANDEM_HOST="your-tandem-server.com"
export TANDEM_USER="your-username"
export TANDEM_PATH="/home/your-username/mq-tests"

# Deploy to Tandem server
./tandem-deploy.sh
```

### 2. Configure on Tandem Server

```bash
# SSH to Tandem server
ssh your-username@your-tandem-server.com

# Navigate to deployment directory
cd /home/your-username/mq-tests

# Setup environment
./setup-environment.sh

# Edit MQ configuration
vi run-mq-tests.sh
```

### 3. Update MQ Configuration

Edit the following variables in `run-mq-tests.sh`:

```bash
# IBM MQ Server Configuration
export MQ_HOST="your-mq-server.com"
export MQ_PORT="1414"
export MQ_QUEUE_MANAGER="PROD_QM1"
export MQ_CHANNEL="SYSTEM.DEF.SVRCONN"
export MQ_USER="your-mq-user"
export MQ_PASSWORD="your-mq-password"

# Queue Names (use your actual queue names)
export MQ_REQUEST_QUEUE="PROD.REQUEST.QUEUE"
export MQ_RESPONSE_QUEUE="PROD.RESPONSE.QUEUE"
export MQ_DLQ="PROD.DEAD.LETTER.QUEUE"
export MQ_TEST_QUEUE="PROD.TEST.QUEUE"
```

## 🚀 Single Command Execution

Once deployed and configured, run tests with a single command:

```bash
./run-mq-tests.sh
```

## 📊 Test Coverage

The test runner performs:

### ✅ **Connection Tests**
- Validates MQ server connectivity
- Verifies queue manager access
- Tests channel authentication

### ✅ **CRUD Operations**
- **CREATE**: Send messages with auto/custom correlation IDs
- **READ**: Receive messages by various filters
- **UPDATE**: Message content modifications
- **DELETE**: Queue clearing and selective removal

### ✅ **Performance Tests**
- Bulk message sending (100 messages)
- Bulk message receiving
- Throughput measurements (messages/second)

### ✅ **Error Handling**
- Connection failure detection
- Queue access validation
- Timeout handling

## 📋 Sample Output

```
=================================================
Starting IBM MQ Tests on Tandem Environment
=================================================
🔗 Testing MQ Connection...
✅ MQ Connection successful
📊 Queue Manager: PROD_QM1
🏠 Host: tandem-mq-server.com:1414
📡 Channel: SYSTEM.DEF.SVRCONN

🧪 Running CRUD Tests...
📝 CREATE Test - Sending messages...
✅ Messages sent - Correlation IDs: abc123, CUSTOM-CORR-ID
📖 READ Test - Reading messages...
📊 Queue depth: 2
✅ Messages received: 'Tandem Test Message 1', 'Tandem Test Message 2'

⚡ Running Performance Tests...
📊 Sent 100 messages in 1250 ms (80.0 msg/sec)
📊 Received 100 messages in 890 ms (112.4 msg/sec)

=================================================
✅ ALL MQ TESTS COMPLETED SUCCESSFULLY
=================================================
```

## 🔧 Configuration Options

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_HOST` | MQ Server hostname | localhost |
| `MQ_PORT` | MQ Server port | 1414 |
| `MQ_QUEUE_MANAGER` | Queue Manager name | QM1 |
| `MQ_CHANNEL` | MQ Channel | SYSTEM.DEF.SVRCONN |
| `MQ_USER` | MQ Username | mquser |
| `MQ_PASSWORD` | MQ Password | mqpass |
| `MQ_TEST_QUEUE` | Test queue name | TEST.QUEUE |
| `MQ_RECEIVE_TIMEOUT` | Receive timeout (ms) | 10000 |
| `MQ_SEND_TIMEOUT` | Send timeout (ms) | 5000 |

### Advanced Configuration

For advanced configuration, edit `application-tandem.yml`:

```yaml
mq:
  test:
    connection:
      host: ${MQ_HOST:your-mq-server.com}
      port: ${MQ_PORT:1414}
      # ... other settings
    timeout:
      receive: ${MQ_RECEIVE_TIMEOUT:10000}
      send: ${MQ_SEND_TIMEOUT:5000}
```

## 🐛 Troubleshooting

### Common Issues:

1. **Connection Refused**
   ```
   Solution: Check MQ_HOST, MQ_PORT, firewall settings
   ```

2. **Authentication Failed**
   ```
   Solution: Verify MQ_USER, MQ_PASSWORD, channel security
   ```

3. **Queue Not Found**
   ```
   Solution: Check MQ_TEST_QUEUE name, queue permissions
   ```

4. **Permission Denied**
   ```
   Solution: Ensure user has PUT/GET permissions on queues
   ```

### Log Files

Test execution logs are saved as:
- `mq-test-YYYYMMDD_HHMMSS.log`

Check logs for detailed error information.

## 📂 Files Deployed

```
/home/your-username/mq-tests/
├── mq-test-utils-1.0.0-executable.jar    # Executable JAR
├── application-tandem.yml                 # Configuration template
├── run-mq-tests.sh                       # Test execution script
└── setup-environment.sh                  # Environment setup
```

## 🔄 Re-deployment

To update with new changes:

```bash
# On development machine
./tandem-deploy.sh

# On Tandem server
./run-mq-tests.sh
```

## 🎯 Key Features

- ✅ **Single Command Execution**: One command runs all tests
- ✅ **Environment Detection**: Automatically configures for Tandem
- ✅ **Comprehensive Testing**: Full CRUD + performance tests
- ✅ **Detailed Logging**: Complete test execution logs
- ✅ **Error Handling**: Graceful failure with clear messages
- ✅ **Configurable**: Easy environment-specific configuration