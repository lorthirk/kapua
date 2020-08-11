/*******************************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.sso.provider.generic;

import org.eclipse.kapua.sso.provider.SingleSignOnProvider;

public class ProviderImpl implements SingleSignOnProvider {

    @Override
    public String getId() {
        return "generic";
    }

    @Override
    public ProviderLocator createLocator() {
        return new GenericSingleSignOnLocator();
    }

}
