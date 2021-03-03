/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.broker.client.protocol;

import org.eclipse.kapua.broker.client.setting.BrokerClientSetting;
import org.eclipse.kapua.qa.markers.junit.JUnitTests;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(JUnitTests.class)
public class DefaultProtocolDescriptorProviderTest {

    String[] protocolName;

    @Before
    public void initialize() {
        protocolName = new String[]{null, "", "protocol name", "name1234567890", "protocol!@#$%^&*()_<>/"};
    }

    @Test
    public void getDescriptorTest() {
        System.setProperty("protocol_descriptor.default.disable", "false");
        System.setProperty("protocol_descriptor.configuration.uri", "file:src/test/resources/protocol.descriptor/1.properties");
        BrokerClientSetting.resetInstance();
        DefaultProtocolDescriptionProvider defaultProtocolDescriptionProvider = new DefaultProtocolDescriptionProvider();
        for (String name : protocolName) {
            System.out.println( defaultProtocolDescriptionProvider.getDescriptor(name));
            Assert.assertThat("Instance of ProtocolDescriptor expected.", defaultProtocolDescriptionProvider.getDescriptor(name), IsInstanceOf.instanceOf(ProtocolDescriptor.class));
        }
    }

    @Test
    public void getDescriptorDisabledDefaultProtocolDescriptorTest() {
        System.setProperty("protocol_descriptor.default.disable", "true");
        System.setProperty("protocol_descriptor.configuration.uri", "");
        BrokerClientSetting.resetInstance();
        DefaultProtocolDescriptionProvider defaultProtocolDescriptionProvider = new DefaultProtocolDescriptionProvider();
        for (String name : protocolName) {
            Assert.assertNull("Null expected.", defaultProtocolDescriptionProvider.getDescriptor(name));
        }
    }

    @Test
    public void getDescriptorConfigurationFirstPropertiesTest() {
        System.setProperty("protocol_descriptor.default.disable", "true");
        System.setProperty("protocol_descriptor.configuration.uri", "file:src/test/resources/protocol.descriptor/1.properties");
        BrokerClientSetting.resetInstance();
        DefaultProtocolDescriptionProvider defaultProtocolDescriptionProvider = new DefaultProtocolDescriptionProvider();
        for (String name : protocolName) {
            Assert.assertNull("Null expected.", defaultProtocolDescriptionProvider.getDescriptor(name));
        }
    }

    @Test
    public void getDescriptorConfigurationSecondPropertiesTest() {
        System.setProperty("protocol_descriptor.configuration.uri", "file:src/test/resources/protocol.descriptor/2.properties");
        BrokerClientSetting.resetInstance();
        DefaultProtocolDescriptionProvider defaultProtocolDescriptionProvider = new DefaultProtocolDescriptionProvider();
        for (String name : protocolName) {
            Assert.assertThat("Instance of ProtocolDescriptor expected.", defaultProtocolDescriptionProvider.getDescriptor(name), IsInstanceOf.instanceOf(ProtocolDescriptor.class));
        }
    }

    @Test(expected = Exception.class)
    public void getDescriptorNoClassExceptionTest() {
        System.setProperty("protocol_descriptor.default.disable", "true");
        System.setProperty("protocol_descriptor.configuration.uri", "file:src/test/resources/protocol.descriptor/3.properties");
        BrokerClientSetting.resetInstance();
        DefaultProtocolDescriptionProvider defaultProtocolDescriptionProvider = new DefaultProtocolDescriptionProvider();
        for (String name : protocolName) {
            defaultProtocolDescriptionProvider.getDescriptor(name);
        }
    }

    @Test(expected = Exception.class)
    public void getDescriptorNoProtocolExceptionTest() {
        System.setProperty("protocol_descriptor.configuration.uri", "aaa");
        BrokerClientSetting.resetInstance();
        new DefaultProtocolDescriptionProvider();
    }
}