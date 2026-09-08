#!/bin/bash

# ==============================================================================
# Titan AHS Streaming Platform - Dynamic Port Resolver
# Detects port collisions and assigns available ports for Docker Compose services.
# ==============================================================================

# Track allocated ports in current run to prevent assigning same port to multiple services
ALLOCATED_PORTS=()

# Load existing .env if present (to respect pre-set configurations)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

if [ -f "$ENV_FILE" ]; then
    while IFS='=' read -r key value || [ -n "$key" ]; do
        # Ignore comments and empty lines
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue
        # Trim leading/trailing whitespace
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        if [ -n "$key" ] && [ -z "${!key}" ]; then
            export "$key"="$value"
        fi
    done < "$ENV_FILE"
fi

# Check if a port is in use on the host
is_port_in_use() {
    local port="$1"
    # Check using lsof (macOS / Linux)
    if command -v lsof >/dev/null 2>&1; then
        if lsof -iTCP:"$port" -sTCP:LISTEN -P -n >/dev/null 2>&1; then
            return 0
        fi
    fi
    # Fallback to nc (netcat)
    if command -v nc >/dev/null 2>&1; then
        if nc -z 127.0.0.1 "$port" >/dev/null 2>&1 || nc -z 0.0.0.0 "$port" >/dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# Check if the service's own Docker container is already running and bound to this port
is_own_container_running() {
    local container_name="$1"
    local port="$2"
    if [ -z "$container_name" ]; then
        return 1
    fi
    if command -v docker >/dev/null 2>&1; then
        if docker ps --filter "name=^/${container_name}$" --filter "status=running" --format '{{.Ports}}' 2>/dev/null | grep -q ":${port}->"; then
            return 0
        fi
    fi
    return 1
}

# Check if a port was already assigned in this resolution pass
is_port_allocated() {
    local port="$1"
    for p in "${ALLOCATED_PORTS[@]}"; do
        if [ "$p" -eq "$port" ]; then
            return 0
        fi
    done
    return 1
}

# Find an available port, incrementing until a free port is found
find_available_port() {
    local default_port="$1"
    local service_label="$2"
    local container_name="$3"
    local user_override="$4"

    local desired_port="${user_override:-$default_port}"
    local port="$desired_port"
    local max_attempts=100
    local attempts=0

    # If this service's own container is already running and using this port, keep it
    if is_own_container_running "$container_name" "$desired_port"; then
        ALLOCATED_PORTS+=("$desired_port")
        echo "$desired_port"
        return 0
    fi

    # Check for collisions and increment until an available port is found
    while { is_port_in_use "$port" || is_port_allocated "$port"; } && [ "$attempts" -lt "$max_attempts" ]; do
        attempts=$((attempts + 1))
        port=$((port + 1))
    done

    if [ "$port" -ne "$desired_port" ]; then
        echo "⚠️  Port collision on ${desired_port} for ${service_label}. Reassigning to available port ${port}." >&2
    fi

    ALLOCATED_PORTS+=("$port")
    echo "$port"
}

# Resolve ports for all services
resolve_all_ports() {
    echo "🔍 Checking port availability..." >&2

    ZOOKEEPER_PORT=$(find_available_port 2181 "Zookeeper" "ahs-zookeeper" "$ZOOKEEPER_PORT")
    KAFKA_PORT=$(find_available_port 9092 "Kafka Broker" "ahs-kafka" "$KAFKA_PORT")
    KAFKA_JMX_PORT=$(find_available_port 9101 "Kafka JMX" "ahs-kafka" "$KAFKA_JMX_PORT")
    KAFKA_UI_PORT=$(find_available_port 8080 "Kafka UI" "ahs-kafka-ui" "$KAFKA_UI_PORT")
    POSTGRES_PORT=$(find_available_port 5432 "PostgreSQL" "ahs-postgres" "$POSTGRES_PORT")
    REDIS_PORT=$(find_available_port 6379 "Redis" "ahs-redis" "$REDIS_PORT")
    DATA_GENERATOR_PORT=$(find_available_port 8082 "Data Generator" "ahs-data-generator" "$DATA_GENERATOR_PORT")
    FLEET_MANAGEMENT_PORT=$(find_available_port 8083 "Fleet Management" "ahs-fleet-management" "$FLEET_MANAGEMENT_PORT")
    VEHICLE_SERVICE_PORT=$(find_available_port 8084 "Vehicle Service" "ahs-vehicle-service" "$VEHICLE_SERVICE_PORT")
    PROMETHEUS_PORT=$(find_available_port 9090 "Prometheus UI" "ahs-prometheus-ui" "$PROMETHEUS_PORT")
    GRAFANA_PORT=$(find_available_port 3000 "Grafana UI" "ahs-grafana-ui" "$GRAFANA_PORT")

    export ZOOKEEPER_PORT
    export KAFKA_PORT
    export KAFKA_JMX_PORT
    export KAFKA_UI_PORT
    export POSTGRES_PORT
    export REDIS_PORT
    export DATA_GENERATOR_PORT
    export FLEET_MANAGEMENT_PORT
    export VEHICLE_SERVICE_PORT
    export PROMETHEUS_PORT
    export GRAFANA_PORT

    # Write resolved configuration to .env file
    cat <<EOF > "$ENV_FILE"
# ==============================================================================
# Titan AHS Streaming Platform - Port Configuration
# Auto-generated by resolve-ports.sh
# ==============================================================================
ZOOKEEPER_PORT=${ZOOKEEPER_PORT}
KAFKA_PORT=${KAFKA_PORT}
KAFKA_JMX_PORT=${KAFKA_JMX_PORT}
KAFKA_UI_PORT=${KAFKA_UI_PORT}
POSTGRES_PORT=${POSTGRES_PORT}
REDIS_PORT=${REDIS_PORT}
DATA_GENERATOR_PORT=${DATA_GENERATOR_PORT}
FLEET_MANAGEMENT_PORT=${FLEET_MANAGEMENT_PORT}
VEHICLE_SERVICE_PORT=${VEHICLE_SERVICE_PORT}
PROMETHEUS_PORT=${PROMETHEUS_PORT}
GRAFANA_PORT=${GRAFANA_PORT}
EOF
}

resolve_all_ports

# If run directly (not sourced), print summary
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    echo ""
    echo "========================================="
    echo " Resolved Service Ports"
    echo "========================================="
    echo "  Zookeeper:          ${ZOOKEEPER_PORT}"
    echo "  Kafka:              ${KAFKA_PORT}"
    echo "  Kafka JMX:          ${KAFKA_JMX_PORT}"
    echo "  Kafka UI:           ${KAFKA_UI_PORT}"
    echo "  PostgreSQL:         ${POSTGRES_PORT}"
    echo "  Redis:              ${REDIS_PORT}"
    echo "  Data Generator:     ${DATA_GENERATOR_PORT}"
    echo "  Fleet Management:   ${FLEET_MANAGEMENT_PORT}"
    echo "  Vehicle Service:    ${VEHICLE_SERVICE_PORT}"
    echo "  Prometheus:         ${PROMETHEUS_PORT}"
    echo "  Grafana:            ${GRAFANA_PORT}"
    echo "========================================="
    echo "Port configuration saved to: ${ENV_FILE}"
fi
