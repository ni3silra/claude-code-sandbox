#!/bin/bash

#################################################################
# IBM MQ Test Utils - Tandem Deployment Script
#################################################################

set -e

# Configuration
JAR_NAME="mq-test-utils-1.0.0-executable.jar"
REMOTE_HOST="${TANDEM_HOST:-tandem-server.company.com}"
REMOTE_USER="${TANDEM_USER:-mqtest}"
REMOTE_PATH="${TANDEM_PATH:-/home/mqtest/mq-tests}"
LOG_FILE="mq-test-$(date +%Y%m%d_%H%M%S).log"

echo "================================================="
echo "IBM MQ Test Utils - Tandem Deployment"
echo "================================================="
echo "Target Host: $REMOTE_HOST"
echo "Remote User: $REMOTE_USER"
echo "Remote Path: $REMOTE_PATH"
echo "JAR File: $JAR_NAME"
echo "================================================="

# Build the executable JAR
echo "🔨 Building executable JAR..."
mvn clean package -DskipTests

# Check if JAR exists
if [ ! -f "mq-test-utils/target/$JAR_NAME" ]; then
    echo "❌ ERROR: JAR file not found at mq-test-utils/target/$JAR_NAME"
    exit 1
fi

echo "✅ JAR built successfully"

# Copy JAR to Tandem server
echo "📤 Copying JAR to Tandem server..."
scp "mq-test-utils/target/$JAR_NAME" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"

# Copy configuration template
echo "📋 Copying configuration template..."
scp "mq-test-utils/src/main/resources/application-tandem.yml" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"

# Create and copy run script
cat > /tmp/run-mq-tests.sh << 'EOF'
#!/bin/bash

#################################################################
# IBM MQ Test Execution Script for Tandem Environment
#################################################################

set -e

# Default values - CUSTOMIZE THESE FOR YOUR ENVIRONMENT
export MQ_HOST=${MQ_HOST:-"localhost"}
export MQ_PORT=${MQ_PORT:-"1414"}
export MQ_QUEUE_MANAGER=${MQ_QUEUE_MANAGER:-"QM1"}
export MQ_CHANNEL=${MQ_CHANNEL:-"SYSTEM.DEF.SVRCONN"}
export MQ_USER=${MQ_USER:-"mquser"}
export MQ_PASSWORD=${MQ_PASSWORD:-"mqpass"}
export MQ_CLIENT_ID=${MQ_CLIENT_ID:-"TANDEM_TEST_CLIENT"}

# Queue names - CUSTOMIZE THESE FOR YOUR ENVIRONMENT
export MQ_REQUEST_QUEUE=${MQ_REQUEST_QUEUE:-"DEV.QUEUE.1"}
export MQ_RESPONSE_QUEUE=${MQ_RESPONSE_QUEUE:-"DEV.QUEUE.2"}
export MQ_DLQ=${MQ_DLQ:-"DEV.DEAD.LETTER.QUEUE"}
export MQ_TEST_QUEUE=${MQ_TEST_QUEUE:-"TEST.QUEUE"}

# Timeouts and retries
export MQ_RECEIVE_TIMEOUT=${MQ_RECEIVE_TIMEOUT:-"10000"}
export MQ_SEND_TIMEOUT=${MQ_SEND_TIMEOUT:-"5000"}
export MQ_RETRY_ATTEMPTS=${MQ_RETRY_ATTEMPTS:-"5"}
export MQ_RETRY_DELAY=${MQ_RETRY_DELAY:-"2000"}

JAR_FILE="mq-test-utils-1.0.0-executable.jar"
LOG_FILE="mq-test-$(date +%Y%m%d_%H%M%S).log"

echo "================================================="
echo "IBM MQ Test Execution - Tandem Environment"
echo "================================================="
echo "MQ Host: $MQ_HOST:$MQ_PORT"
echo "Queue Manager: $MQ_QUEUE_MANAGER"
echo "Channel: $MQ_CHANNEL"
echo "User: $MQ_USER"
echo "Test Queue: $MQ_TEST_QUEUE"
echo "Log File: $LOG_FILE"
echo "================================================="

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ ERROR: JAR file '$JAR_FILE' not found in current directory"
    echo "Please ensure the JAR file is uploaded to this directory"
    exit 1
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ ERROR: Java is not installed or not in PATH"
    exit 1
fi

# Run the tests
echo "🚀 Starting MQ Tests..."
java -jar "$JAR_FILE" \
    --spring.profiles.active=tandem \
    --logging.file.name="$LOG_FILE" \
    2>&1 | tee "$LOG_FILE"

EXIT_CODE=${PIPESTATUS[0]}

if [ $EXIT_CODE -eq 0 ]; then
    echo "================================================="
    echo "✅ MQ Tests completed successfully!"
    echo "📊 Results logged to: $LOG_FILE"
    echo "================================================="
else
    echo "================================================="
    echo "❌ MQ Tests failed with exit code: $EXIT_CODE"
    echo "📊 Error details in: $LOG_FILE"
    echo "================================================="
    exit $EXIT_CODE
fi
EOF

chmod +x /tmp/run-mq-tests.sh
scp /tmp/run-mq-tests.sh "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"

echo "📋 Creating environment setup script..."

# Create environment setup script
cat > /tmp/setup-environment.sh << 'EOF'
#!/bin/bash

#################################################################
# Environment Setup for IBM MQ Tests
#################################################################

echo "Setting up environment for MQ Tests..."

# Create directory if it doesn't exist
mkdir -p /home/mqtest/mq-tests
cd /home/mqtest/mq-tests

# Make run script executable
chmod +x run-mq-tests.sh

echo "================================================="
echo "Setup completed!"
echo "================================================="
echo "Next steps:"
echo "1. Edit the MQ configuration in run-mq-tests.sh"
echo "2. Update the following variables:"
echo "   - MQ_HOST"
echo "   - MQ_QUEUE_MANAGER" 
echo "   - MQ_CHANNEL"
echo "   - MQ_USER"
echo "   - MQ_PASSWORD"
echo "   - MQ_TEST_QUEUE"
echo "3. Run: ./run-mq-tests.sh"
echo "================================================="
EOF

chmod +x /tmp/setup-environment.sh
scp /tmp/setup-environment.sh "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"

echo "🎉 Deployment completed successfully!"
echo "================================================="
echo "Files deployed to $REMOTE_HOST:$REMOTE_PATH/"
echo "- $JAR_NAME"
echo "- application-tandem.yml"
echo "- run-mq-tests.sh"
echo "- setup-environment.sh"
echo "================================================="
echo "Next steps on Tandem server:"
echo "1. SSH to: ssh $REMOTE_USER@$REMOTE_HOST"
echo "2. Go to: cd $REMOTE_PATH"
echo "3. Setup: ./setup-environment.sh"
echo "4. Configure MQ settings in run-mq-tests.sh"
echo "5. Run tests: ./run-mq-tests.sh"
echo "================================================="