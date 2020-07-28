/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.console.module.api.shared.util;

import org.eclipse.kapua.app.console.module.api.shared.model.GwtEntityModel;
import org.eclipse.kapua.app.console.module.api.shared.model.GwtUpdatableEntityModel;
import org.eclipse.kapua.model.KapuaEntity;
import org.eclipse.kapua.model.KapuaUpdatableEntity;
import org.eclipse.kapua.model.id.KapuaId;

public class KapuaGwtCommonsModelConverter {

    private KapuaGwtCommonsModelConverter() {
    }

    /**
     * Converts a {@link KapuaId} into its {@link String} short id representation.
     * <p>
     * Example: 1 =&gt; AQ
     * </p>
     *
     * @param kapuaId The {@link KapuaId} to convertKapuaId
     * @return The short id representation of the {@link KapuaId}
     * @since 1.0.0
     */
    public static String convertKapuaId(KapuaId kapuaId) {
        if (kapuaId == null) {
            return null;
        }
        return kapuaId.toCompactId();
    }

    /**
     * Utility method to convertKapuaId commons properties of {@link KapuaUpdatableEntity} object to the GWT matching {@link GwtUpdatableEntityModel} object
     *
     * @param kapuaEntity The {@link KapuaUpdatableEntity} from which to copy values
     * @param gwtEntity   The {@link GwtUpdatableEntityModel} into which copy values
     * @since 1.0.0
     */
    public static void convertUpdatableEntity(KapuaUpdatableEntity kapuaEntity, GwtUpdatableEntityModel gwtEntity) {
        if (kapuaEntity == null || gwtEntity == null) {
            return;
        }
        convertEntity(kapuaEntity, gwtEntity);
        gwtEntity.setModifiedOn(kapuaEntity.getModifiedOn());
        gwtEntity.setModifiedBy(convertKapuaId(kapuaEntity.getModifiedBy()));
        gwtEntity.setModifiedByName(kapuaEntity.getModifiedByName());
        gwtEntity.setOptlock(kapuaEntity.getOptlock());
    }

    /**
     * Utility method to convertKapuaId commons properties of {@link KapuaEntity} object to the GWT matching {@link GwtEntityModel} object
     *
     * @param kapuaEntity The {@link KapuaEntity} from which to copy values
     * @param gwtEntity   The {@link GwtEntityModel} into which copy values
     * @since 1.0.0
     */
    public static void convertEntity(KapuaEntity kapuaEntity, GwtEntityModel gwtEntity) {
        if (kapuaEntity == null || gwtEntity == null) {
            return;
        }
        gwtEntity.setScopeId(convertKapuaId(kapuaEntity.getScopeId()));
        gwtEntity.setId(convertKapuaId(kapuaEntity.getId()));
        gwtEntity.setCreatedOn(kapuaEntity.getCreatedOn());
        gwtEntity.setCreatedBy(convertKapuaId(kapuaEntity.getCreatedBy()));
        gwtEntity.setCreatedByName(kapuaEntity.getCreatedByName());
    }

}
