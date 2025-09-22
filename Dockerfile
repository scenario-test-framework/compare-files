# Build stage
FROM maven:3.8-openjdk-8 AS builder

ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

WORKDIR /app

# Configure Maven settings for GitHub Packages
RUN mkdir -p ~/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
                              http://maven.apache.org/xsd/settings-1.0.0.xsd"> \
      <servers> \
        <server> \
          <id>github</id> \
          <username>suwa-sh</username> \
          <password>'${GITHUB_TOKEN}'</password> \
        </server> \
      </servers> \
    </settings>' > ~/.m2/settings.xml

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src
COPY env ./env

# Build the application
RUN mvn clean package -DskipTests -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

# Runtime stage
FROM openjdk:8-jre-slim

WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/lib /app/config /app/bin /app/sample /app/logs

# Copy built artifacts from builder stage
COPY --from=builder /app/target/compare_files_*/lib/* /app/lib/
COPY --from=builder /app/target/compare_files_*/config/* /app/config/
COPY --from=builder /app/target/compare_files_*/bin/* /app/bin/
COPY --from=builder /app/target/compare_files_*/sample/* /app/sample/
COPY --from=builder /app/target/compare_files_*/README.md /app/
COPY --from=builder /app/target/compare_files_*/version.txt /app/

# Make scripts executable
RUN chmod +x /app/bin/*.sh

# Set environment variables
ENV COMPAREFILES_JAVA_OPT="-Xmx1024m"
ENV COMPAREFILES_CLASSPATH="/app/config:/app/lib/*"

# Create volume for input/output data
VOLUME ["/data"]

# Default working directory for file operations
WORKDIR /data

# Default command (show help for compare_files.sh)
CMD ["/app/bin/compare_files.sh", "--help"]