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
package org.eclipse.kapua.service.device.management.job.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaEntityUniquenessException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.AbstractKapuaConfigurableResourceLimitedService;
import org.eclipse.kapua.commons.model.query.predicate.AndPredicateImpl;
import org.eclipse.kapua.commons.model.query.predicate.AttributePredicateImpl;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.locator.KapuaProvider;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperation;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationAttributes;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationCreator;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationFactory;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationListResult;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationQuery;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationService;
import org.eclipse.kapua.service.job.JobDomains;
import org.eclipse.kapua.service.user.UserService;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link JobDeviceManagementOperationService} implementation
 *
 * @since 1.1.0
 */
@KapuaProvider
public class JobDeviceManagementOperationServiceImpl extends AbstractKapuaConfigurableResourceLimitedService<JobDeviceManagementOperation, JobDeviceManagementOperationCreator, JobDeviceManagementOperationService, JobDeviceManagementOperationListResult, JobDeviceManagementOperationQuery, JobDeviceManagementOperationFactory>
        implements JobDeviceManagementOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobDeviceManagementOperationServiceImpl.class);

    private static final KapuaLocator LOCATOR = KapuaLocator.getInstance();

    private static final AuthorizationService AUTHORIZATION_SERVICE = LOCATOR.getService(AuthorizationService.class);
    private static final PermissionFactory PERMISSION_FACTORY = LOCATOR.getFactory(PermissionFactory.class);
    private static final UserService USER_SERVICE = LOCATOR.getService(UserService.class);

    public JobDeviceManagementOperationServiceImpl() {
        super(JobDeviceManagementOperationService.class.getName(), JobDomains.JOB_DOMAIN, JobDeviceManagementOperationEntityManagerFactory.getInstance(), JobDeviceManagementOperationService.class, JobDeviceManagementOperationFactory.class);
    }

    @Override
    public JobDeviceManagementOperation create(JobDeviceManagementOperationCreator jobDeviceManagementOperationCreator) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator, "jobDeviceManagementOperationCreator");
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator.getScopeId(), "jobDeviceManagementOperationCreator.scopeId");
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator.getJobId(), "jobDeviceManagementOperationCreator.jobId");
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator.getDeviceManagementOperationId(), "jobDeviceManagementOperationCreator.deviceManagementOperationId");

        //
        // Check access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.write, null));

        //
        // Check duplicate
        JobDeviceManagementOperationQuery query = new JobDeviceManagementOperationQueryImpl(jobDeviceManagementOperationCreator.getScopeId());
        query.setPredicate(
                new AndPredicateImpl(
                        new AttributePredicateImpl<>(JobDeviceManagementOperationAttributes.JOB_ID, jobDeviceManagementOperationCreator.getJobId()),
                        new AttributePredicateImpl<>(JobDeviceManagementOperationAttributes.DEVICE_MANAGEMENT_OPERATION_ID, jobDeviceManagementOperationCreator.getDeviceManagementOperationId())
                )
        );

        if (count(query) > 0) {
            List<Map.Entry<String, Object>> uniqueAttributes = new ArrayList<>();

            uniqueAttributes.add(new AbstractMap.SimpleEntry<>(JobDeviceManagementOperationAttributes.JOB_ID, jobDeviceManagementOperationCreator.getJobId()));
            uniqueAttributes.add(new AbstractMap.SimpleEntry<>(JobDeviceManagementOperationAttributes.DEVICE_MANAGEMENT_OPERATION_ID, jobDeviceManagementOperationCreator.getDeviceManagementOperationId()));

            throw new KapuaEntityUniquenessException(JobDeviceManagementOperation.TYPE, uniqueAttributes);
        }

        //
        // Do create
        return entityManagerSession.doTransactedAction(em -> updateAuditFields(JobDeviceManagementOperationDAO.create(em, jobDeviceManagementOperationCreator)));
    }


    @Override
    public JobDeviceManagementOperation find(KapuaId scopeId, KapuaId jobDeviceManagementOperationId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(jobDeviceManagementOperationId, "jobDeviceManagementOperationId");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.write, scopeId));

        //
        // Do find
        return entityManagerSession.doAction(em -> updateAuditFields(JobDeviceManagementOperationDAO.find(em, scopeId, jobDeviceManagementOperationId)));
    }

    @Override
    public JobDeviceManagementOperationListResult query(KapuaQuery<JobDeviceManagementOperation> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> {
            JobDeviceManagementOperationListResult jobDeviceManagementOperationListResult = JobDeviceManagementOperationDAO.query(em, query);
            jobDeviceManagementOperationListResult.getItems().forEach(this::updateAuditFields);
            return jobDeviceManagementOperationListResult;
        });
    }

    @Override
    public long count(KapuaQuery<JobDeviceManagementOperation> query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> JobDeviceManagementOperationDAO.count(em, query));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId jobDeviceManagementOperationId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(jobDeviceManagementOperationId, "jobDeviceManagementOperationId");

        //
        // Check Access
        AUTHORIZATION_SERVICE.checkPermission(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.delete, scopeId));

        //
        // Check existence
        if (find(scopeId, jobDeviceManagementOperationId) == null) {
            throw new KapuaEntityNotFoundException(JobDeviceManagementOperation.TYPE, jobDeviceManagementOperationId);
        }

        //
        // Do delete
        entityManagerSession.doTransactedAction(em -> JobDeviceManagementOperationDAO.delete(em, scopeId, jobDeviceManagementOperationId));
    }

    private JobDeviceManagementOperation updateAuditFields(JobDeviceManagementOperation jobDeviceManagementOperation) {
        try {
            if (jobDeviceManagementOperation != null && AUTHORIZATION_SERVICE.isPermitted(PERMISSION_FACTORY.newPermission(JobDomains.JOB_DOMAIN, Actions.info, jobDeviceManagementOperation.getScopeId()))) {
                jobDeviceManagementOperation.setCreatedByName(KapuaSecurityUtils.doPrivileged(() -> USER_SERVICE.getName(jobDeviceManagementOperation.getCreatedBy())));
                jobDeviceManagementOperation.setModifiedByName(KapuaSecurityUtils.doPrivileged(() -> USER_SERVICE.getName(jobDeviceManagementOperation.getModifiedBy())));
            }
        } catch (KapuaException ex) {
            LOGGER.warn("Unable to resolve entity name");
        }
        return jobDeviceManagementOperation;
    }

}
