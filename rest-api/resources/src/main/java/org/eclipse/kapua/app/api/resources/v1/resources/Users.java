/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.api.resources.v1.resources;

import java.io.StringReader;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.oxm.json.JsonStructureSource;
import org.xml.sax.SAXException;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.app.api.resources.v1.resources.model.CountResult;
import org.eclipse.kapua.app.api.resources.v1.resources.model.EntityId;
import org.eclipse.kapua.app.api.resources.v1.resources.model.query.RestQuery;
import org.eclipse.kapua.app.api.resources.v1.resources.model.ScopeId;
import org.eclipse.kapua.commons.model.id.KapuaEid;
import org.eclipse.kapua.commons.util.xml.XmlUtil;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.KapuaNamedEntityAttributes;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.predicate.AndPredicate;
import org.eclipse.kapua.model.query.predicate.AttributePredicate;
import org.eclipse.kapua.model.query.predicate.AttributePredicate.Operator;
import org.eclipse.kapua.service.KapuaService;
import org.eclipse.kapua.service.user.User;
import org.eclipse.kapua.service.user.UserCreator;
import org.eclipse.kapua.service.user.UserFactory;
import org.eclipse.kapua.service.user.UserListResult;
import org.eclipse.kapua.service.user.UserQuery;
import org.eclipse.kapua.service.user.UserService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

@Path("{scopeId}/users")
public class Users extends AbstractKapuaResource {

    private final KapuaLocator locator = KapuaLocator.getInstance();
    private final UserService userService = locator.getService(UserService.class);
    private final UserFactory userFactory = locator.getFactory(UserFactory.class);

    /**
     * Gets the {@link User} list in the scope.
     *
     * @param scopeId The {@link ScopeId} in which to search results.
     * @param name    The {@link User} name in which to search results.
     * @param offset  The result set offset.
     * @param limit   The result set limit.
     * @return The {@link UserListResult} of all the users associated to the current selected scope.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public UserListResult simpleQuery(
            @PathParam("scopeId") ScopeId scopeId,
            @QueryParam("name") String name,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("50") int limit) throws KapuaException {
        UserQuery query = userFactory.newQuery(scopeId);

        AndPredicate andPredicate = query.andPredicate();
        if (!Strings.isNullOrEmpty(name)) {
            andPredicate.and(query.attributePredicate(KapuaNamedEntityAttributes.NAME, name));
        }
        query.setPredicate(andPredicate);

        query.setOffset(offset);
        query.setLimit(limit);

        return query(scopeId, query);
    }

    /**
     * Queries the results with the given {@link UserQuery} parameter.
     *
     * @param scopeId The {@link ScopeId} in which to search results.
     * @param query   The {@link UserQuery} to use to filter results.
     * @return The {@link UserListResult} of all the result matching the given {@link UserQuery} parameter.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @POST
    @Path("_query")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public UserListResult query(
            @PathParam("scopeId") ScopeId scopeId,
            UserQuery query) throws KapuaException {
        query.setScopeId(scopeId);

        return userService.query(query);
    }

    /**
     * Counts the results with the given {@link UserQuery} parameter.
     *
     * @param scopeId The {@link ScopeId} in which to count results.
     * @param query   The {@link UserQuery} to use to filter results.
     * @return The count of all the result matching the given {@link UserQuery} parameter.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @POST
    @Path("_count")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public CountResult count(
            @PathParam("scopeId") ScopeId scopeId,
            UserQuery query) throws KapuaException {
        query.setScopeId(scopeId);

        return new CountResult(userService.count(query));
    }

    /**
     * Creates a new User based on the information provided in UserCreator
     * parameter.
     *
     * @param scopeId     The {@link ScopeId} in which to create the {@link User}
     * @param userCreator Provides the information for the new User to be created.
     * @return The newly created User object.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response create(
            @PathParam("scopeId") ScopeId scopeId,
            UserCreator userCreator) throws KapuaException {
        userCreator.setScopeId(scopeId);

        return returnCreated(userService.create(userCreator));
    }

    /**
     * Returns the User specified by the "userId" path parameter.
     *
     * @param scopeId The {@link ScopeId} of the requested {@link User}.
     * @param userId  The id of the requested User.
     * @return The requested User object.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @GET
    @Path("{userId}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public User find(
            @PathParam("scopeId") ScopeId scopeId,
            @PathParam("userId") EntityId userId) throws KapuaException {
        User user = userService.find(scopeId, userId);

        if (user == null) {
            throw new KapuaEntityNotFoundException(User.TYPE, userId);
        }

        return user;
    }

    /**
     * Updates the User based on the information provided in the User parameter.
     *
     * @param scopeId The ScopeId of the requested {@link User}.
     * @param userId  The id of the requested {@link User}
     * @param user    The modified User whose attributed need to be updated.
     * @return The updated user.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @PUT
    @Path("{userId}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public User update(
            @PathParam("scopeId") ScopeId scopeId,
            @PathParam("userId") EntityId userId,
            User user) throws KapuaException {
        user.setScopeId(scopeId);
        user.setId(userId);

        return userService.update(user);
    }

    /**
     * Deletes the User specified by the "userId" path parameter.
     *
     * @param scopeId The ScopeId of the requested {@link User}.
     * @param userId  The id of the User to be deleted.
     * @return HTTP 200 if operation has completed successfully.
     * @throws KapuaException Whenever something bad happens. See specific {@link KapuaService} exceptions.
     * @since 1.0.0
     */
    @DELETE
    @Path("{userId}")
    public Response deleteUser(
            @PathParam("scopeId") ScopeId scopeId,
            @PathParam("userId") EntityId userId) throws KapuaException {
        userService.delete(scopeId, userId);

        return returnNoContent();
    }

