/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.commons.service.internal;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.validation.constraints.NotNull;

import java.util.Map;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.query.predicate.MatchPredicate;

public interface MatchPredicateProcessor {

    <E> Predicate processMatchPredicate(@NotNull MatchPredicate<?> orPredicate,
                                        @NotNull Map<ParameterExpression, Object> binds,
                                        @NotNull CriteriaBuilder cb,
                                        @NotNull Root<E> entityRoot,
                                        @NotNull EntityType<E> entityType) throws KapuaException;

}
