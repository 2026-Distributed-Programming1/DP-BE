#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/home/ec2-user"
ENV_FILE="${APP_DIR}/.env"
COMPOSE_FILE="${APP_DIR}/docker-compose.yml"

echo "Checking Docker installation..."
if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed. Installing Docker..."
    if command -v apt-get >/dev/null 2>&1; then
        sudo apt-get update
        sudo apt-get install -y docker.io
    elif command -v yum >/dev/null 2>&1; then
        sudo yum install -y docker
    elif command -v dnf >/dev/null 2>&1; then
        sudo dnf install -y docker
    else
        echo "No supported package manager found. Install Docker manually and rerun this script."
        exit 1
    fi

    sudo systemctl start docker
    sudo systemctl enable docker
elif command -v systemctl >/dev/null 2>&1; then
    sudo systemctl start docker
    sudo systemctl enable docker
fi

DOCKER_CMD="docker"
DOCKER_NEEDS_SUDO=false
if ! docker info >/dev/null 2>&1; then
    DOCKER_CMD="sudo docker"
    DOCKER_NEEDS_SUDO=true
fi

if ${DOCKER_CMD} compose version >/dev/null 2>&1; then
    COMPOSE_CMD="${DOCKER_CMD} compose"
elif command -v docker-compose >/dev/null 2>&1; then
    if [ "${DOCKER_NEEDS_SUDO}" = "true" ]; then
        COMPOSE_CMD="sudo docker-compose"
    else
        COMPOSE_CMD="docker-compose"
    fi

    if ! ${COMPOSE_CMD} version >/dev/null 2>&1; then
        echo "Docker Compose is installed but cannot be executed."
        exit 1
    fi
else
    echo "Docker Compose is not installed. Installing standalone docker-compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    COMPOSE_CMD="sudo docker-compose"
fi

echo "Starting deployment: $(date)"
cd "${APP_DIR}"

if [ ! -f "${ENV_FILE}" ]; then
    echo ".env file was not found at ${ENV_FILE}"
    exit 1
fi

if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "docker-compose.yml file was not found at ${COMPOSE_FILE}"
    exit 1
fi

chmod 600 "${ENV_FILE}"

set -a
source "${ENV_FILE}"
set +a

if [ -z "${DOCKER_IMAGE:-}" ]; then
    echo "DOCKER_IMAGE is required in .env"
    exit 1
fi

echo "Pulling latest application image: ${DOCKER_IMAGE}"
${DOCKER_CMD} pull "${DOCKER_IMAGE}"

echo "Restarting containers..."
${COMPOSE_CMD} --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --force-recreate --remove-orphans

echo "Removing unused Docker images..."
${DOCKER_CMD} image prune -f

echo "Deployment completed."
