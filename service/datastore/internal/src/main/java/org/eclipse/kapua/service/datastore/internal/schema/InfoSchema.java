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
package org.eclipse.kapua.service.datastore.internal.schema;

import org.eclipse.kapua.commons.util.KapuaDateUtils;
import org.eclipse.kapua.service.datastore.client.DatamodelMappingException;
import org.eclipse.kapua.service.datastore.client.SchemaKeys;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class InfoSchema {

    private InfoSchema() { }

    /**
     * Channel information type name
     */
    public static final String CHANNEL_TYPE_NAME = "channel";

    /**
     * Client information type name
     */
    public static final String CLIENT_TYPE_NAME = "client";

    /**
     * Metric information type name
     */
    public static final String METRIC_TYPE_NAME = "metric";

    /**
     * Document information type name
     */
    public static final String INFO_TYPE_NAME = "info";

    /**
     * document type field name
     */
    private static final String TYPE = "type";

    /**
     * Channel information - channel
     * Metric information - channel
     */
    public static final String CHANNEL = "channel";

    /**
     * Channel information - client identifier
     * Client information - client identifier
     * Metric information - client identifier
     */
    public static final String CLIENT_ID = "client_id";

    /**
     * Channel information - scope id
     * Client information - scope id
     * Metric information - scope id
     */
    public static final String SCOPE_ID = "scope_id";

    /**
     * Channel information - message timestamp (of the first message published in this channel)
     * Client information - message timestamp (of the first message published in this channel)
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * Channel information - message identifier (of the first message published in this channel)
     * Client information - message identifier (of the first message published in this channel)
     */
    public static final String MESSAGE_ID = "message_id";

    /**
     * Metric information - metric map prefix
     */
    public static final String METRIC_MTR = "metric";

    /**
     * Metric information - name
     */
    public static final String METRIC_MTR_NAME = "name";

    /**
     * Metric information - full name (so with the metric type suffix)
     */
    public static final String METRIC_MTR_NAME_FULL = "metric.name";

    /**
     * Metric information - type
     */
    public static final String METRIC_MTR_TYPE = "type";

    /**
     * Metric information - full type (so with the metric type suffix)
     */
    public static final String METRIC_MTR_TYPE_FULL = "metric.type";

    /**
     * Metric information - value
     */
    private static final String METRIC_MTR_VALUE = "value";

    /**
     * Metric information - full value (so with the metric type suffix)
     */
    public static final String METRIC_MTR_VALUE_FULL = "metric.value";

    /**
     * Metric information - message timestamp (of the first message published in this channel)
     */
    public static final String METRIC_MTR_TIMESTAMP = "timestamp";

    /**
     * Metric information - message timestamp (of the first message published in this channel, with the metric type suffix)
     */
    public static final String METRIC_MTR_TIMESTAMP_FULL = "metric.timestamp";

    /**
     * Metric information - message identifier (of the first message published in this channel)
     */
    public static final String METRIC_MTR_MSG_ID = "message_id";

    /**
     * Metric information - full message identifier (of the first message published in this channel, with the metric type suffix)
     */
    public static final String METRIC_MTR_MSG_ID_FULL = "metric.message_id";

    /**
     * Create and return the Json representation of the info schema
     *
     * @param allEnable
     * @param sourceEnable
     * @return
     * @throws DatamodelMappingException
     */
    public static JsonNode getInfoTypeSchema(boolean allEnable, boolean sourceEnable) throws DatamodelMappingException {
        ObjectNode rootNode = SchemaUtil.getObjectNode();

        ObjectNode infoNode = SchemaUtil.getObjectNode();
        ObjectNode sourceInfo = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_ENABLED, sourceEnable) });
        infoNode.set(SchemaKeys.KEY_SOURCE, sourceInfo);

        ObjectNode allInfo = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_ENABLED, allEnable) });
        infoNode.set(SchemaKeys.KEY_ALL, allInfo);

        ObjectNode propertiesNode = SchemaUtil.getObjectNode();
        ObjectNode type = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        propertiesNode.set(TYPE, type);
        ObjectNode scopeId = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        propertiesNode.set(SCOPE_ID, scopeId);
        ObjectNode clientId = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        propertiesNode.set(CLIENT_ID, clientId);
        ObjectNode name = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        propertiesNode.set(CHANNEL, name);
        ObjectNode timestamp = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_DATE), new KeyValueEntry(SchemaKeys.KEY_FORMAT, KapuaDateUtils.ISO_DATE_PATTERN) });
        propertiesNode.set(TIMESTAMP, timestamp);
        ObjectNode messageId = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        propertiesNode.set(MESSAGE_ID, messageId);

        ObjectNode metricMtrNode = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_OBJECT), new KeyValueEntry(SchemaKeys.KEY_ENABLED, true),
                        new KeyValueEntry(SchemaKeys.KEY_DYNAMIC, false), new KeyValueEntry(SchemaKeys.KEY_INCLUDE_IN_ALL, false) });
        ObjectNode metricMtrPropertiesNode = SchemaUtil.getObjectNode();
        ObjectNode metricMtrNameNode = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        metricMtrPropertiesNode.set(METRIC_MTR_NAME, metricMtrNameNode);
        ObjectNode metricMtrTypeNode = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        metricMtrPropertiesNode.set(METRIC_MTR_TYPE, metricMtrTypeNode);
        ObjectNode metricMtrValueNode = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        metricMtrPropertiesNode.set(METRIC_MTR_VALUE, metricMtrValueNode);
        ObjectNode metricMtrTimestampNode = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_DATE), new KeyValueEntry(SchemaKeys.KEY_FORMAT, KapuaDateUtils.ISO_DATE_PATTERN) });
        metricMtrPropertiesNode.set(METRIC_MTR_TIMESTAMP, metricMtrTimestampNode);
        ObjectNode metricMtrMsgIdNode = SchemaUtil.getField(
                new KeyValueEntry[] { new KeyValueEntry(SchemaKeys.KEY_TYPE, SchemaKeys.TYPE_KEYWORD), new KeyValueEntry(SchemaKeys.KEY_INDEX, SchemaKeys.VALUE_TRUE) });
        metricMtrPropertiesNode.set(METRIC_MTR_MSG_ID, metricMtrMsgIdNode);
        metricMtrNode.set(SchemaKeys.FIELD_NAME_PROPERTIES, metricMtrPropertiesNode);
        propertiesNode.set(METRIC_MTR, metricMtrNode);

        infoNode.set(SchemaKeys.FIELD_NAME_PROPERTIES, propertiesNode);
        rootNode.set(INFO_TYPE_NAME, infoNode);
        return rootNode;
    }
}
