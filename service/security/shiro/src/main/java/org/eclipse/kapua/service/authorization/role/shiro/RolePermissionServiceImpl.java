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
package org.eclipse.kapua.service.authorization.role.shiro;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaEntityUniquenessException;
import org.eclipse.kapua.KapuaErrorCodes;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.jpa.EntityManagerContainer;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.service.internal.AbstractKapuaService;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.KapuaEntityAttributes;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationDomains;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.Permission;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.authorization.permission.shiro.PermissionValidator;
import org.eclipse.kapua.service.authorization.role.Role;
import org.eclipse.kapua.service.authorization.role.RolePermission;
import org.eclipse.kapua.service.authorization.role.RolePermissionAttributes;
import org.eclipse.kapua.service.authorization.role.RolePermissionCreator;
import org.eclipse.kapua.service.authorization.role.RolePermissionListResult;
import org.eclipse.kapua.service.authorization.role.RolePermissionQuery;
import org.eclipse.kapua.service.authorization.role.RolePermissionService;
import org.eclipse.kapua.service.authorization.role.RoleService;
import org.eclipse.kapua.service.authorization.shiro.AuthorizationEntityManagerFactory;
import org.eclipse.kapua.service.user.UserService;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RolePermission} service implementation.
 *
 * @since 1.0
 */
