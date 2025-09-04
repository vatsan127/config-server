FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required packages
RUN apk add --no-cache git

# Create base config directory
RUN mkdir -p /config && \
    chmod 755 /config

# Copy the application JAR
COPY target/config-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# JVM Configuration with Parallel GC and optimizations for vault service
ENTRYPOINT ["java", \
    # Garbage Collection Configuration - Parallel GC for throughput
    "-XX:+UseParallelGC", \
    "-XX:ParallelGCThreads=4", \
    "-XX:MaxGCPauseMillis=200", \
    # Memory Configuration
    "-Xms512m", \
    "-Xmx2g", \
    "-XX:NewRatio=3", \
    # Performance Optimizations
    "-XX:+OptimizeStringConcat", \
    "-XX:+UseStringDeduplication", \
    "-XX:+UseCompressedOops", \
    # Security Optimizations for encryption workloads
    "-Djava.security.egd=file:/dev/./urandom", \
    # Spring Boot specific optimizations
    "-Dspring.backgroundpreinitializer.ignore=true", \
    "-Dspring.jmx.enabled=false", \
    # Monitoring and debugging
    "-XX:+PrintGCDetails", \
    "-XX:+PrintGCTimeStamps", \
    "-XX:+PrintGCApplicationStoppedTime", \
    # Application JAR
    "-jar", "app.jar"]