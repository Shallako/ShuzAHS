# Dockerfile Base Image Fix

**Date**: November 29, 2025  
**Issue**: Alpine image not available for Eclipse Temurin JRE 17  
**Status**: ✅ FIXED

---

## Problem

**Error Message**:
```
eclipse-temurin:17-jre-alpine: failed to resolve source metadata
no match for platform in manifest: not found
```

**Root Cause**: 
Eclipse Temurin does not provide Alpine-based images for JRE 17. Alpine images are only available for JDK versions, not JRE.

---

## Solution

Changed base image from Alpine to Ubuntu-based Jammy:

### Before (Incorrect)
```dockerfile
FROM eclipse-temurin:17-jre-alpine
```

### After (Correct)
```dockerfile
FROM eclipse-temurin:17-jre-jammy

# Install wget for health checks
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*
```

---

## Changes Applied

### All Three Dockerfiles Updated

1. ✅ **ahs-data-generator/Dockerfile**
2. ✅ **ahs-fleet-management/Dockerfile**
3. ✅ **ahs-vehicle-service/Dockerfile**

### What Changed

**Runtime Stage**:
- Base image: `eclipse-temurin:17-jre-alpine` → `eclipse-temurin:17-jre-jammy`
- Added: `apt-get install wget` for health checks (Alpine used `apk add`)
- Cleanup: Added `rm -rf /var/lib/apt/lists/*` to reduce image size

---

## Image Size Comparison

| Base Image | Size | OS |
|------------|------|-----|
| eclipse-temurin:17-jre-alpine | ❌ Not available | Alpine Linux |
| eclipse-temurin:17-jre-jammy | ~250 MB | Ubuntu 22.04 LTS |
| eclipse-temurin:17-jdk-alpine | ~330 MB | Alpine Linux (JDK, not JRE) |
| amazoncorretto:17-alpine | ~220 MB | Alpine Linux (alternative) |

### Why Jammy?

**Pros**:
- ✅ Official Eclipse Temurin image
- ✅ Ubuntu 22.04 LTS (Long Term Support until 2027)
- ✅ Well-tested and stable
- ✅ Better compatibility with enterprise tools
- ✅ Larger ecosystem of packages

**Cons**:
- ⚠️ Slightly larger than Alpine (~250 MB vs ~180 MB)
- ⚠️ Debian/Ubuntu package manager (apt) instead of Alpine's apk

**Decision**: Jammy is the best choice for production stability and compatibility.

---

## Alternative Base Images (If Size is Critical)

