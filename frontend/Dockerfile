# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy full source (src/, configs). .dockerignore excludes node_modules, dist
COPY . .

# Allow override at build time for local docker
ARG VITE_API_BASE_URL
ARG VITE_INTAKE_API_BASE_URL
ARG VITE_API_TIMEOUT_MS
ARG VITE_INTAKE_API_TIMEOUT_MS
ARG VITE_INTAKE_POLL_TIMEOUT_MS
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_INTAKE_API_BASE_URL=${VITE_INTAKE_API_BASE_URL}
ENV VITE_API_TIMEOUT_MS=${VITE_API_TIMEOUT_MS}
ENV VITE_INTAKE_API_TIMEOUT_MS=${VITE_INTAKE_API_TIMEOUT_MS}
ENV VITE_INTAKE_POLL_TIMEOUT_MS=${VITE_INTAKE_POLL_TIMEOUT_MS}

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
