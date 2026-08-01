FROM redis:7-alpine

# Build argument for environment
ARG ENVIRONMENT=dev

# Install additional tools and cron
RUN apk add --no-cache \
    curl \
    bash \
    dcron \
    tzdata \
    zip \
    gzip

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create directories
RUN mkdir -p /backups && chmod 777 /backups
# RUN mkdir -p /scripts && chmod 755 /scripts
RUN mkdir -p /var/log/cron && chmod 755 /var/log/cron

# Copy Redis configuration based on environment
COPY config/${ENVIRONMENT}/redis.conf /usr/local/etc/redis/redis.conf

# Copy backup and maintenance scripts
COPY scripts/ /scripts/
RUN chmod +x /scripts/*.sh

# Set environment variable
ENV ENVIRONMENT=${ENVIRONMENT}

# Copy and set custom entrypoint
COPY entrypoint.sh /usr/local/bin/custom-entrypoint.sh
RUN chmod +x /usr/local/bin/custom-entrypoint.sh

# Create log files
RUN touch /var/log/cron/cron.log
RUN touch /var/log/cron/backup.log
RUN touch /var/log/cron/health.log

EXPOSE 6379

ENTRYPOINT ["/usr/local/bin/custom-entrypoint.sh"]
