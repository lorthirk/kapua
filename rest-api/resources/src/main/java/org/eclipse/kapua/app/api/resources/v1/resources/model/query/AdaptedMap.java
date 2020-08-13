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
package org.eclipse.kapua.app.api.resources.v1.resources.model.query;

import javax.xml.bind.annotation.XmlAnyElement;

public class AdaptedMap {

    private Object value;

    @XmlAnyElement
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
