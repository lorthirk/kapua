/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.device.management.registry.operation.notification.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.service.internal.AbstractKapuaService;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperation;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationRegistryService;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementRegistryDomains;
import org.eclipse.kapua.service.device.management.registry.operation.internal.DeviceManagementOperationEntityManagerFactory;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotification;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationCreator;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationListResult;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationService;
import org.eclipse.kapua.service.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KapuaProvider
public class ManagementOperationNotificationServiceImpl extends AbstractKapuaService implements ManagementOperationNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementOperationNotificationServiceImpl.class);

    private static final KapuaLocator LOCATOR = KapuaLocator.getInstance();

    private static final AuthorizationService AUTHORIZATION_SERVICE = LOCATOR.getService(AuthorizationService.class);
    private static final PermissionFactory PERMISSION_FACTORY = LOCATOR.getFactory(PermissionFactory.class);

    private static final DeviceManagementOperationRegistryService DEVICE_MANAGEMENT_OPERATION_REGISTRY_SERVICE = LOCATOR.getService(DeviceManagementOperationRegistryService.class);

    private static final UserService USER_SERVICE = LOCATOR.getService(UserService.class);

    protected ManagementOperationNotificationServiceImpl() {
        super(DeviceManagementOperationEntityManagerFactory.getInstance());
    }

    @Override
    public ManagementOperationNotification create(ManagementOperationNotificationCreator creator) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(creator, "managementOperationNotificationCreator");
        ArgumentValidator.notNull(creator.getScopeId(), "managementOperationNotificationCreator.scopeId");
        ArgumentValidator.notNull(creator.getOperationId(), "managementOperationNotificationCreator.operationId");
        ArgumentValidator.notNull(creator.getSentOn(), "managementOperationNotificationCreator.sentOn");
        ArgumentValidator.notNull(creator.getStatus(), "managementOperationNotificationCreator.status");
        ArgumentValidator.notNull(creator.getProgress(), "managementOperationNotificationCreator.progress");
        ArgumentValidator.notNegative(creator.getProgress(), "managementOperationNotificationCreator.progress");

        //
        // Check access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(DeviceManagementRegistryDomains.DEVICE_MANAGEMENT_REGISTRY_DOMAIN, Actions.write, null));

        //
        // Check operation existence
        if (KapuaSecurityUtils.doPrivileged(() -> DEVICE_MANAGEMENT_OPERATION_REGISTRY_SERVICE.find(creator.getScopeId(), creator.getOperationId()) == null)) {
            throw new KapuaEntityNotFoundException(DeviceManagementOperation.TYPE, creator.getOperationId());
        }

        //
        // Do create
        return entityManagerSession.doTransactedAction(em -> updateAuditFields(ManagementOperationNotificationDAO.create(em, creator)));
    }

    @Override
    public ManagementOperationNotification find(KapuaId scopeId, KapuaId entityId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(entityId, "managementOperationNotificationId");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(DeviceManagementRegistryDomains.DEVICE_MANAGEMENT_REGISTRY_DOMAIN, Actions.read, scopeId));

        //
        // Do find
        return entityManagerSession.doAction(em -> updateAuditFields(ManagementOperationNotificationDAO.find(em, scopeId, entityId)));
    }

    @Override
    public ManagementOperationNotificationListResult query(KapuaQuery<ManagementOperationNotification> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(DeviceManagementRegistryDomains.DEVICE_MANAGEMENT_REGISTRY_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> {
            ManagementOperationNotificationListResult managementOperationNotificationListResult = ManagementOperationNotificationDAO.query(em, query);
            managementOperationNotificationListResult.getItems().forEach(this::updateAuditFields);
            return managementOperationNotificationListResult;
        });
    }

    @Override
    public long count(KapuaQuery<ManagementOperationNotification> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(DeviceManagementRegistryDomains.DEVICE_MANAGEMENT_REGISTRY_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do count
        return entityManagerSession.doAction(em -> ManagementOperationNotificationDAO.count(em, query));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId entityId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(entityId, "managementOperationNotificationId");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(DeviceManagementRegistryDomains.DEVICE_MANAGEMENT_REGISTRY_DOMAIN, Actions.delete, null));

        //
        // Do delete
        entityManagerSession.doTransactedAction(em -> ManagementOperationNotificationDAO.delete(em, scopeId, entityId));
    }

    private ManagementOperationNotification updateAuditFields(ManagementOperationNotification managementOperationNotification) {
        try {
            if (managementOperationNotification != null && AUTHORIZATION_SERVICE.isPermitted(PERMISSION_FACTORY.newPermission(DeviceManagementRegistryDomains.DEVICE_MANAGEMENT_REGISTRY_DOMAIN, Actions.info, managementOperationNotification.getScopeId()))) {
                managementOperationNotification.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> USER_SERVICE.getName(managementOperationNotification.getCreatedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return managementOperationNotification;
    }

}
