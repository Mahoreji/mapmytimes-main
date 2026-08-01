FROM postgis/postgis:15-3.4

# Build argument
ARG ENVIRONMENT=dev

# Install required build tools (Debian-based)
RUN apt-get update && apt-get install -y \
    curl \
    bash \
    postgresql-server-dev-15 \
    cron \
    tzdata \
    zip \
    gzip \
    git \
    build-essential \
    libssl-dev \
    libkrb5-dev \
    clang-13 \
    llvm-13 \
    && rm -rf /var/lib/apt/lists/*

# -----------------------------
# Install pgvector & pg_cron
# -----------------------------
RUN git clone https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && cd /tmp/pgvector && make && make install \
    && git clone https://github.com/citusdata/pg_cron.git /tmp/pg_cron \
    && cd /tmp/pg_cron && make && make install \
    && git clone -b REL_15_STABLE https://github.com/pgaudit/pgaudit.git /tmp/pgaudit \
    && cd /tmp/pgaudit && make install USE_PGXS=1 \
    && rm -rf /tmp/pgvector /tmp/pg_cron /tmp/pgaudit

# -----------------------------
# Timezone setup
# -----------------------------
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# -----------------------------
# Create directories
# -----------------------------
RUN mkdir -p /backups && chmod 777 /backups
RUN mkdir -p /scripts && chmod 755 /scripts
RUN mkdir -p /var/log/cron && chmod 755 /var/log/cron

# -----------------------------
# Copy initialization scripts
# -----------------------------
COPY init-scripts/ /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/*.sh

# -----------------------------
# Copy custom scripts
# -----------------------------
COPY scripts/ /scripts/
RUN chmod +x /scripts/*.sh

# -----------------------------
# Set environment
# -----------------------------
ENV ENVIRONMENT=${ENVIRONMENT}

# -----------------------------
# Custom entrypoint
# -----------------------------
COPY entrypoint.sh /usr/local/bin/custom-entrypoint.sh
RUN chmod +x /usr/local/bin/custom-entrypoint.sh

# -----------------------------
# Cron setup
# -----------------------------
RUN mkdir -p /var/log/cron && touch /var/log/cron/cron.log

EXPOSE 5432

ENTRYPOINT ["/usr/local/bin/custom-entrypoint.sh"]
