#!/usr/bin/env bash
###############################################################################
# Copyright (c) 2016, 2019 Red Hat Inc and others
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Red Hat Inc - initial API and implementation
#     Eurotech
###############################################################################

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. ${SCRIPT_DIR}/docker-common.sh

#if [[ -z ${KAPUA_JWT_CERTIFICATE} ]] || [[ -z ${KAPUA_JWT_PRIVATE_KEY} ]]
#then
#    echo "Either KAPUA_JWT_CERTIFICATE or KAPUA_JWT_PRIVATE_KEY are not set. For more information please refer to the Kapua Documentation."
#    exit 1;
#fi

echo "Deploying Eclipse Kapua..."

docker-compose -f ${SCRIPT_DIR}/../compose/docker-compose.yml up -d

echo "Deploying Eclipse Kapua... DONE!"
echo "Run \"docker-compose -f ${SCRIPT_DIR}/../compose/docker-compose.yml logs -f\" for container logs"
