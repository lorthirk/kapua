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
package org.eclipse.kapua.service.job.execution.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.AbstractKapuaConfigurableResourceLimitedService;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.job.JobDomains;
import org.eclipse.kapua.service.job.execution.JobExecution;
import org.eclipse.kapua.service.job.execution.JobExecutionCreator;
import org.eclipse.kapua.service.job.execution.JobExecutionFactory;
import org.eclipse.kapua.service.job.execution.JobExecutionListResult;
import org.eclipse.kapua.service.job.execution.JobExecutionQuery;
import org.eclipse.kapua.service.job.execution.JobExecutionService;
import org.eclipse.kapua.service.job.internal.JobEntityManagerFactory;
import org.eclipse.kapua.service.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link JobExecutionService} implementation
 *
 * @since 1.0.0
 */
@KapuaProvider
public class JobExecutionServiceImpl
        extends AbstractKapuaConfigurableResourceLimitedService<JobExecution, JobExecutionCreator, JobExecutionService, JobExecutionListResult, JobExecutionQuery, JobExecutionFactory>
        implements JobExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionServiceImpl.class);
    private static final KapuaLocator LOCATOR = KapuaLocator.getInstance();

    private static final AuthorizationService AUTHORIZATION_SERVICE = LOCATOR.getService(AuthorizationService.class);
    private static final PermissionFactory PERMISSION_FACTORY = LOCATOR.getFactory(PermissionFactory.class);

    private static final UserService USER_SERVICE = LOCATOR.getService(UserService.class);

    public JobExecutionServiceImpl() {
        super(JobExecutionService.class.getName(), JobDomains.JOB_DOMAIN, JobEntityManagerFactory.getInstance(), JobExecutionService.class, JobExecutionFactory.class);
    }

    @Override
    public JobExecution create(JobExecutionCreator creator) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(creator, "jobExecutionCreator");
        ArgumentValidator.notNull(creator.getScopeId(), "jobExecutionCreator.scopeId");

        //
        // Check access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.write, creator.getScopeId()));

        //
        // Do create
        return entityManagerSession.doTransactedAction(em -> updateAuditFields(JobExecutionDAO.create(em, creator)));
    }

    @Override
    public JobExecution update(JobExecution jobExecution) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(jobExecution, "jobExecution");
        ArgumentValidator.notNull(jobExecution.getScopeId(), "jobExecution.scopeId");

        //
        // Check access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.write, jobExecution.getScopeId()));

        return entityManagerSession.doTransactedAction(em -> updateAuditFields(JobExecutionDAO.update(em, jobExecution)));
    }

    @Override
    public JobExecution find(KapuaId scopeId, KapuaId jobExecutionId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(jobExecutionId, "jobExecutionId");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.read, scopeId));

        //
        // Do find
        return entityManagerSession.doAction(em -> updateAuditFields(JobExecutionDAO.find(em, scopeId, jobExecutionId)));
    }

    @Override
    public JobExecutionListResult query(KapuaQuery<JobExecution> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> {
            JobExecutionListResult jobExecutionListResult = JobExecutionDAO.query(em, query);
            jobExecutionListResult.getItems().forEach(this::updateAuditFields);
            return jobExecutionListResult;
        });
    }

    @Override
    public long count(KapuaQuery<JobExecution> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> JobExecutionDAO.count(em, query));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId jobExecutionId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(jobExecutionId, "jobExecutionId");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.delete, scopeId));

        //
        // Do delete
        entityManagerSession.doTransactedAction(em -> {
            if (JobExecutionDAO.find(em, scopeId, jobExecutionId) == null) {
                throw new KapuaEntityNotFoundException(JobExecution.TYPE, jobExecutionId);
            }

            return JobExecutionDAO.delete(em, scopeId, jobExecutionId);
        });
    }

    private JobExecution updateAuditFields(JobExecution jobExecution) {
        try {
            if (jobExecution != null && AUTHORIZATION_SERVICE.isPermitted(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.info, jobExecution.getScopeId()))) {
                jobExecution.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> USER_SERVICE.getName(jobExecution.getCreatedBy())));
                jobExecution.setModifiedByName(KapuaSecurityUtils.doPrivileged(() -> USER_SERVICE.getName(jobExecution.getModifiedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return jobExecution;
    }

}
