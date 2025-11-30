#!/bin/bash
# Script to submit Flink job to cluster

set -e

FLINK_HOST="${FLINK_JOBMANAGER_HOST:-flink-jobmanager}"
FLINK_PORT="${FLINK_JOBMANAGER_PORT:-8081}"
FLINK_URL="http://${FLINK_HOST}:${FLINK_PORT}"
JAR_PATH="/app/telemetry-processor.jar"
MAX_RETRIES=60
RETRY_INTERVAL=5

echo "========================================="
echo " AHS Telemetry Processor - Job Submitter"
echo "========================================="
echo ""
echo "Flink JobManager: ${FLINK_URL}"
echo "JAR: ${JAR_PATH}"
echo ""

# Wait for Flink JobManager to be ready
echo "⏳ Waiting for Flink JobManager to be ready..."
for i in $(seq 1 $MAX_RETRIES); do
    if curl -s "${FLINK_URL}/overview" > /dev/null 2>&1; then
        echo "✅ Flink JobManager is ready!"
        break
    fi
    if [ $i -eq $MAX_RETRIES ]; then
        echo "❌ Flink JobManager not ready after ${MAX_RETRIES} attempts"
        exit 1
    fi
    echo "   Attempt $i/$MAX_RETRIES - waiting ${RETRY_INTERVAL}s..."
    sleep $RETRY_INTERVAL
done

# Ensure at least one TaskManager is registered before submission
echo "⏳ Waiting for at least one TaskManager to register..."
for i in $(seq 1 $MAX_RETRIES); do
    TM_COUNT=$(curl -s "${FLINK_URL}/taskmanagers" | grep -o '"id"' | wc -l | tr -d ' ')
    if [ "${TM_COUNT}" != "" ] && [ ${TM_COUNT} -ge 1 ]; then
        echo "✅ TaskManagers registered: ${TM_COUNT}"
        break
    fi
    if [ $i -eq $MAX_RETRIES ]; then
        echo "❌ No TaskManagers registered after ${MAX_RETRIES} attempts"
        exit 1
    fi
    echo "   Attempt $i/$MAX_RETRIES - waiting ${RETRY_INTERVAL}s... (registered: ${TM_COUNT:-0})"
    sleep $RETRY_INTERVAL
done

# Check if job is already running
echo ""
echo "🔍 Checking for existing jobs..."
RUNNING_JOBS=$(curl -s "${FLINK_URL}/jobs" | grep -o '"status":"RUNNING"' | wc -l || echo "0")

if [ "$RUNNING_JOBS" -gt 0 ]; then
    echo "⚠️  Found $RUNNING_JOBS running job(s). Checking if telemetry processor is already running..."
    # List jobs to check
    curl -s "${FLINK_URL}/jobs/overview" | head -50
fi

# Upload the JAR
echo ""
echo "📤 Uploading JAR to Flink..."
UPLOAD_RESPONSE=$(curl -s -X POST -H "Expect:" \
    -F "jarfile=@${JAR_PATH}" \
    "${FLINK_URL}/jars/upload")

echo "Upload response: ${UPLOAD_RESPONSE}"

# Extract the JAR ID from response
JAR_ID=$(echo "$UPLOAD_RESPONSE" | grep -o '"filename":"[^"]*"' | sed 's/"filename":"//;s/"//' | sed 's|.*/||')

if [ -z "$JAR_ID" ]; then
    echo "❌ Failed to upload JAR or extract JAR ID"
    echo "Response: ${UPLOAD_RESPONSE}"
    exit 1
fi

echo "✅ JAR uploaded successfully: ${JAR_ID}"

# Submit the job
echo ""
echo "🚀 Submitting Flink job..."
SUBMIT_RESPONSE=$(curl -s -X POST \
    "${FLINK_URL}/jars/${JAR_ID}/run" \
    -H "Content-Type: application/json" \
    -d '{
        "programArgs": "--kafka.bootstrap.servers='${KAFKA_BOOTSTRAP_SERVERS:-kafka:29092}'"
    }')

echo "Submit response: ${SUBMIT_RESPONSE}"

# Extract job ID
JOB_ID=$(echo "$SUBMIT_RESPONSE" | grep -o '"jobid":"[^"]*"' | sed 's/"jobid":"//;s/"//')

if [ -z "$JOB_ID" ]; then
    echo "❌ Failed to submit job or extract Job ID"
    echo "Response: ${SUBMIT_RESPONSE}"
    exit 1
fi

echo ""
echo "========================================="
echo " ✅ Job Submitted Successfully!"
echo "========================================="
echo ""
echo "Job ID: ${JOB_ID}"
echo "Dashboard: ${FLINK_URL}/#/job/${JOB_ID}"
echo ""

# Keep container running and monitor job status
echo "📊 Monitoring job status..."
while true; do
    JOB_STATUS=$(curl -s "${FLINK_URL}/jobs/${JOB_ID}" | grep -o '"state":"[^"]*"' | head -1 | sed 's/"state":"//;s/"//')
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${TIMESTAMP}] Job ${JOB_ID}: ${JOB_STATUS}"
    
    if [ "$JOB_STATUS" = "FAILED" ] || [ "$JOB_STATUS" = "CANCELED" ]; then
        echo "❌ Job ended with status: ${JOB_STATUS}"
        # Get exception details
        curl -s "${FLINK_URL}/jobs/${JOB_ID}/exceptions" | head -100
        exit 1
    fi
    
    sleep 30
done
