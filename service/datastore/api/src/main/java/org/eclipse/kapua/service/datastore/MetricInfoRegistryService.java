/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.datastore;

import org.eclipse.kapua.service.KapuaService;
import org.eclipse.kapua.service.datastore.model.MetricInfo;
import org.eclipse.kapua.service.datastore.model.MetricInfoListResult;
import org.eclipse.kapua.service.datastore.model.query.MetricInfoQuery;
import org.eclipse.kapua.service.storable.StorableService;

/**
 * {@link MetricInfoRegistryService} definition.
 * <p>
 * {@link StorableService} for {@link MetricInfo}
 *
 * @since 1.0.0
 */
public interface MetricInfoRegistryService extends KapuaService, StorableService<MetricInfo, MetricInfoListResult, MetricInfoQuery> {
}
