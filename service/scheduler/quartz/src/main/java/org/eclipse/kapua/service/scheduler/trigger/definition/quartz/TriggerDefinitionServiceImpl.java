/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.scheduler.trigger.definition.quartz;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.service.internal.AbstractKapuaService;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.job.JobDomains;
import org.eclipse.kapua.service.scheduler.quartz.SchedulerEntityManagerFactory;
import org.eclipse.kapua.service.scheduler.trigger.definition.TriggerDefinition;
import org.eclipse.kapua.service.scheduler.trigger.definition.TriggerDefinitionCreator;
import org.eclipse.kapua.service.scheduler.trigger.definition.TriggerDefinitionListResult;
import org.eclipse.kapua.service.scheduler.trigger.definition.TriggerDefinitionService;
import org.eclipse.kapua.service.user.UserService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TriggerDefinitionService} exposes APIs to manage {@link TriggerDefinition} objects.<br>
 * It includes APIs to create, update, find, list and delete {@link TriggerDefinition}s.<br>
 *
 * @since 1.1.0
 */
@KapuaProvider
public class TriggerDefinitionServiceImpl extends AbstractKapuaService implements TriggerDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerDefinitionServiceImpl.class);

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PermissionFactory permissionFactory;

    @Inject
    private UserService userService;

    public TriggerDefinitionServiceImpl() {
        super(SchedulerEntityManagerFactory.getInstance());
    }

    @Override
    public TriggerDefinition create(TriggerDefinitionCreator creator) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(creator, "stepDefinitionCreator");
        ArgumentValidator.notNull(creator.getScopeId(), "stepDefinitionCreator.scopeId");
        ArgumentValidator.notNull(creator.getTriggerType(), "stepDefinitionCreator.stepType");
        ArgumentValidator.validateEntityName(creator.getName(), "stepDefinitionCreator.name");
        ArgumentValidator.notEmptyOrNull(creator.getProcessorName(), "stepDefinitionCreator.processorName");

        //
        // Check access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.write, null));

        //
        // Do create
        return entityManagerSession.doTransactedAction(em -> updateAuditFields(TriggerDefinitionDAO.create(em, creator)));
    }

    @Override
    public TriggerDefinition update(TriggerDefinition triggerDefinition) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(triggerDefinition, "stepDefinition");
        ArgumentValidator.notNull(triggerDefinition.getScopeId(), "stepDefinition.scopeId");
        ArgumentValidator.notNull(triggerDefinition.getTriggerType(), "triggerDefinition.stepType");
        ArgumentValidator.validateEntityName(triggerDefinition.getName(), "triggerDefinition.name");
        ArgumentValidator.notEmptyOrNull(triggerDefinition.getProcessorName(), "triggerDefinition.processorName");

        //
        // Check access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.write, null));

        return entityManagerSession.doTransactedAction(em -> updateAuditFields(TriggerDefinitionDAO.update(em, triggerDefinition)));
    }

    @Override
    public TriggerDefinition find(KapuaId stepDefinitionId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(stepDefinitionId, "stepDefinitionId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, KapuaId.ANY));

        //
        // Do find
        return entityManagerSession.doAction(em -> updateAuditFields(TriggerDefinitionDAO.find(em, stepDefinitionId)));
    }

    @Override
    public TriggerDefinition find(KapuaId scopeId, KapuaId stepDefinitionId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(stepDefinitionId, "stepDefinitionId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, KapuaId.ANY));

        //
        // Do find
        return entityManagerSession.doAction(em -> updateAuditFields(TriggerDefinitionDAO.find(em, scopeId, stepDefinitionId)));
    }

    @Override
    public TriggerDefinition findByName(String name) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(name, "name");

        //
        // Do find
        return entityManagerSession.doAction(em -> {
            TriggerDefinition triggerDefinition = TriggerDefinitionDAO.findByName(em, name);
            if (triggerDefinition != null) {
                //
                // Check Access
                authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, KapuaId.ANY));
            }
            return updateAuditFields(triggerDefinition);
        });
    }

    @Override
    public TriggerDefinitionListResult query(KapuaQuery<TriggerDefinition> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, KapuaId.ANY));

        //
        // Do query
        return entityManagerSession.doAction(em -> {
            TriggerDefinitionListResult triggerDefinitionListResult = TriggerDefinitionDAO.query(em, query);
            triggerDefinitionListResult.getItems().forEach(this::updateAuditFields);
            return triggerDefinitionListResult;
        });
    }

    @Override
    public long count(KapuaQuery<TriggerDefinition> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, KapuaId.ANY));

        //
        // Do query
        return entityManagerSession.doAction(em -> TriggerDefinitionDAO.count(em, query));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId stepDefinitionId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(stepDefinitionId, "stepDefinitionId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.delete, null));

        //
        // Do delete
        entityManagerSession.doTransactedAction(em -> {
            if (TriggerDefinitionDAO.find(em, scopeId, stepDefinitionId) == null) {
                throw new KapuaEntityNotFoundException(TriggerDefinition.TYPE, stepDefinitionId);
            }

            return TriggerDefinitionDAO.delete(em, scopeId, stepDefinitionId);
        });
    }

    private TriggerDefinition updateAuditFields(TriggerDefinition triggerDefinition) {
        try {
            if (triggerDefinition != null && authorizationService.isPermitted(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.info, triggerDefinition.getScopeId()))) {
                triggerDefinition.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(triggerDefinition.getCreatedBy())));
                triggerDefinition.setModifiedByName(KapuaSecurityUtils.doPrivileged(() -> userService.getName(triggerDefinition.getModifiedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return triggerDefinition;
    }

}
