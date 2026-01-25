# SmartFleet Dashboard - Production Dockerfile
# Multi-stage build for optimized image

# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY nx.json ./
COPY tsconfig*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source code
COPY apps/ ./apps/
COPY libs/ ./libs/

# Build the application
RUN npx nx build dashboard --configuration=production

# Stage 2: Production
FROM nginx:alpine

# Copy custom nginx config
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf

# Copy built application
COPY --from=builder /app/dist/apps/dashboard/browser /usr/share/nginx/html

# Add healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --quiet --tries=1 --spider http://localhost:80/health || exit 1

# Expose port
EXPOSE 80 443

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