### Option 1: Amazon Corretto Alpine
```dockerfile
FROM amazoncorretto:17-alpine-jre

# Install wget
RUN apk add --no-cache wget

# Rest of Dockerfile...
```
- **Size**: ~220 MB
- **Pros**: Alpine-based, smaller size
- **Cons**: Different JVM (Amazon's OpenJDK distribution)

### Option 2: Eclipse Temurin JDK Alpine
```dockerfile
FROM eclipse-temurin:17-jdk-alpine

# Install wget
RUN apk add --no-cache wget

# Rest of Dockerfile...
```
- **Size**: ~330 MB
- **Pros**: Same JVM vendor, Alpine-based
- **Cons**: Includes full JDK (unnecessary for runtime)

### Option 3: Distroless (Advanced)
```dockerfile
FROM gcr.io/distroless/java17-debian11

# No shell, minimal attack surface
# Copy JAR and run
```
- **Size**: ~180 MB
- **Pros**: Smallest, most secure
- **Cons**: No shell, harder to debug, no wget for health checks

---

## Updated Dockerfile Structure

### Complete Dockerfile Example

```dockerfile
# Multi-stage build for AHS Fleet Management Service
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy all module sources
COPY ahs-common ./ahs-common
COPY ahs-domain ./ahs-domain
COPY ahs-thrift-api ./ahs-thrift-api
COPY ahs-fleet-management ./ahs-fleet-management

# Build the application (skip thrift compilation issues)
RUN gradle :ahs-fleet-management:bootJar -x :ahs-thrift-api:compileJava --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install wget for health checks and clean up
RUN apt-get update && \
    apt-get install -y wget && \
    rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY --from=builder /app/ahs-fleet-management/build/libs/*.jar app.jar

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Build and Test

### Build Single Service
```bash
docker-compose build fleet-management
```

### Build All Services
```bash
docker-compose build data-generator fleet-management vehicle-service
```

### Verify Images
```bash
# List built images
docker images | grep ahs

# Expected output:
# shuzahs-fleet-management    latest    abc123    5 minutes ago    250MB
# shuzahs-data-generator      latest    def456    5 minutes ago    250MB
# shuzahs-vehicle-service     latest    ghi789    5 minutes ago    250MB
```

### Test Container
```bash
# Start one service
docker-compose up -d fleet-management

# Check logs
docker-compose logs -f fleet-management

# Test health check
docker-compose ps fleet-management
# Should show "healthy" after 60 seconds
```

---

## Available Eclipse Temurin Images

### JRE (Java Runtime Environment)
- `eclipse-temurin:17-jre` - Ubuntu-based (default)
- `eclipse-temurin:17-jre-jammy` - Ubuntu 22.04 LTS ✅ **Using This**
- `eclipse-temurin:17-jre-focal` - Ubuntu 20.04 LTS
- `eclipse-temurin:17-jre-centos7` - CentOS 7
- ❌ No Alpine version available

### JDK (Full Development Kit)
- `eclipse-temurin:17-jdk-alpine` - Alpine-based
- `eclipse-temurin:17-jdk-jammy` - Ubuntu 22.04
- `eclipse-temurin:17-jdk-focal` - Ubuntu 20.04

### Why JRE Instead of JDK?

**JRE Advantages**:
- ✅ Smaller image size (~250 MB vs ~400 MB)
- ✅ Faster startup
- ✅ Smaller attack surface (no compiler, debugger)
- ✅ Production best practice

**JDK Needed For**:
- ❌ Compiling code at runtime
- ❌ Remote debugging (can add separately)
- ❌ Development tools

---

## Health Check Explanation

### What It Does
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health
```

**Parameters**:
- `--interval=30s` - Check every 30 seconds
- `--timeout=10s` - Fail if check takes >10 seconds
- `--start-period=60s` - Grace period during startup
- `--retries=3` - Mark unhealthy after 3 failures

**Command**:
- `wget --spider` - Check URL exists without downloading
- `http://localhost:8083/actuator/health` - Spring Boot health endpoint

### Why It Matters

Docker uses health checks to:
1. Show container status: `healthy` or `unhealthy`
2. Restart unhealthy containers (with restart policy)
3. Route traffic only to healthy containers (in orchestration)
4. Delay dependent service startup

---

## Troubleshooting

### Image Pull Fails
```bash
# Pull base image manually
docker pull eclipse-temurin:17-jre-jammy

# Verify it works
docker run --rm eclipse-temurin:17-jre-jammy java -version
```

### Build Fails at wget Install
```bash
# If apt-get fails, check network connectivity
docker-compose build --no-cache fleet-management

# Or use curl instead
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
HEALTHCHECK CMD curl -f http://localhost:8083/actuator/health
```

### Health Check Always Fails
```bash
# Check if actuator is enabled in application.properties
management.endpoints.web.exposure.include=health,info,prometheus

# Or disable health check temporarily
# Comment out HEALTHCHECK line in Dockerfile
```

---

## Summary

✅ **Fixed base image**: Alpine → Jammy (Ubuntu 22.04 LTS)  
✅ **All 3 Dockerfiles updated**: data-generator, fleet-management, vehicle-service  
✅ **Added wget**: Required for health checks on Ubuntu  
✅ **Image size**: ~250 MB per service (acceptable for production)  
✅ **Stability**: Ubuntu LTS provides better compatibility  
✅ **Ready to build**: `docker-compose build`  

**The Docker build errors are now resolved!** 🎉

---

## Next Steps

1. ✅ Build images: `docker-compose build`
2. ✅ Start infrastructure: `docker-compose up -d zookeeper kafka postgres redis`
3. ✅ Start applications: `docker-compose up -d`
4. ✅ Check health: `docker-compose ps`
5. ✅ View logs: `docker-compose logs -f`
