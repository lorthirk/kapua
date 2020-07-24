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
package org.eclipse.kapua.service.authorization.group.shiro;

import javax.inject.Inject;

import org.eclipse.kapua.KapuaDuplicateNameException;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaMaxNumberOfItemsReachedException;
import org.eclipse.kapua.commons.configuration.AbstractKapuaConfigurableResourceLimitedService;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.KapuaEntityAttributes;
import org.eclipse.kapua.model.KapuaNamedEntityAttributes;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.model.query.predicate.AttributePredicate.Operator;
import org.eclipse.kapua.service.authorization.AuthorizationDomains;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.group.Group;
import org.eclipse.kapua.service.authorization.group.GroupCreator;
import org.eclipse.kapua.service.authorization.group.GroupFactory;
import org.eclipse.kapua.service.authorization.group.GroupListResult;
import org.eclipse.kapua.service.authorization.group.GroupQuery;
import org.eclipse.kapua.service.authorization.group.GroupService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.authorization.shiro.AuthorizationEntityManagerFactory;
import org.eclipse.kapua.service.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GroupService} implementation.
 *
 * @since 1.0.0
 */
@KapuaProvider
public class GroupServiceImpl extends AbstractKapuaConfigurableResourceLimitedService<Group, GroupCreator, GroupService, GroupListResult, GroupQuery, GroupFactory> implements GroupService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PermissionFactory permissionFactory;

    @Inject
    private UserService userService;

    public GroupServiceImpl() {
        super(GroupService.class.getName(), AuthorizationDomains.GROUP_DOMAIN, AuthorizationEntityManagerFactory.getInstance(), GroupService.class, GroupFactory.class);
    }

    @Override
    public Group create(GroupCreator groupCreator) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(groupCreator, "groupCreator");
        ArgumentValidator.notNull(groupCreator.getScopeId(), "roleCreator.scopeId");
        ArgumentValidator.validateEntityName(groupCreator.getName(), "groupCreator.name");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.write, groupCreator.getScopeId()));

        //
        // Check limits
        if (allowedChildEntities(groupCreator.getScopeId()) <= 0) {
            throw new KapuaMaxNumberOfItemsReachedException("Groups");
        }

        //
        // Check duplicate name
        GroupQuery query = new GroupQueryImpl(groupCreator.getScopeId());
        query.setPredicate(query.attributePredicate(KapuaNamedEntityAttributes.NAME, groupCreator.getName()));

        if (count(query) > 0) {
            throw new KapuaDuplicateNameException(groupCreator.getName());
        }

        //
        // Do create
        return entityManagerSession.doTransactedAction(em -> updateAuditFields(GroupDAO.create(em, groupCreator)));
    }

    @Override
    public Group update(Group group) throws KapuaException {
        //
        // Argument validator
        ArgumentValidator.notNull(group, "group");
        ArgumentValidator.notNull(group.getId(), "group.id");
        ArgumentValidator.notNull(group.getScopeId(), "group.scopeId");
        ArgumentValidator.validateEntityName(group.getName(), "group.name");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.write, group.getScopeId()));

        //
        // Check existence
        if (find(group.getScopeId(), group.getId()) == null) {
            throw new KapuaEntityNotFoundException(Group.TYPE, group.getId());
        }

        //
        // Check duplicate name
        GroupQuery query = new GroupQueryImpl(group.getScopeId());
        query.setPredicate(
                query.andPredicate(
                        query.attributePredicate(KapuaNamedEntityAttributes.NAME, group.getName()),
                        query.attributePredicate(KapuaEntityAttributes.ENTITY_ID, group.getId(), Operator.NOT_EQUAL)
                )
        );

        if (count(query) > 0) {
            throw new KapuaDuplicateNameException(group.getName());
        }

        //
        // Do update
        return entityManagerSession.doTransactedAction(em -> updateAuditFields(GroupDAO.update(em, group)));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId groupId) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(groupId, "groupId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.delete, scopeId));

        //
        // Check existence
        if (find(scopeId, groupId) == null) {
            throw new KapuaEntityNotFoundException(Group.TYPE, groupId);
        }

        //
        // Do delete
        entityManagerSession.doTransactedAction(em -> updateAuditFields(GroupDAO.delete(em, scopeId, groupId)));
    }

    @Override
    public Group find(KapuaId scopeId, KapuaId groupId) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(groupId, "groupId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.read, scopeId));

        //
        // Do find
        return entityManagerSession.doAction(em -> GroupDAO.find(em, scopeId, groupId));
    }

    @Override
    public GroupListResult query(KapuaQuery<Group> query) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> {
            GroupListResult groupListResult = GroupDAO.query(em, query);
            groupListResult.getItems().forEach(this::updateAuditFields);
            return groupListResult;
        });
    }

    @Override
    public long count(KapuaQuery<Group> query) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do count
        return entityManagerSession.doAction(em -> GroupDAO.count(em, query));
    }

    //@ListenServiceEvent(fromAddress="account")
    public void onKapuaEvent(ServiceEvent kapuaEvent) throws KapuaException {
        if (kapuaEvent == null) {
            //service bus error. Throw some exception?
        }

        LOG.info("GroupService: received kapua event from {}, operation {}", kapuaEvent.getService(), kapuaEvent.getOperation());
        if ("account".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteGroupByAccountId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        }
    }

    private void deleteGroupByAccountId(KapuaId scopeId, KapuaId accountId) throws KapuaException {
        GroupQuery query = new GroupQueryImpl(accountId);

        GroupListResult groupsToDelete = query(query);

        for (Group g : groupsToDelete.getItems()) {
            delete(g.getScopeId(), g.getId());
        }
    }

    private Group updateAuditFields(Group group) {
        try {
            if (group != null && authorizationService.isPermitted(permissionFactory.newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.info, group.getScopeId()))) {
                group.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(group.getCreatedBy())));
                group.setModifiedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(group.getModifiedBy())));
            }
        } catch (KapuaException ex) {
            LOG.warn("Unable to resolve entity name");
        }
        return group;
    }

}