    @POST
    @Path(("_read"))
    @Consumes({ MediaType.APPLICATION_JSON })
    public UserListResult read(@PathParam("scopeId") ScopeId scopeId,
                               String restQueryJsonObject) throws KapuaException, JAXBException, SAXException, XMLStreamException, JsonProcessingException {
        UserQuery userQuery = convertQuery(restQueryJsonObject, scopeId);
        UserListResult userListResult = userService.query(userQuery);
        return userListResult;
    }

    @GET
    @Path(("write"))
    public RestQuery write(@PathParam("scopeId") ScopeId scopeId) throws KapuaException {
        RestQuery restQuery = new RestQuery();
        Map<String, Object> map = Stream.of(new AbstractMap.SimpleEntry<>("id", new KapuaId[]{ KapuaId.ONE, KapuaEid.parseCompactId("Ag") }))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        restQuery.setFields(map);
        return restQuery;
    }

    private UserQuery convertQuery(String restQueryJson, ScopeId scopeId) throws JsonProcessingException {
//        JsonReader jsonReader = Json.createReader(new StringReader(restQueryJson));
//        JsonObject jsonObject = jsonReader.readObject();
//        JsonStructureSource jsonStructureSource = new JsonStructureSource(jsonObject);
        ObjectMapper mapper = new ObjectMapper();
        RestQuery restQuery = mapper.readValue(restQueryJson, RestQuery.class);
//        RestQuery restQuery = XmlUtil.unmarshalJson(jsonStructureSource, RestQuery.class, null);
        UserQuery userQuery = userFactory.newQuery(scopeId);
        AndPredicate andPredicate = userQuery.andPredicate();
//        if (StringUtils.isNotEmpty(restQuery.getMatch())) {
//            // TODO UserMatchPredicate missing, and not currently possible due to circular dependency
//            // between kapua-commons and kapua-user-api
//        }
        restQuery.getFields().forEach((key, value) -> {
            // TODO check array
            try {
                Class<?> typeClass = User.class.getMethod("get" + StringUtils.capitalize(key)).getReturnType();
                if (value instanceof List) {
                    value = ((List<String>) value).stream().map(KapuaEid::parseCompactId).collect(Collectors.toList()).toArray(new KapuaId[0]);
                }
                AttributePredicate<?> attributePredicate = userQuery.attributePredicate(key, value);
                andPredicate.and(attributePredicate);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        userQuery.setPredicate(andPredicate);
        return userQuery;
    }

}
