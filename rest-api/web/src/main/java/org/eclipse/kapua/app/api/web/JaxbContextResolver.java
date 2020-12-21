/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.api.web;

import org.eclipse.kapua.app.api.core.exception.model.EntityNotFoundExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.ExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.IllegalArgumentExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.IllegalNullArgumentExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.InternalUserOnlyExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.MfaRequiredExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.SelfManagedOnlyExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.SubjectUnauthorizedExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.ThrowableInfo;
import org.eclipse.kapua.app.api.core.model.CountResult;
import org.eclipse.kapua.app.api.core.model.StorableEntityId;
import org.eclipse.kapua.app.api.core.model.data.JsonDatastoreMessage;
import org.eclipse.kapua.app.api.core.model.data.JsonMessageQuery;
import org.eclipse.kapua.app.api.core.model.device.management.JsonGenericRequestMessage;
import org.eclipse.kapua.app.api.core.model.device.management.JsonGenericResponseMessage;
import org.eclipse.kapua.app.api.core.model.job.IsJobRunningResponse;
import org.eclipse.kapua.app.api.core.model.message.JsonKapuaPayload;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreRecordCreator;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreRecordListResult;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreRecordQuery;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreXmlRegistry;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.job.engine.JobEngineXmlRegistry;
import org.eclipse.kapua.job.engine.JobStartOptions;
import org.eclipse.kapua.app.api.core.exception.model.CleanJobDataExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobAlreadyRunningExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobEngineExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobExecutionEnqueuedExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobInvalidTargetExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobMissingStepExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobMissingTargetExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobNotRunningExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobResumingExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobRunningExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobStartingExceptionInfo;
import org.eclipse.kapua.app.api.core.exception.model.JobStoppingExceptionInfo;
import org.eclipse.kapua.message.device.data.KapuaDataChannel;
import org.eclipse.kapua.message.device.data.KapuaDataMessage;
import org.eclipse.kapua.message.device.data.KapuaDataPayload;
import org.eclipse.kapua.message.xml.MessageXmlRegistry;
import org.eclipse.kapua.model.config.metatype.KapuaTad;
import org.eclipse.kapua.model.config.metatype.KapuaTicon;
import org.eclipse.kapua.model.config.metatype.KapuaTmetadata;
import org.eclipse.kapua.model.config.metatype.KapuaTocd;
import org.eclipse.kapua.model.config.metatype.KapuaToption;
import org.eclipse.kapua.service.account.Account;
import org.eclipse.kapua.service.account.AccountCreator;
import org.eclipse.kapua.service.account.AccountListResult;
import org.eclipse.kapua.service.account.AccountQuery;
import org.eclipse.kapua.service.account.AccountXmlRegistry;
import org.eclipse.kapua.service.authentication.ApiKeyCredentials;
import org.eclipse.kapua.service.authentication.AuthenticationCredentials;
import org.eclipse.kapua.service.authentication.AuthenticationXmlRegistry;
import org.eclipse.kapua.service.authentication.JwtCredentials;
import org.eclipse.kapua.service.authentication.RefreshTokenCredentials;
import org.eclipse.kapua.service.authentication.UsernamePasswordCredentials;
import org.eclipse.kapua.service.authentication.credential.Credential;
import org.eclipse.kapua.service.authentication.credential.CredentialCreator;
import org.eclipse.kapua.service.authentication.credential.CredentialListResult;
import org.eclipse.kapua.service.authentication.credential.CredentialQuery;
import org.eclipse.kapua.service.authentication.credential.CredentialType;
import org.eclipse.kapua.service.authentication.credential.CredentialXmlRegistry;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOption;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionCreator;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionListResult;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionQuery;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionXmlRegistry;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCode;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeCreator;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeListResult;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeQuery;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeXmlRegistry;
import org.eclipse.kapua.service.authentication.token.AccessToken;
import org.eclipse.kapua.service.authentication.token.LoginInfo;
import org.eclipse.kapua.service.authorization.access.AccessInfo;
import org.eclipse.kapua.service.authorization.access.AccessInfoCreator;
import org.eclipse.kapua.service.authorization.access.AccessInfoListResult;
import org.eclipse.kapua.service.authorization.access.AccessInfoQuery;
import org.eclipse.kapua.service.authorization.access.AccessInfoXmlRegistry;
import org.eclipse.kapua.service.authorization.access.AccessPermission;
import org.eclipse.kapua.service.authorization.access.AccessPermissionCreator;
import org.eclipse.kapua.service.authorization.access.AccessPermissionListResult;
import org.eclipse.kapua.service.authorization.access.AccessPermissionQuery;
import org.eclipse.kapua.service.authorization.access.AccessPermissionXmlRegistry;
import org.eclipse.kapua.service.authorization.access.AccessRole;
import org.eclipse.kapua.service.authorization.access.AccessRoleCreator;
import org.eclipse.kapua.service.authorization.access.AccessRoleListResult;
import org.eclipse.kapua.service.authorization.access.AccessRoleQuery;
import org.eclipse.kapua.service.authorization.access.AccessRoleXmlRegistry;
import org.eclipse.kapua.service.authorization.domain.Domain;
import org.eclipse.kapua.service.authorization.domain.DomainListResult;
import org.eclipse.kapua.service.authorization.domain.DomainQuery;
import org.eclipse.kapua.service.authorization.domain.DomainXmlRegistry;
import org.eclipse.kapua.service.authorization.group.Group;
import org.eclipse.kapua.service.authorization.group.GroupCreator;
import org.eclipse.kapua.service.authorization.group.GroupListResult;
import org.eclipse.kapua.service.authorization.group.GroupQuery;
import org.eclipse.kapua.service.authorization.group.GroupXmlRegistry;
import org.eclipse.kapua.service.authorization.permission.Permission;
import org.eclipse.kapua.service.authorization.role.Role;
import org.eclipse.kapua.service.authorization.role.RoleCreator;
import org.eclipse.kapua.service.authorization.role.RoleListResult;
import org.eclipse.kapua.service.authorization.role.RolePermission;
import org.eclipse.kapua.service.authorization.role.RolePermissionCreator;
import org.eclipse.kapua.service.authorization.role.RolePermissionListResult;
import org.eclipse.kapua.service.authorization.role.RolePermissionQuery;
import org.eclipse.kapua.service.authorization.role.RolePermissionXmlRegistry;
import org.eclipse.kapua.service.authorization.role.RoleQuery;
import org.eclipse.kapua.service.authorization.role.RoleXmlRegistry;
import org.eclipse.kapua.service.config.ServiceComponentConfiguration;
import org.eclipse.kapua.service.config.ServiceConfiguration;
import org.eclipse.kapua.service.config.ServiceConfigurationXmlRegistry;
import org.eclipse.kapua.service.datastore.model.ChannelInfo;
import org.eclipse.kapua.service.datastore.model.ChannelInfoListResult;
import org.eclipse.kapua.service.datastore.model.ClientInfo;
import org.eclipse.kapua.service.datastore.model.ClientInfoListResult;
import org.eclipse.kapua.service.datastore.model.DatastoreMessage;
import org.eclipse.kapua.service.datastore.model.MessageListResult;
import org.eclipse.kapua.service.datastore.model.MetricInfo;
import org.eclipse.kapua.service.datastore.model.MetricInfoListResult;
import org.eclipse.kapua.service.datastore.model.query.ChannelInfoQuery;
import org.eclipse.kapua.service.datastore.model.query.ClientInfoQuery;
import org.eclipse.kapua.service.datastore.model.query.MessageQuery;
import org.eclipse.kapua.service.datastore.model.query.MetricInfoQuery;
import org.eclipse.kapua.service.datastore.model.xml.ChannelInfoXmlRegistry;
import org.eclipse.kapua.service.datastore.model.xml.ClientInfoXmlRegistry;
import org.eclipse.kapua.service.datastore.model.xml.DatastoreMessageXmlRegistry;
import org.eclipse.kapua.service.datastore.model.xml.MetricInfoXmlRegistry;
import org.eclipse.kapua.service.device.call.kura.model.bundle.KuraBundles;
import org.eclipse.kapua.service.device.call.kura.model.configuration.KuraDeviceConfiguration;
import org.eclipse.kapua.service.device.call.kura.model.deploy.KuraDeploymentPackage;
import org.eclipse.kapua.service.device.call.kura.model.deploy.KuraDeploymentPackages;
import org.eclipse.kapua.service.device.call.kura.model.snapshot.KuraSnapshotIds;
import org.eclipse.kapua.service.device.management.asset.DeviceAssetXmlRegistry;
import org.eclipse.kapua.service.device.management.asset.DeviceAssets;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundle;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundleXmlRegistry;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundles;
import org.eclipse.kapua.service.device.management.command.DeviceCommandInput;
import org.eclipse.kapua.service.device.management.command.DeviceCommandOutput;
import org.eclipse.kapua.service.device.management.command.DeviceCommandXmlRegistry;
import org.eclipse.kapua.service.device.management.configuration.DeviceComponentConfiguration;
import org.eclipse.kapua.service.device.management.configuration.DeviceConfiguration;
import org.eclipse.kapua.service.device.management.configuration.DeviceConfigurationXmlRegistry;
import org.eclipse.kapua.service.device.management.message.notification.OperationStatus;
import org.eclipse.kapua.service.device.management.message.request.KapuaRequestChannel;
import org.eclipse.kapua.service.device.management.message.request.KapuaRequestMessage;
import org.eclipse.kapua.service.device.management.message.request.KapuaRequestPayload;
import org.eclipse.kapua.service.device.management.message.request.xml.RequestMessageXmlRegistry;
import org.eclipse.kapua.service.device.management.message.response.KapuaResponseChannel;
import org.eclipse.kapua.service.device.management.message.response.KapuaResponseMessage;
import org.eclipse.kapua.service.device.management.packages.model.DevicePackage;
import org.eclipse.kapua.service.device.management.packages.model.DevicePackageBundleInfo;
import org.eclipse.kapua.service.device.management.packages.model.DevicePackageBundleInfos;
import org.eclipse.kapua.service.device.management.packages.model.DevicePackageXmlRegistry;
import org.eclipse.kapua.service.device.management.packages.model.DevicePackages;
import org.eclipse.kapua.service.device.management.packages.model.download.DevicePackageDownloadRequest;
import org.eclipse.kapua.service.device.management.packages.model.install.DevicePackageInstallRequest;
import org.eclipse.kapua.service.device.management.packages.model.uninstall.DevicePackageUninstallRequest;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperation;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationCreator;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationListResult;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationQuery;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationXmlRegistry;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotification;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationCreator;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationListResult;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationQuery;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationXmlRegistry;
import org.eclipse.kapua.service.device.management.request.GenericRequestXmlRegistry;
import org.eclipse.kapua.service.device.management.request.message.request.GenericRequestChannel;
import org.eclipse.kapua.service.device.management.request.message.request.GenericRequestMessage;
import org.eclipse.kapua.service.device.management.request.message.request.GenericRequestPayload;
import org.eclipse.kapua.service.device.management.request.message.response.GenericResponseChannel;
import org.eclipse.kapua.service.device.management.request.message.response.GenericResponseMessage;
import org.eclipse.kapua.service.device.management.request.message.response.GenericResponsePayload;
import org.eclipse.kapua.service.device.management.snapshot.DeviceSnapshot;
import org.eclipse.kapua.service.device.management.snapshot.DeviceSnapshotXmlRegistry;
import org.eclipse.kapua.service.device.management.snapshot.DeviceSnapshots;
import org.eclipse.kapua.service.device.registry.Device;
import org.eclipse.kapua.service.device.registry.DeviceCreator;
import org.eclipse.kapua.service.device.registry.DeviceListResult;
import org.eclipse.kapua.service.device.registry.DeviceQuery;
import org.eclipse.kapua.service.device.registry.DeviceXmlRegistry;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnection;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionListResult;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionQuery;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionXmlRegistry;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOption;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionXmlRegistry;
import org.eclipse.kapua.service.device.registry.event.DeviceEvent;
import org.eclipse.kapua.service.device.registry.event.DeviceEventListResult;
import org.eclipse.kapua.service.device.registry.event.DeviceEventQuery;
import org.eclipse.kapua.service.device.registry.event.DeviceEventXmlRegistry;
import org.eclipse.kapua.service.endpoint.EndpointInfo;
import org.eclipse.kapua.service.endpoint.EndpointInfoCreator;
import org.eclipse.kapua.service.endpoint.EndpointInfoListResult;
import org.eclipse.kapua.service.endpoint.EndpointInfoQuery;
import org.eclipse.kapua.service.endpoint.EndpointInfoXmlRegistry;
import org.eclipse.kapua.service.endpoint.EndpointUsage;
import org.eclipse.kapua.service.job.Job;
import org.eclipse.kapua.service.job.JobListResult;
import org.eclipse.kapua.service.job.JobQuery;
import org.eclipse.kapua.service.job.JobXmlRegistry;
import org.eclipse.kapua.service.job.execution.JobExecution;
import org.eclipse.kapua.service.job.execution.JobExecutionListResult;
import org.eclipse.kapua.service.job.execution.JobExecutionQuery;
import org.eclipse.kapua.service.job.execution.JobExecutionXmlRegistry;
import org.eclipse.kapua.service.job.step.JobStep;
import org.eclipse.kapua.service.job.step.JobStepListResult;
import org.eclipse.kapua.service.job.step.JobStepQuery;
import org.eclipse.kapua.service.job.step.JobStepXmlRegistry;
import org.eclipse.kapua.service.job.step.definition.JobStepProperty;
import org.eclipse.kapua.service.job.targets.JobTarget;
import org.eclipse.kapua.service.job.targets.JobTargetListResult;
import org.eclipse.kapua.service.job.targets.JobTargetQuery;
import org.eclipse.kapua.service.scheduler.trigger.Trigger;
import org.eclipse.kapua.service.scheduler.trigger.TriggerListResult;
import org.eclipse.kapua.service.scheduler.trigger.TriggerQuery;
import org.eclipse.kapua.service.scheduler.trigger.TriggerXmlRegistry;
import org.eclipse.kapua.service.storable.model.id.StorableId;
import org.eclipse.kapua.service.storable.model.query.SortField;
import org.eclipse.kapua.service.storable.model.query.SortFieldXmlAdapter;
import org.eclipse.kapua.service.storable.model.query.XmlAdaptedSortField;
import org.eclipse.kapua.service.storable.model.query.XmlAdaptedSortFields;
import org.eclipse.kapua.service.tag.Tag;
import org.eclipse.kapua.service.tag.TagCreator;
import org.eclipse.kapua.service.tag.TagListResult;
import org.eclipse.kapua.service.tag.TagQuery;
import org.eclipse.kapua.service.tag.TagXmlRegistry;
import org.eclipse.kapua.service.user.User;
import org.eclipse.kapua.service.user.UserCreator;
import org.eclipse.kapua.service.user.UserListResult;
import org.eclipse.kapua.service.user.UserQuery;
import org.eclipse.kapua.service.user.UserXmlRegistry;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Provide a customized JAXBContext that makes the concrete implementations
 * known and available for marshalling
 *
 * @since 1.0.0
 */
