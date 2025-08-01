# Wellfound Job Scraper - Production Docker Image
# Multi-stage build for optimized production deployment

# Build stage
FROM maven:3.9.6-openjdk-11-slim AS builder

# Set working directory
WORKDIR /app

# Copy POM file for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Verify JAR file
RUN ls -la target/ && \
    test -f target/wellfound-scraper-1.0.0.jar

# Production stage
FROM openjdk:11-jre-slim

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r scraper && useradd -r -g scraper scraper

# Create application directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/wellfound-scraper-1.0.0.jar app.jar

# Copy configuration files
COPY --from=builder /app/src/main/resources/application.properties ./
COPY --from=builder /app/src/main/resources/logback.xml ./

# Create logs directory
RUN mkdir -p logs && chown -R scraper:scraper /app

# Switch to application user
USER scraper

# Expose port (if running as service)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD java -jar app.jar --version || exit 1

# Application metadata
LABEL maintainer="Gigacrawl Team" \
      version="1.0.0" \
      description="Production-ready Wellfound job scraper" \
      target-quality="75%" \
      features="multi-strategy-extraction,anti-bot-evasion,quality-scoring"

# Default command
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--help"]

# Usage examples:
# docker build -t wellfound-scraper .
# docker run wellfound-scraper --version
# docker run -e DATABASE_URL=jdbc:postgresql://host:5432/db wellfound-scraper --companies 10
# docker run -e DATABASE_URL=jdbc:postgresql://host:5432/db wellfound-scraper --full 20