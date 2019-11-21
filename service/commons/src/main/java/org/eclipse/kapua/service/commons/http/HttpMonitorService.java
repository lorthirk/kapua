/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.commons.http;

import org.eclipse.kapua.service.commons.Service;

import io.vertx.core.Vertx;

public interface HttpMonitorService extends Service {

    public static HttpMonitorServiceBuilder builder(Vertx aVertx) {
        return new HttpMonitorServiceImpl.Builder(aVertx);
    }

    public static HttpMonitorServiceBuilder builder(Vertx aVertx, HttpMonitorServiceConfig aConfig) {
        return new HttpMonitorServiceImpl.Builder(aVertx, aConfig);
    }
}