@Provider
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class JaxbContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext jaxbContext;

    public JaxbContextResolver() {
        try {
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

            jaxbContext = JAXBContextFactory.createContext(new Class[]{

                    // REST API utility models
                    CountResult.class,

                    // REST API exception models
                    ThrowableInfo.class,
                    ExceptionInfo.class,

                    InternalUserOnlyExceptionInfo.class,
                    SelfManagedOnlyExceptionInfo.class,
                    SubjectUnauthorizedExceptionInfo.class,

                    EntityNotFoundExceptionInfo.class,
                    IllegalArgumentExceptionInfo.class,
                    IllegalNullArgumentExceptionInfo.class,
                    MfaRequiredExceptionInfo.class,

                    // Jobs Exception Info
                    CleanJobDataExceptionInfo.class,
                    JobAlreadyRunningExceptionInfo.class,
                    JobEngineExceptionInfo.class,
                    JobExecutionEnqueuedExceptionInfo.class,
                    JobInvalidTargetExceptionInfo.class,
                    JobMissingStepExceptionInfo.class,
                    JobMissingTargetExceptionInfo.class,
                    JobNotRunningExceptionInfo.class,
                    JobResumingExceptionInfo.class,
                    JobRunningExceptionInfo.class,
                    JobStartingExceptionInfo.class,
                    JobStoppingExceptionInfo.class,

                    // Tocds
                    KapuaTocd.class,
                    KapuaTad.class,
                    KapuaTicon.class,
                    KapuaTmetadata.class,
                    KapuaToption.class,

                    // Account
                    Account.class,
                    AccountCreator.class,
                    AccountListResult.class,
                    AccountQuery.class,
                    AccountXmlRegistry.class,

                    // Data Channel Info
                    ChannelInfo.class,
                    ChannelInfoListResult.class,
                    ChannelInfoQuery.class,
                    ChannelInfoXmlRegistry.class,

                    // Data Client Info
                    ClientInfo.class,
                    ClientInfoListResult.class,
                    ClientInfoQuery.class,
                    ClientInfoXmlRegistry.class,

                    // Data Metric Info
                    MetricInfo.class,
                    MetricInfoListResult.class,
                    MetricInfoQuery.class,
                    MetricInfoXmlRegistry.class,

                    // Data Messages
                    KapuaDataMessage.class,
                    KapuaDataChannel.class,
                    KapuaDataPayload.class,

                    MessageListResult.class,
                    MessageQuery.class,
                    MessageXmlRegistry.class,

                    JsonKapuaPayload.class,
                    JsonDatastoreMessage.class,

                    DatastoreMessage.class,
                    DatastoreMessageXmlRegistry.class,
                    StorableEntityId.class,
                    StorableId.class,
                    SortField.class,
                    SortFieldXmlAdapter.class,
                    XmlAdaptedSortField.class,
                    XmlAdaptedSortFields.class,
                    JsonMessageQuery.class,

                    // Device
                    Device.class,
                    DeviceCreator.class,
                    DeviceListResult.class,
                    DeviceQuery.class,
                    DeviceXmlRegistry.class,

                    // Device Connection
                    DeviceConnection.class,
                    DeviceConnectionListResult.class,
                    DeviceConnectionQuery.class,
                    DeviceConnectionXmlRegistry.class,

                    // Device Connection Options
                    DeviceConnectionOption.class,
                    DeviceConnectionOptionXmlRegistry.class,

                    // Device Event
                    DeviceEvent.class,
                    DeviceEventListResult.class,
                    DeviceEventQuery.class,
                    DeviceEventXmlRegistry.class,

                    // Device Management Assets
                    DeviceAssets.class,
                    DeviceAssetXmlRegistry.class,

                    // Device Management Bundles
                    KuraBundles.class,
                    DeviceBundle.class,
                    DeviceBundles.class,
                    DeviceBundleXmlRegistry.class,

                    // Device Management Command
                    DeviceCommandInput.class,
                    DeviceCommandOutput.class,
                    DeviceCommandXmlRegistry.class,

                    // Device Management Configuration
                    KuraDeviceConfiguration.class,
                    DeviceConfiguration.class,
                    DeviceComponentConfiguration.class,
                    DeviceConfigurationXmlRegistry.class,

                    // Device Management Snapshots
                    KuraSnapshotIds.class,
                    DeviceSnapshot.class,
                    DeviceSnapshots.class,
                    DeviceSnapshotXmlRegistry.class,

                    // Device Management Packages
                    KuraDeploymentPackages.class,
                    KuraDeploymentPackage.class,
                    DevicePackage.class,
                    DevicePackages.class,
                    DevicePackageBundleInfo.class,
                    DevicePackageBundleInfos.class,
                    DevicePackageDownloadRequest.class,
                    DevicePackageInstallRequest.class,
                    DevicePackageUninstallRequest.class,
                    DevicePackageXmlRegistry.class,

                    // Device Management Requests
                    KapuaRequestMessage.class,
                    KapuaResponseMessage.class,
                    KapuaRequestChannel.class,
                    KapuaResponseChannel.class,
                    KapuaRequestPayload.class,
                    RequestMessageXmlRegistry.class,

                    // Device Management Registry
                    DeviceManagementOperation.class,
                    DeviceManagementOperationCreator.class,
                    DeviceManagementOperationListResult.class,
                    DeviceManagementOperationQuery.class,
                    DeviceManagementOperationXmlRegistry.class,
                    OperationStatus.class,

                    // Device Management Registry Notification
                    ManagementOperationNotification.class,
                    ManagementOperationNotificationCreator.class,
                    ManagementOperationNotificationListResult.class,
                    ManagementOperationNotificationQuery.class,
                    ManagementOperationNotificationXmlRegistry.class,

                    // Device Management Generic Request
                    GenericRequestChannel.class,
                    GenericRequestPayload.class,
                    GenericRequestMessage.class,
                    GenericResponseChannel.class,
                    GenericResponsePayload.class,
                    GenericResponseMessage.class,
                    GenericRequestXmlRegistry.class,

                    JsonGenericRequestMessage.class,
                    JsonGenericResponseMessage.class,

                    // Authentication
                    AuthenticationCredentials.class,
                    AuthenticationXmlRegistry.class,
                    AccessToken.class,
                    LoginInfo.class,
                    ApiKeyCredentials.class,
                    JwtCredentials.class,
                    UsernamePasswordCredentials.class,
                    RefreshTokenCredentials.class,

                    // Credential
                    Credential.class,
                    CredentialListResult.class,
                    CredentialCreator.class,
                    CredentialType.class,
                    CredentialQuery.class,
                    CredentialXmlRegistry.class,

                    // Multi Factor Authentication
                    MfaOption.class,
                    MfaOptionListResult.class,
                    MfaOptionCreator.class,
                    MfaOptionQuery.class,
                    MfaOptionXmlRegistry.class,
                    ScratchCode.class,
                    ScratchCodeListResult.class,
                    ScratchCodeCreator.class,
                    ScratchCodeQuery.class,
                    ScratchCodeXmlRegistry.class,

                    // Permission
                    Permission.class,

                    // Endpoint Info
                    EndpointUsage.class,
                    EndpointInfo.class,
                    EndpointInfoListResult.class,
                    EndpointInfoCreator.class,
                    EndpointInfoQuery.class,
                    EndpointInfoXmlRegistry.class,

                    // Roles
                    Role.class,
                    RoleListResult.class,
                    RoleCreator.class,
                    RoleQuery.class,
                    RoleXmlRegistry.class,

                    // Role Permissions
                    RolePermission.class,
                    RolePermissionListResult.class,
                    RolePermissionCreator.class,
                    RolePermissionQuery.class,
                    RolePermissionXmlRegistry.class,

                    // Domains
                    Domain.class,
                    DomainListResult.class,
                    DomainQuery.class,
                    DomainXmlRegistry.class,

                    // Groups
                    Group.class,
                    GroupListResult.class,
                    GroupCreator.class,
                    GroupQuery.class,
                    GroupXmlRegistry.class,

                    // Access Info
                    AccessInfo.class,
                    AccessInfoListResult.class,
                    AccessInfoCreator.class,
                    AccessInfoQuery.class,
                    AccessInfoXmlRegistry.class,

                    // Access Permissions
                    AccessPermission.class,
                    AccessPermissionListResult.class,
                    AccessPermissionCreator.class,
                    AccessPermissionQuery.class,
                    AccessPermissionXmlRegistry.class,

                    // Access Roles
                    AccessRole.class,
                    AccessRoleListResult.class,
                    AccessRoleCreator.class,
                    AccessRoleQuery.class,
                    AccessRoleXmlRegistry.class,

                    // Tag
                    Tag.class,
                    TagListResult.class,
                    TagCreator.class,
                    TagQuery.class,
                    TagXmlRegistry.class,

                    // User
                    User.class,
                    UserCreator.class,
                    UserListResult.class,
                    UserQuery.class,
                    UserXmlRegistry.class,

                    // KapuaEvent
                    ServiceEvent.class,
                    EventStoreRecordCreator.class,
                    EventStoreRecordListResult.class,
                    EventStoreRecordQuery.class,
                    EventStoreXmlRegistry.class,

                    // Service Config
                    ServiceConfigurationXmlRegistry.class,
                    ServiceConfiguration.class,
                    ServiceComponentConfiguration.class,

                    // Jobs
                    Job.class,
                    JobStartOptions.class,
                    IsJobRunningResponse.class,
                    JobListResult.class,
                    JobQuery.class,
                    JobXmlRegistry.class,
                    JobEngineXmlRegistry.class,

                    JobStep.class,
                    JobStepListResult.class,
                    JobStepQuery.class,
                    JobStepXmlRegistry.class,
                    JobStepProperty.class,

                    JobExecution.class,
                    JobExecutionListResult.class,
                    JobExecutionQuery.class,
                    JobExecutionXmlRegistry.class,

                    JobTarget.class,
                    JobTargetListResult.class,
                    JobTargetQuery.class,
                    JobExecutionXmlRegistry.class,

                    Trigger.class,
                    TriggerListResult.class,
                    TriggerQuery.class,
                    TriggerXmlRegistry.class
            }, properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JAXBContext getContext(Class<?> type) {
        return jaxbContext;
    }

}
