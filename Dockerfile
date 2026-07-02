# Build stage
FROM golang:1.25 AS builder

WORKDIR /app

# Download dependencies first to leverage Docker cache
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY cmd ./cmd
COPY internal ./internal

# Build static binaries
# 互換性のため、旧イメージのエントリポイント名 (*.sh) へのシンボリックリンクも用意する
ENV CGO_ENABLED=0
RUN go build -trimpath -ldflags "-s -w" -o /out/bin/compare_files ./cmd/compare_files && \
    go build -trimpath -ldflags "-s -w" -o /out/bin/compare_regex ./cmd/compare_regex && \
    ln -s compare_files /out/bin/compare_files.sh && \
    ln -s compare_regex /out/bin/compare_regex.sh

# Runtime stage
FROM gcr.io/distroless/static-debian12

WORKDIR /app

# Copy binaries (シンボリックリンク含む)
COPY --from=builder /out/bin /app/bin

# Copy default configuration and samples
COPY dist/config /app/config
COPY dist/sample /app/sample
COPY README.md /app/

# Set environment variables (COMPAREFILES_JAVA_OPT は後方互換のため参照される)
ENV COMPAREFILES_CLASSPATH="/app/config"

# Create volume for input/output data
VOLUME ["/data"]

# Default working directory for file operations
WORKDIR /data

# Default command (show help)
CMD ["/app/bin/compare_files", "--help"]
