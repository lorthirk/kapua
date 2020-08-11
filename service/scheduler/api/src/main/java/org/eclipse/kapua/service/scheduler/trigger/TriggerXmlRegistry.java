/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.scheduler.trigger;

import org.eclipse.kapua.locator.KapuaLocator;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * {@link Trigger} xml factory class
 *
 * @since 1.0
 */
@XmlRegistry
public class TriggerXmlRegistry {

    private static final KapuaLocator LOCATOR = KapuaLocator.getInstance();
    private static final TriggerFactory TRIGGER_FACTORY = LOCATOR.getFactory(TriggerFactory.class);

    /**
     * Creates a new schedule instance
     *
     * @return
     */
    public Trigger newTrigger() {
        return TRIGGER_FACTORY.newEntity(null);
    }

    /**
     * Creates a new schedule creator instance
     *
     * @return
     */
    public TriggerCreator newTriggerCreator() {
        return TRIGGER_FACTORY.newCreator(null);
    }

    /**
     * Creates a new schedule list result instance
     *
     * @return
     */
    public TriggerListResult newTriggerListResult() {
        return TRIGGER_FACTORY.newListResult();
    }

    public TriggerQuery newQuery() {
        return TRIGGER_FACTORY.newQuery(null);
    }
}
