FROM alpine:3.18

# Install required runtime packages
RUN apk add --no-cache \
    bash \
    curl \
    docker-cli \
    postgresql-client \
    redis \
    dcron \
    tzdata \
    zip \
    gzip \
    tar \
    rsync \
    rclone \
    net-tools \
    python3 \
    py3-pip

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create working directories
RUN mkdir -p /app/scripts \
    /app/logs \
    /backups \
    /var/log/cron \
    /etc/cron.d

WORKDIR /app

# Copy Python requirements
COPY requirements.txt /app/

# Install Python dependencies with build tools, then remove build tools
RUN apk add --no-cache --virtual .build-deps \
        gcc \
        musl-dev \
        python3-dev \
        libffi-dev \
        make \
    && pip3 install --upgrade pip \
    && pip3 install -r requirements.txt \
    && apk del .build-deps

# Create log files
RUN touch /var/log/cron/backup-manager.log \
    && touch /app/logs/backup-manager.log

# Copy scripts and entrypoint
# COPY scripts/ /app/scripts/
# RUN chmod +x /app/scripts/*.sh

# COPY configuration files if needed
# COPY config/ /app/config/

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Set environment variables
ENV PYTHONPATH=/app
# ENV PATH="/app/scripts:${PATH}"

ENTRYPOINT ["/app/entrypoint.sh"]

# FROM alpine:3.18

# # Install required packages
# RUN apk add --no-cache \
#     bash \
#     curl \
#     docker-cli \
#     postgresql-client \
#     redis \
#     dcron \
#     tzdata \
#     zip \
#     gzip \
#     tar \
#     rsync \
#     python3 \
#     py3-pip

# # Set timezone
# ENV TZ=Asia/Kolkata
# RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# # Create working directories
# RUN mkdir -p /app/scripts
# RUN mkdir -p /app/logs
# RUN mkdir -p /backups
# RUN mkdir -p /var/log/cron

# WORKDIR /app

# # Copy scripts
# # COPY scripts/ /app/scripts/
# # RUN chmod +x /app/scripts/*.sh

# # Copy configuration
# # COPY config/ /app/config/

# # Install Python dependencies for monitoring
# COPY requirements.txt /app/
# RUN pip3 install -r requirements.txt

# # Create cron jobs directory
# RUN mkdir -p /etc/cron.d

# # Create log files
# RUN touch /var/log/cron/backup-manager.log
# RUN touch /app/logs/backup-manager.log

# # Copy entrypoint
# COPY entrypoint.sh /app/entrypoint.sh
# RUN chmod +x /app/entrypoint.sh

# # Set environment variables
# ENV PYTHONPATH=/app
# # ENV PATH="/app/scripts:${PATH}"

# ENTRYPOINT ["/app/entrypoint.sh"]
