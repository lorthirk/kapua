/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.datastore.internal.schema;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kapua.service.datastore.internal.mediator.Metric;

/**
 * Metadata object
 * 
 * @since 1.0
 *
 */
public class Metadata {

    // Info fields does not change within the same account name
    private String dataIndexName;
    private String channelRegistryIndexName;
    private String clientRegistryIndexName;
    private String metricRegistryIndexName;
    //

    // Custom mappings can only increase within the same account
    // No removal of existing cached mappings or changes in the
    // existing mappings.
    private Map<String, Metric> messageMappingsCache;
    //

    /**
     * Get the mappings cache
     * 
     * @return
     */
    public Map<String, Metric> getMessageMappingsCache() {
        return messageMappingsCache;
    }

    /**
     * Contruct metadata
     */
    public Metadata(String dataIndexName, String channelRegistryIndexName, String clientRegistryIndexName, String metricRegistryIndexName) {
        messageMappingsCache = new HashMap<>(100);
        this.dataIndexName = dataIndexName;
        this.channelRegistryIndexName = channelRegistryIndexName;
        this.clientRegistryIndexName = clientRegistryIndexName;
        this.metricRegistryIndexName = metricRegistryIndexName;
    }

    /**
     * Get the Elasticsearch data index name
     * 
     * @return
     */
    public String getDataIndexName() {
        return dataIndexName;
    }

    /**
     * Get the Kapua channel index name
     * 
     * @return
     */
    public String getChannelRegistryIndexName() {
        return channelRegistryIndexName;
    }

    public String getClientRegistryIndexName() {
        return clientRegistryIndexName;
    }

    public String getMetricRegistryIndexName() {
        return metricRegistryIndexName;
    }
}
