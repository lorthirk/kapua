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
package org.eclipse.kapua.service.device.registry.internal;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import org.eclipse.kapua.KapuaDuplicateNameException;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaMaxNumberOfItemsReachedException;
import org.eclipse.kapua.commons.configuration.AbstractKapuaConfigurableResourceLimitedService;
import org.eclipse.kapua.commons.jpa.EntityManagerContainer;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.registry.Device;
import org.eclipse.kapua.service.device.registry.DeviceAttributes;
import org.eclipse.kapua.service.device.registry.DeviceCreator;
import org.eclipse.kapua.service.device.registry.DeviceDomains;
import org.eclipse.kapua.service.device.registry.DeviceFactory;
import org.eclipse.kapua.service.device.registry.DeviceListResult;
import org.eclipse.kapua.service.device.registry.DeviceQuery;
import org.eclipse.kapua.service.device.registry.DeviceRegistryService;
import org.eclipse.kapua.service.device.registry.common.DeviceValidation;
import org.eclipse.kapua.service.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DeviceRegistryService} implementation.
 *
 * @since 1.0.0
 */
@KapuaProvider
public class DeviceRegistryServiceImpl extends AbstractKapuaConfigurableResourceLimitedService<Device, DeviceCreator, DeviceRegistryService, DeviceListResult, DeviceQuery, DeviceFactory>
        implements DeviceRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRegistryServiceImpl.class);

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PermissionFactory permissionFactory;

    @Inject
    private UserService userService;

    /**
     * Constructor
     *
     * @param deviceEntityManagerFactory
     */
    public DeviceRegistryServiceImpl(DeviceEntityManagerFactory deviceEntityManagerFactory) {
        super(DeviceRegistryService.class.getName(), DeviceDomains.DEVICE_DOMAIN, deviceEntityManagerFactory,
                DeviceRegistryCacheFactory.getInstance(), DeviceRegistryService.class, DeviceFactory.class);
    }

    /**
     * Constructor
     */
    public DeviceRegistryServiceImpl() {
        this(DeviceEntityManagerFactory.instance());
    }

    // Operations implementation
    @Override
    public Device create(DeviceCreator deviceCreator) throws KapuaException {
        DeviceValidation.validateCreatePreconditions(deviceCreator);

        //
        // Check limits
        if (allowedChildEntities(deviceCreator.getScopeId()) <= 0) {
            throw new KapuaMaxNumberOfItemsReachedException("Devices");
        }

        //
        // Check duplicate clientId
        DeviceQuery query = new DeviceQueryImpl(deviceCreator.getScopeId());
        query.setPredicate(query.attributePredicate(DeviceAttributes.CLIENT_ID, deviceCreator.getClientId()));

        if (count(query) > 0) {
            throw new KapuaDuplicateNameException(deviceCreator.getClientId());
        }

        return entityManagerSession.doTransactedAction(EntityManagerContainer.<Device>create().onResultHandler(entityManager -> updateAuditFields(DeviceDAO.create(entityManager, deviceCreator))));
    }

    @Override
    public Device update(Device device) throws KapuaException {
        DeviceValidation.validateUpdatePreconditions(device);

        return entityManagerSession.doTransactedAction(EntityManagerContainer.<Device>create()
                .onBeforeHandler(() -> {
                    entityCache.remove(device.getScopeId(), device);
                    return null;
                })
                .onResultHandler(entityManager -> {
                    Device currentDevice = DeviceDAO.find(entityManager, device.getScopeId(), device.getId());
                    if (currentDevice == null) {
                        throw new KapuaEntityNotFoundException(Device.TYPE, device.getId());
                    }
                    // Update
                    return updateAuditFields(DeviceDAO.update(entityManager, device));
                }));
    }

    @Override
    public Device find(KapuaId scopeId, KapuaId entityId) throws KapuaException {
        DeviceValidation.validateFindPreconditions(scopeId, entityId);

        return entityManagerSession.doAction(EntityManagerContainer.<Device>create()
                .onBeforeHandler(() -> (Device) entityCache.get(scopeId, entityId))
                .onResultHandler(entityManager -> updateAuditFields(DeviceDAO.find(entityManager, scopeId, entityId)))
                .onAfterHandler(entity -> entityCache.put(entity)));
    }

    @Override
    public DeviceListResult query(KapuaQuery<Device> query) throws KapuaException {
        DeviceValidation.validateQueryPreconditions(query);

        return entityManagerSession.doAction(EntityManagerContainer.<DeviceListResult>create().onResultHandler(entityManager -> {
            DeviceListResult deviceListResult = DeviceDAO.query(entityManager, query);
            deviceListResult.getItems().forEach(this::updateAuditFields);
            return deviceListResult;
        }));
    }

    @Override
    public long count(KapuaQuery<Device> query) throws KapuaException {
        DeviceValidation.validateCountPreconditions(query);

        return entityManagerSession.doAction(EntityManagerContainer.<Long>create().onResultHandler(entityManager -> DeviceDAO.count(entityManager, query)));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId deviceId) throws KapuaException {
        DeviceValidation.validateDeletePreconditions(scopeId, deviceId);

        entityManagerSession.doTransactedAction(EntityManagerContainer.create().onResultHandler(entityManager -> DeviceDAO.delete(entityManager, scopeId, deviceId))
                .onAfterHandler(emptyParam -> entityCache.remove(scopeId, deviceId)));
    }

    @Override
    public Device findByClientId(KapuaId scopeId, String clientId) throws KapuaException {
        DeviceValidation.validateFindByClientIdPreconditions(scopeId, clientId);
        Device device = (Device) ((DeviceRegistryCache) entityCache).getByClientId(scopeId, clientId);
        if (device == null) {
            DeviceQueryImpl query = new DeviceQueryImpl(scopeId);
            query.setPredicate(query.attributePredicate(DeviceAttributes.CLIENT_ID, clientId));
            query.setFetchAttributes(Lists.newArrayList(DeviceAttributes.CONNECTION, DeviceAttributes.LAST_EVENT));

            //
            // Query and parse result
            DeviceListResult result = query(query);
            if (!result.isEmpty()) {
                device = updateAuditFields(result.getFirstItem());
                entityCache.put(device);
            }
        }
        return device;
    }

    //@ListenServiceEvent(fromAddress="account")
    //@ListenServiceEvent(fromAddress="authorization")
    public void onKapuaEvent(ServiceEvent kapuaEvent) throws KapuaException {
        if (kapuaEvent == null) {
            //service bus error. Throw some exception?
        }
        LOGGER.info("DeviceRegistryService: received kapua event from {}, operation {}", kapuaEvent.getService(), kapuaEvent.getOperation());
        if ("group".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteDeviceByGroupId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        } else if ("account".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteDeviceByAccountId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        }
    }

    private void deleteDeviceByGroupId(KapuaId scopeId, KapuaId groupId) throws KapuaException {
        KapuaLocator locator = KapuaLocator.getInstance();
        DeviceFactory deviceFactory = locator.getFactory(DeviceFactory.class);

        DeviceQuery query = deviceFactory.newQuery(scopeId);
        query.setPredicate(query.attributePredicate(DeviceAttributes.GROUP_ID, groupId));

        DeviceListResult devicesToDelete = query(query);

        for (Device d : devicesToDelete.getItems()) {
            d.setGroupId(null);
            update(d);
        }
    }

    private void deleteDeviceByAccountId(KapuaId scopeId, KapuaId accountId) throws KapuaException {
        KapuaLocator locator = KapuaLocator.getInstance();
        DeviceFactory deviceFactory = locator.getFactory(DeviceFactory.class);

        DeviceQuery query = deviceFactory.newQuery(accountId);

        DeviceListResult devicesToDelete = query(query);

        for (Device d : devicesToDelete.getItems()) {
            delete(d.getScopeId(), d.getId());
        }
    }

    private Device updateAuditFields(Device device) {
        try {
            if (device != null && authorizationService.isPermitted(permissionFactory.newPermission(DeviceDomains.DEVICE_DOMAIN, Actions.info, device.getScopeId(), device.getGroupId()))) {
                device.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(device.getCreatedBy())));
                device.setModifiedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(device.getModifiedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return device;
    }

}
