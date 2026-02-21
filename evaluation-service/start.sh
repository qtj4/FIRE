#!/bin/bash

# Evaluation Service Docker Compose Startup Script
# Author: FIRE Team
# Version: 1.0.0

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  Evaluation Service${NC}"
    echo -e "${BLUE}  Docker Compose Setup${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    print_status "Docker is running"
}

# Check if Docker Compose is available
check_docker_compose() {
    if ! command -v docker-compose > /dev/null 2>&1; then
        if ! docker compose version > /dev/null 2>&1; then
            print_error "Docker Compose is not available. Please install Docker Compose."
            exit 1
        else
            DOCKER_COMPOSE="docker compose"
        fi
    else
        DOCKER_COMPOSE="docker-compose"
    fi
    print_status "Using: $DOCKER_COMPOSE"
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    mkdir -p grafana/provisioning/datasources
    mkdir -p grafana/provisioning/dashboards
}

# Create Grafana datasource configuration
create_grafana_config() {
    print_status "Creating Grafana configuration..."
    
    cat > grafana/provisioning/datasources/prometheus.yml << EOF
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF

    cat > grafana/provisioning/dashboards/dashboard.yml << EOF
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
EOF
}

# Start services
start_services() {
    print_status "Starting Evaluation Service infrastructure..."
    
    # Start infrastructure services first
    print_status "Starting infrastructure services (PostgreSQL, Redis, Kafka)..."
    $DOCKER_COMPOSE up -d postgres redis zookeeper kafka
    
    # Wait for infrastructure to be ready
    print_status "Waiting for infrastructure services to be ready..."
    sleep 30
    
    # Start remaining services
    print_status "Starting application and monitoring services..."
    $DOCKER_COMPOSE up -d evaluation-service prometheus grafana kafka-ui
    
    print_status "All services started successfully!"
}

# Wait for services to be healthy
wait_for_health() {
    print_status "Waiting for services to become healthy..."
    
    # Wait for Evaluation Service
    for i in {1..30}; do
        if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
            print_status "Evaluation Service is healthy!"
            break
        fi
        if [ $i -eq 30 ]; then
            print_warning "Evaluation Service might not be fully ready yet"
        fi
        sleep 2
    done
}

# Show service URLs
show_urls() {
    print_header
    echo -e "${GREEN}Services are running!${NC}"
    echo ""
    echo -e "${BLUE}Application URLs:${NC}"
    echo -e "  • Evaluation Service: ${GREEN}http://localhost:8081${NC}"
    echo -e "  • Health Check: ${GREEN}http://localhost:8081/actuator/health${NC}"
    echo -e "  • Metrics: ${GREEN}http://localhost:8081/actuator/prometheus${NC}"
    echo ""
    echo -e "${BLUE}Monitoring URLs:${NC}"
    echo -e "  • Prometheus: ${GREEN}http://localhost:9091${NC}"
    echo -e "  • Grafana: ${GREEN}http://localhost:3000${NC} (admin/admin123)"
    echo -e "  • Kafka UI: ${GREEN}http://localhost:8090${NC}"
    echo ""
    echo -e "${BLUE}Database URLs:${NC}"
    echo -e "  • PostgreSQL: ${GREEN}localhost:1111${NC} (postgres/postgres)"
    echo -e "  • Redis: ${GREEN}localhost:2222${NC} (password: fire123)"
    echo -e "  • Kafka: ${GREEN}localhost:4444${NC}"
    echo ""
}

# Show logs
show_logs() {
    print_status "Showing logs for all services..."
    $DOCKER_COMPOSE logs -f
}

# Stop services
stop_services() {
    print_status "Stopping all services..."
    $DOCKER_COMPOSE down
    print_status "All services stopped"
}

# Clean up
cleanup() {
    print_status "Cleaning up containers and volumes..."
    $DOCKER_COMPOSE down -v --remove-orphans
    docker system prune -f
    print_status "Cleanup completed"
}

# Main execution
main() {
    print_header
    
    case "${1:-start}" in
        start)
            check_docker
            check_docker_compose
            create_directories
            create_grafana_config
            start_services
            wait_for_health
            show_urls
            ;;
        logs)
            show_logs
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 5
            main start
            ;;
        cleanup)
            cleanup
            ;;
        status)
            $DOCKER_COMPOSE ps
            ;;
        *)
            echo "Usage: $0 {start|stop|restart|logs|cleanup|status}"
            echo ""
            echo "Commands:"
            echo "  start   - Start all services"
            echo "  stop    - Stop all services"
            echo "  restart - Restart all services"
            echo "  logs    - Show logs for all services"
            echo "  cleanup - Remove all containers and volumes"
            echo "  status  - Show status of all services"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"
