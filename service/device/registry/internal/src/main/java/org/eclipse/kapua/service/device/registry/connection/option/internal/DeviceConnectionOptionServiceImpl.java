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
package org.eclipse.kapua.service.device.registry.connection.option.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaRuntimeErrorCodes;
import org.eclipse.kapua.KapuaRuntimeException;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.service.internal.AbstractKapuaService;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.model.query.predicate.AndPredicate;
import org.eclipse.kapua.model.query.predicate.AttributePredicate.Operator;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.registry.DeviceDomains;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionAttributes;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionQuery;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionService;
import org.eclipse.kapua.service.device.registry.connection.internal.DeviceConnectionQueryImpl;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOption;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionCreator;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionListResult;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionService;
import org.eclipse.kapua.service.device.registry.connection.option.UserAlreadyReservedException;
import org.eclipse.kapua.service.device.registry.internal.DeviceEntityManagerFactory;
import org.eclipse.kapua.service.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeviceConnectionService exposes APIs to retrieve Device connections under a scope.
 * It includes APIs to find, list, and update devices connections associated with a scope.
 *
 * @since 1.0
 */
@KapuaProvider
public class DeviceConnectionOptionServiceImpl extends AbstractKapuaService implements DeviceConnectionOptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConnectionOptionServiceImpl.class);

    private final KapuaLocator locator = KapuaLocator.getInstance();
    private final UserService userService = locator.getService(UserService.class);
    private final AuthorizationService authorizationService = locator.getService(AuthorizationService.class);
    private final PermissionFactory permissionFactory = locator.getFactory(PermissionFactory.class);

    public DeviceConnectionOptionServiceImpl() {
        super(DeviceEntityManagerFactory.instance());
    }

    @Override
    public DeviceConnectionOption create(DeviceConnectionOptionCreator deviceConnectionCreator)
            throws KapuaException {
        throw new KapuaRuntimeException(KapuaRuntimeErrorCodes.SERVICE_OPERATION_NOT_SUPPORTED);
    }

    @Override
    public DeviceConnectionOption update(DeviceConnectionOption deviceConnectionOptions)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(deviceConnectionOptions, "deviceConnection");
        ArgumentValidator.notNull(deviceConnectionOptions.getId(), "deviceConnection.id");
        ArgumentValidator.notNull(deviceConnectionOptions.getScopeId(), "deviceConnection.scopeId");

        //
        // Check Access
        DeviceConnectionService deviceConnectionService = locator.getService(DeviceConnectionService.class);
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.write, deviceConnectionOptions.getScopeId()));

        if (deviceConnectionOptions.getReservedUserId() != null) {
            DeviceConnectionQuery query = new DeviceConnectionQueryImpl(deviceConnectionOptions.getScopeId());

            AndPredicate deviceAndPredicate = query.andPredicate(
                    query.attributePredicate(DeviceConnectionAttributes.RESERVED_USER_ID, deviceConnectionOptions.getReservedUserId()),
                    query.attributePredicate(DeviceConnectionAttributes.ENTITY_ID, deviceConnectionOptions.getId(), Operator.NOT_EQUAL)
            );

            query.setPredicate(deviceAndPredicate);
            if (deviceConnectionService.count(query) > 0) {
                throw new UserAlreadyReservedException(deviceConnectionOptions.getScopeId(), deviceConnectionOptions.getId(), deviceConnectionOptions.getReservedUserId());
            }
        }

        return entityManagerSession.doTransactedAction(em -> {
            if (DeviceConnectionOptionDAO.find(em, deviceConnectionOptions.getScopeId(), deviceConnectionOptions.getId()) == null) {
                throw new KapuaEntityNotFoundException(DeviceConnectionOption.TYPE, deviceConnectionOptions.getId());
            }

            return updateAuditFields(DeviceConnectionOptionDAO.update(em, deviceConnectionOptions));
        });
    }

    @Override
    public DeviceConnectionOption find(KapuaId scopeId, KapuaId entityId)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(entityId, "entityId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.read, scopeId));

        return entityManagerSession.doAction(em -> updateAuditFields(DeviceConnectionOptionDAO.find(em, scopeId, entityId)));
    }

    @Override
    public DeviceConnectionOptionListResult query(KapuaQuery<DeviceConnectionOption> query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.read, query.getScopeId()));

        return entityManagerSession.doAction(em -> {
            DeviceConnectionOptionListResult deviceConnectionOptionListResult = DeviceConnectionOptionDAO.query(em, query);
            deviceConnectionOptionListResult.getItems().forEach(this::updateAuditFields);
            return deviceConnectionOptionListResult;
        });
    }

    @Override
    public long count(KapuaQuery<DeviceConnectionOption> query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.read, query.getScopeId()));

        return entityManagerSession.doAction(em -> DeviceConnectionOptionDAO.count(em, query));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId deviceConnectionOptionsId)
            throws KapuaException {
        throw new KapuaRuntimeException(KapuaRuntimeErrorCodes.SERVICE_OPERATION_NOT_SUPPORTED);
    }

    private DeviceConnectionOption updateAuditFields(DeviceConnectionOption deviceConnectionOption) {
        try {
            if (deviceConnectionOption != null && authorizationService.isPermitted(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.info, deviceConnectionOption.getScopeId()))) {
                deviceConnectionOption.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(deviceConnectionOption.getCreatedBy())));
                deviceConnectionOption.setModifiedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(deviceConnectionOption.getModifiedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return deviceConnectionOption;
    }

}
