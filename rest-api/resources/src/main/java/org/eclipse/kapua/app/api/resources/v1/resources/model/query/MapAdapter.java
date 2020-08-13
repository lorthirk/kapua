package org.eclipse.kapua.app.api.resources.v1.resources.model.query;

/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.eclipse.persistence.oxm.annotations.XmlVariableNode;

public class MapAdapter extends XmlAdapter<MapAdapter.AdaptedMap, Map<String, Integer>> {

    public static class AdaptedMap {

        @XmlVariableNode("key")
        List<AdaptedEntry> entries = new ArrayList<>();

    }

    public static class AdaptedEntry {

        @XmlTransient
        public String key;

        @XmlValue
        public Integer value;

    }

    @Override
    public AdaptedMap marshal(Map<String, Integer> map) throws Exception {
        AdaptedMap adaptedMap = new AdaptedMap();
        for(Entry<String, Integer> entry : map.entrySet()) {
            AdaptedEntry adaptedEntry = new AdaptedEntry();
            adaptedEntry.key = entry.getKey();
            adaptedEntry.value = entry.getValue();
            adaptedMap.entries.add(adaptedEntry);
        }
        return adaptedMap;
    }

    @Override
    public Map<String, Integer> unmarshal(AdaptedMap adaptedMap) throws Exception {
        List<AdaptedEntry> adaptedEntries = adaptedMap.entries;
        Map<String, Integer> map = new HashMap<>(adaptedEntries.size());
        for(AdaptedEntry adaptedEntry : adaptedEntries) {
            map.put(adaptedEntry.key, adaptedEntry.value);
        }
        return map;
    }

}
