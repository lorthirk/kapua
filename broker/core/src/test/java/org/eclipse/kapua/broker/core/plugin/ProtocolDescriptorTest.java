/*******************************************************************************
 * Copyright (c) 2017, 2021 Red Hat Inc and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kapua.broker.core.plugin;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.kapua.KapuaErrorCodes;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.broker.client.message.MessageType;
import org.eclipse.kapua.broker.client.protocol.DefaultProtocolDescriptionProvider;
import org.eclipse.kapua.broker.client.protocol.ProtocolDescriptor;
import org.eclipse.kapua.broker.client.protocol.ProtocolDescriptorProvider;
import org.eclipse.kapua.broker.client.protocol.ProtocolDescriptorProviders;
import org.eclipse.kapua.broker.client.setting.BrokerClientSettingKey;
import org.eclipse.kapua.broker.core.JAXBContextLoader;
import org.eclipse.kapua.broker.core.setting.BrokerSetting;
import org.eclipse.kapua.broker.core.setting.BrokerSettingKey;
import org.eclipse.kapua.consumer.commons.setting.ConsumerSetting;
import org.eclipse.kapua.qa.markers.junit.JUnitTests;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

@Category(JUnitTests.class)
public class ProtocolDescriptorTest {

    private static final String BROKER_IP_RESOLVER_CLASS_NAME;

    private JAXBContextLoader jaxbContextLoader;

    static {
        BrokerSetting config = BrokerSetting.getInstance();
        BROKER_IP_RESOLVER_CLASS_NAME = config.getString(BrokerSettingKey.BROKER_IP_RESOLVER_CLASS_NAME);
    }

    @Before
    public void resetSettings() throws KapuaException {
        jaxbContextLoader = new JAXBContextLoader();
        jaxbContextLoader.init();
        BrokerSetting.resetInstance();
        ConsumerSetting.resetInstance();
    }

    @After
    public void resetJAXBContext() {
        jaxbContextLoader.reset();
    }

    @Test
    public void nonNullProviderTest() {
        ProtocolDescriptorProvider provider = ProtocolDescriptorProviders.getInstance();
        assertNotNull("Null not expected.", provider);
    }

    @Test
    public void defaultDescriptorFromProviderTest() {
        ProtocolDescriptorProvider provider = ProtocolDescriptorProviders.getInstance();
        ProtocolDescriptor descriptor = provider.getDescriptor("foo");
        assertNotNull("Null not expected.", descriptor);
    }

    @Test
    public void getDescriptorFromProvidersClassTest() {
        assertNotNull("Null not expected.", ConnectorDescriptorProviders.getDescriptor("foo"));
    }

    @Test
    public void getTransportProtocolTest() {
        ProtocolDescriptorProviders provider = ProtocolDescriptorProviders.getInstance();
        ConnectorDescriptor descriptor = provider.getDescriptor("foo");
        assertEquals("Expected and actual values should be the same.", "MQTT", descriptor.getTransportProtocol());
    }

    @Test
    public void defaultProviderWithDisabledDefaultDescriptorTest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.DISABLE_DEFAULT_PROTOCOL_DESCRIPTOR.key(), "true");

        Tests.runWithProperties(properties, () -> {
            DefaultProtocolDescriptionProvider provider = new DefaultProtocolDescriptionProvider();
            ProtocolDescriptor descriptor = provider.getDescriptor("foo");
            assertNull("Null expected.", descriptor);
        });
    }

    @Test(expected = Exception.class)
    public void defaultProviderWithNonExistingFileTest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.CONFIGURATION_URI.key(), "file:src/test/resources/does-not-exist.properties");

        Tests.runWithProperties(properties, DefaultProtocolDescriptionProvider::new);
    }

    @Test
    public void defaultProviderAllowingDefaultFallbackTest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.CONFIGURATION_URI.key(), "file:src/test/resources/conector.descriptor/1.properties");

        Tests.runWithProperties(properties, () -> {
            DefaultProtocolDescriptionProvider provider = new DefaultProtocolDescriptionProvider();
            ProtocolDescriptor descriptor = provider.getDescriptor("foo");
            assertNotNull("Null not expected.", descriptor);
        });
    }

    @Test
    public void defaultProviderWithEmptyFileTest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.DISABLE_DEFAULT_PROTOCOL_DESCRIPTOR.key(), "true");
        properties.put(BrokerClientSettingKey.CONFIGURATION_URI.key(), "file:src/test/resources/conector.descriptor/1.properties");

        Tests.runWithProperties(properties, () -> {
            DefaultProtocolDescriptionProvider provider = new DefaultProtocolDescriptionProvider();
            ProtocolDescriptor descriptor = provider.getDescriptor("foo");
            assertNull("Null expected.", descriptor);
        });
    }

    @Test
    public void defaultProviderUsingNonEmptyConfigurationTest() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.DISABLE_DEFAULT_PROTOCOL_DESCRIPTOR.key(), "true");
        properties.put(BrokerClientSettingKey.CONFIGURATION_URI.key(), "file:src/test/resources/conector.descriptor/2.properties");

        Tests.runWithProperties(properties, () -> {
            DefaultProtocolDescriptionProvider provider = new DefaultProtocolDescriptionProvider();
            assertNull("Null expected.", provider.getDescriptor("foo"));

            ProtocolDescriptor descriptor = provider.getDescriptor("mqtt");
            assertNotNull("Null not expected.", descriptor);

            assertEquals("Expected and actual values should be the same.", org.eclipse.kapua.service.device.call.message.kura.lifecycle.KuraAppsMessage.class, descriptor.getDeviceClass(MessageType.APP));
            assertEquals("Expected and actual values should be the same.", org.eclipse.kapua.message.device.lifecycle.KapuaAppsMessage.class, descriptor.getKapuaClass(MessageType.APP));

            assertNull("Null expected.", descriptor.getDeviceClass(MessageType.DATA));
            assertNull("Null expected.", descriptor.getKapuaClass(MessageType.DATA));
        });
    }

    @Test(expected = Exception.class)
    public void defaultProviderWithInvalidConfigurationTest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.DISABLE_DEFAULT_PROTOCOL_DESCRIPTOR.key(), "true");
        properties.put(BrokerClientSettingKey.CONFIGURATION_URI.key(), "file:src/test/resources/conector.descriptor/3.properties");

        Tests.runWithProperties(properties, DefaultProtocolDescriptionProvider::new);
    }

    @Test
    public void emptyConfigurationUrlTest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(BrokerClientSettingKey.CONFIGURATION_URI.key(), "");

        Tests.runWithProperties(properties, DefaultProtocolDescriptionProvider::new);
    }

    @Test
    public void testBrokerIpOrHostNameConfigFile() throws Exception {
        System.clearProperty("broker.ip");
        System.setProperty("kapua.config.url", "broker.setting/kapua-broker-setting-1.properties");

        BrokerIpResolver brokerIpResolver = newInstance(BROKER_IP_RESOLVER_CLASS_NAME, DefaultBrokerIpResolver.class);
        String ipOrHostName = brokerIpResolver.getBrokerIpOrHostName();
        assertEquals("Expected and actual values should be the same.", "192.168.33.10", ipOrHostName);
    }

    @Test
    public void testBrokerIpOrHostNameEnvProperty() throws Exception {
        System.clearProperty("kapua.config.url");
        System.setProperty("broker.ip", "192.168.33.10");

        BrokerIpResolver brokerIpResolver = newInstance(BROKER_IP_RESOLVER_CLASS_NAME, DefaultBrokerIpResolver.class);
        String ipOrHostName = brokerIpResolver.getBrokerIpOrHostName();
        assertEquals("Expected and actual values should be the same.", "192.168.33.10", ipOrHostName);
    }

    @Test
    public void testBrokerIpOrHostNameEmptyConfigFile() throws Exception {
        System.clearProperty("broker.ip");
        System.setProperty("kapua.config.url", "broker.setting/kapua-broker-setting-2.properties");

        BrokerIpResolver brokerIpResolver = newInstance(BROKER_IP_RESOLVER_CLASS_NAME, DefaultBrokerIpResolver.class);
        String ipOrHostName = brokerIpResolver.getBrokerIpOrHostName();
        assertEquals("Expected and actual values should be the same.", "192.168.33.10", ipOrHostName);
    }

    @Test(expected = Exception.class)
    public void testBrokerIpOrHostNameNoEnvProperty() throws Exception {
        System.clearProperty("broker.ip");
        System.setProperty("kapua.config.url", "broker.setting/kapua-broker-setting-3.properties");

        BrokerIpResolver brokerIpResolver = newInstance(BROKER_IP_RESOLVER_CLASS_NAME, DefaultBrokerIpResolver.class);
        brokerIpResolver.getBrokerIpOrHostName();
    }

    /**
     * Code reused form KapuaSecurityBrokerFilter for instantiating broker ip resolver class.
     *
     * @param clazz           class that instantiates broker ip resolver
     * @param defaultInstance default instance of class
     * @param <T>             generic type
     * @return instance of ip resolver
     * @throws KapuaException
     */
    protected <T> T newInstance(String clazz, Class<T> defaultInstance) throws KapuaException {
        T instance;
        // lazy synchronization
        try {
            if (!StringUtils.isEmpty(clazz)) {
                Class<T> clazzToInstantiate = (Class<T>) Class.forName(clazz);
                instance = clazzToInstantiate.newInstance();
            } else {
                instance = defaultInstance.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new KapuaException(KapuaErrorCodes.INTERNAL_ERROR, e, "Class instantiation exception " + clazz);
        }

        return instance;
    }

}