@KapuaProvider
public class RolePermissionServiceImpl extends AbstractKapuaService implements RolePermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionServiceImpl.class);

    private final KapuaLocator locator = KapuaLocator.getInstance();
    private final RoleService roleService = KapuaLocator.getInstance().getService(RoleService.class);
    private final AuthorizationService authorizationService = locator.getService(AuthorizationService.class);
    private final PermissionFactory permissionFactory = locator.getFactory(PermissionFactory.class);
    private final UserService userService = locator.getService(UserService.class);

    public RolePermissionServiceImpl() {
        super(AuthorizationEntityManagerFactory.getInstance(), RolePermissionCacheFactory.getInstance());
    }

    @Override
    public RolePermission create(RolePermissionCreator rolePermissionCreator)
            throws KapuaException {
        ArgumentValidator.notNull(rolePermissionCreator, "rolePermissionCreator");
        ArgumentValidator.notNull(rolePermissionCreator.getRoleId(), "rolePermissionCreator.roleId");
        ArgumentValidator.notNull(rolePermissionCreator.getPermission(), "rolePermissionCreator.permission");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.write, rolePermissionCreator.getScopeId()));

        //
        // Check role existence
        if (roleService.find(rolePermissionCreator.getScopeId(), rolePermissionCreator.getRoleId()) == null) {
            throw new KapuaEntityNotFoundException(Role.TYPE, rolePermissionCreator.getRoleId());
        }

        //
        // Check that the given permission matches the definition of the Domains.
        PermissionValidator.validatePermission(rolePermissionCreator.getPermission());

        //
        // If permission are created out of the role permission scope, check that the current user has the permission on the external scopeId.
        Permission permission = rolePermissionCreator.getPermission();
        if (permission.getTargetScopeId() == null || !permission.getTargetScopeId().equals(rolePermissionCreator.getScopeId())) {
            authorizationService.checkPermission(permission);
        }

        //
        // Check duplicates
        RolePermissionQuery query = new RolePermissionQueryImpl(rolePermissionCreator.getScopeId());
        query.setPredicate(
                query.andPredicate(
                        query.attributePredicate(KapuaEntityAttributes.SCOPE_ID, rolePermissionCreator.getScopeId()),
                        query.attributePredicate(RolePermissionAttributes.ROLE_ID, rolePermissionCreator.getRoleId()),
                        query.attributePredicate(RolePermissionAttributes.PERMISSION_DOMAIN, rolePermissionCreator.getPermission().getDomain()),
                        query.attributePredicate(RolePermissionAttributes.PERMISSION_ACTION, rolePermissionCreator.getPermission().getAction()),
                        query.attributePredicate(RolePermissionAttributes.PERMISSION_TARGET_SCOPE_ID, rolePermissionCreator.getPermission().getTargetScopeId()),
                        query.attributePredicate(RolePermissionAttributes.PERMISSION_GROUP_ID, rolePermissionCreator.getPermission().getGroupId()),
                        query.attributePredicate(RolePermissionAttributes.PERMISSION_FORWARDABLE, rolePermissionCreator.getPermission().getForwardable())
                )
        );
        if (count(query) > 0) {
            List<Map.Entry<String, Object>> uniquesFieldValues = new ArrayList<>();

            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(KapuaEntityAttributes.SCOPE_ID, rolePermissionCreator.getScopeId()));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(RolePermissionAttributes.ROLE_ID, rolePermissionCreator.getRoleId()));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(RolePermissionAttributes.PERMISSION_DOMAIN, rolePermissionCreator.getPermission().getDomain()));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(RolePermissionAttributes.PERMISSION_ACTION, rolePermissionCreator.getPermission().getAction()));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(RolePermissionAttributes.PERMISSION_TARGET_SCOPE_ID, rolePermissionCreator.getPermission().getTargetScopeId()));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(RolePermissionAttributes.PERMISSION_GROUP_ID, rolePermissionCreator.getPermission().getGroupId()));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(RolePermissionAttributes.PERMISSION_FORWARDABLE, rolePermissionCreator.getPermission().getForwardable()));

            throw new KapuaEntityUniquenessException(RolePermission.TYPE, uniquesFieldValues);
        }

        return entityManagerSession.doTransactedAction(EntityManagerContainer.<RolePermission>create()
                .onResultHandler(em -> updateAuditFields(RolePermissionDAO.create(em, rolePermissionCreator)))
                .onAfterHandler(entity -> entityCache.removeList(entity.getScopeId(), entity.getRoleId())));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId rolePermissionId) throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(rolePermissionId, "rolePermissionId");

        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.delete, scopeId));

        entityManagerSession.doTransactedAction(EntityManagerContainer.<RolePermission>create().onResultHandler(em -> {
            // TODO: check if it is correct to remove this statement (already thrown by the delete method, but
            //  without TYPE)
            RolePermission rolePermission = RolePermissionDAO.find(em, scopeId, rolePermissionId);
            if (rolePermission == null) {
                throw new KapuaEntityNotFoundException(RolePermission.TYPE, rolePermissionId);
            } else if (KapuaId.ONE.equals(rolePermissionId)) {
                throw new KapuaException(KapuaErrorCodes.PERMISSION_DELETE_NOT_ALLOWED);
            }

            return RolePermissionDAO.delete(em, scopeId, rolePermissionId);
        }).onAfterHandler((entity) -> {
            entityCache.remove(scopeId, rolePermissionId);
            entityCache.removeList(scopeId, entity.getRoleId());
        }));
    }

    @Override
    public RolePermission find(KapuaId scopeId, KapuaId rolePermissionId)
            throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(rolePermissionId, "rolePermissionId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.read, scopeId));

        return entityManagerSession.doAction(EntityManagerContainer.<RolePermission>create()
                .onBeforeHandler(() -> (RolePermission) entityCache.get(scopeId, rolePermissionId))
                .onResultHandler(em -> updateAuditFields(RolePermissionDAO.find(em, scopeId, rolePermissionId)))
                .onAfterHandler(entity -> entityCache.put(entity)));
    }

    @Override
    public RolePermissionListResult findByRoleId(KapuaId scopeId, KapuaId roleId)
            throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(roleId, "roleId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.read, scopeId));

        RolePermissionListResult listResult = (RolePermissionListResult) entityCache.getList(scopeId, roleId);
        if (listResult == null) {

            //
            // Build query
            RolePermissionQuery query = new RolePermissionQueryImpl(scopeId);
            query.setPredicate(query.attributePredicate(RolePermissionAttributes.ROLE_ID, roleId));

            listResult = query(query);
            entityCache.putList(scopeId, roleId, listResult);
        }
        return listResult;
    }

    @Override
    public RolePermissionListResult query(KapuaQuery<RolePermission> query)
            throws KapuaException {
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.read, query.getScopeId()));

        return entityManagerSession.doAction(EntityManagerContainer.<RolePermissionListResult>create().onResultHandler(em -> {
            RolePermissionListResult rolePermissionListResult = RolePermissionDAO.query(em, query);
            rolePermissionListResult.getItems().forEach(this::updateAuditFields);
            return rolePermissionListResult;
        }));
    }

    @Override
    public long count(KapuaQuery<RolePermission> query)
            throws KapuaException {
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.read, query.getScopeId()));

        return entityManagerSession.doAction(EntityManagerContainer.<Long>create().onResultHandler(em -> RolePermissionDAO.count(em, query)));
    }

    private RolePermission updateAuditFields(RolePermission rolePermission) {
        try {
            if (rolePermission != null && authorizationService.isPermitted(permissionFactory.newPermission(AuthorizationDomains.ROLE_DOMAIN, Actions.info, rolePermission.getScopeId()))) {
                rolePermission.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(rolePermission.getCreatedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return rolePermission;
    }

}
