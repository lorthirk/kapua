#!/usr/bin/env bash
###############################################################################
# Copyright (c) 2019 Eurotech and/or its affiliates and others
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Eurotech - initial API and implementation
#
###############################################################################
set -eo pipefail

# Database configurations

JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.connection.host=${DB_HOST:-db}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.connection.port=${DB_PORT:-3306}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.name=${DB_NAME:-kapuadb}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.schema=${DB_SCHEMA_NAME:-kapuadb}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.schema.update=${DB_SCHEMA_UPDATE:-true}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.username=${DB_USERNAME}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.password=${DB_PASSWORD}"
# Advanced options
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.connection.scheme=${DB_CONNECTION_SCHEME:-jdbc:h2:tcp}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.jdbcConnectionUrlResolver=${DB_RESOLVER:-H2}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.db.jdbc.driver=${DB_DRIVER:-org.h2.Driver}"

# Datastore configurations

JAVA_OPTS="${JAVA_OPTS} -Ddatastore.elasticsearch.nodes=${STORAGE_HOST:-es}"
JAVA_OPTS="${JAVA_OPTS} -Ddatastore.elasticsearch.port=${STORAGE_PORT:-9200}"
JAVA_OPTS="${JAVA_OPTS} -Ddatastore.elasticsearch.ssl.enabled=${STORAGE_SSL:-false}"
JAVA_OPTS="${JAVA_OPTS} -Ddatastore.index.prefix=${STORAGE_PREFIX:-''}"
JAVA_OPTS="${JAVA_OPTS} -Ddatastore.client.class=${STORAGE_CLIENT:-org.eclipse.kapua.service.datastore.client.rest.RestDatastoreClient}"

# Events broker configurations

JAVA_OPTS="${JAVA_OPTS} -Dcommons.eventbus.url=amqp://${EVENTS_BROKER_HOST:-kapua-events-broker}:${EVENTS_BROKER_PORT:-5672}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.eventbus.username=${EVENTS_BROKER_USERNAME}"
JAVA_OPTS="${JAVA_OPTS} -Dcommons.eventbus.password=${EVENTS_BROKER_PASSWORD}"

# Transport configurations

JAVA_OPTS="${JAVA_OPTS} -Dtransport.credential.username=${TRANSPORT_USERNAME}"
JAVA_OPTS="${JAVA_OPTS} -Dtransport.credential.password=${TRANSPORT_PASSWORD}"

# JWT Certificate configurations
JAVA_OPTS="${JAVA_OPTS} -Dcertificate.jwt.certificate=${JWT_CERTIFICATE:-file:///var/opt/jetty/cert.pem}"
JAVA_OPTS="${JAVA_OPTS} -Dcertificate.jwt.private.key=${JWT_PRIVATE_KEY:-file:///var/opt/jetty/key.pk8}"

# Other configurations

JAVA_OPTS="${JAVA_OPTS} -Dkapua.config.dir=/etc/opt/ec/defaults"

# Run the JVM

export JAVA_OPTS

exec /entrypoint.sh "$@"
