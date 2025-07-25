# hadolint global ignore=DL3006
ARG BASE_IMAGE="alpine:3.22.1"
ARG BASE_DIGEST="sha256:4bcff63911fcb4448bd4fdacec207030997caf25e9bea4045fa6c8c44de311d1"

# Prepare tasklist Distribution
FROM ${BASE_IMAGE}@${BASE_DIGEST} AS prepare

ARG DISTBALL="dist/target/camunda-zeebe-*.tar.gz"
WORKDIR /tmp/tasklist

# download tasklist
COPY ${DISTBALL} tasklist.tar.gz
RUN tar xzvf tasklist.tar.gz --strip 1 && \
    rm tasklist.tar.gz

### Base image ###
# hadolint ignore=DL3006
FROM ${BASE_IMAGE}@${BASE_DIGEST} AS base

# Install Tini
RUN apk update && apk add --no-cache tini

### Application Image ###
# hadolint ignore=DL3006

FROM base AS app
# leave unset to use the default value at the top of the file
ARG BASE_IMAGE
ARG BASE_DIGEST
ARG VERSION=""
ARG DATE=""
ARG REVISION=""

# OCI labels: https://github.com/opencontainers/image-spec/blob/main/annotations.md
LABEL org.opencontainers.image.base.name="docker.io/library/${BASE_IMAGE}"
LABEL org.opencontainers.image.base.digest="${BASE_DIGEST}"
LABEL org.opencontainers.image.created="${DATE}"
LABEL org.opencontainers.image.authors="hto@camunda.com"
LABEL org.opencontainers.image.url="https://camunda.com/platform/tasklist/"
LABEL org.opencontainers.image.documentation="https://docs.camunda.io/docs/self-managed/tasklist-deployment/install-and-start/"
LABEL org.opencontainers.image.source="https://github.com/camunda/camunda"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.revision="${REVISION}"
LABEL org.opencontainers.image.vendor="Camunda Services GmbH"
LABEL org.opencontainers.image.licenses="(Apache-2.0 AND LicenseRef-Camunda-License-1.0)"
LABEL org.opencontainers.image.title="Camunda Tasklist"
LABEL org.opencontainers.image.description="Tasklist is a ready-to-use application to rapidly implement business processes alongside user tasks in Zeebe"

# OpenShift labels: https://docs.openshift.com/container-platform/4.10/openshift_images/create-images.html#defining-image-metadata
LABEL io.openshift.tags="bpmn,tasklist,camunda"
LABEL io.openshift.wants="zeebe,elasticsearch"
LABEL io.openshift.non-scalable="false"
LABEL io.openshift.min-memory="1Gi"
LABEL io.openshift.min-cpu="1"
LABEL io.k8s.description="Tasklist is a ready-to-use application to rapidly implement business processes alongside user tasks in Zeebe"

EXPOSE 8080

RUN apk update && apk upgrade && \
    apk add --no-cache bash openjdk21-jre tzdata gcompat libgcc libc6-compat

ENV TASKLIST_HOME=/usr/local/tasklist

WORKDIR ${TASKLIST_HOME}
VOLUME /tmp
VOLUME ${TASKLIST_HOME}/logs

RUN addgroup --gid 1001 camunda && \
    adduser -D -h ${TASKLIST_HOME} -G camunda -u 1001 camunda && \
    # These directories are to be mounted by users, eagerly creating them and setting ownership
    # helps to avoid potential permission issues due to default volume ownership.
    mkdir ${TASKLIST_HOME}/logs && \
    chown -R 1001:0 ${TASKLIST_HOME} && \
    chmod -R 0775 ${TASKLIST_HOME}

COPY --from=prepare --chown=1001:0 --chmod=0775 /tmp/tasklist ${TASKLIST_HOME}

# rename tasklist-migrate script to migrate (as expected by SaaS)
RUN mv ${TASKLIST_HOME}/bin/tasklist-migrate ${TASKLIST_HOME}/bin/migrate

USER 1001:1001

ENTRYPOINT ["/sbin/tini", "--", "/usr/local/tasklist/bin/tasklist"]
