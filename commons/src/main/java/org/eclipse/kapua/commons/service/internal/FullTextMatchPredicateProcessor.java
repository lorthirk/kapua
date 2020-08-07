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
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import org.eclipse.kapua.model.query.predicate.MatchPredicate;

import com.google.common.collect.Lists;
import org.eclipse.persistence.jpa.JpaCriteriaBuilder;

public class FullTextMatchPredicateProcessor implements MatchPredicateProcessor {

    @Override
    public <E> Predicate processMatchPredicate(@NotNull MatchPredicate<?> matchPredicate,
                                               @NotNull Map<ParameterExpression, Object> binds,
                                               @NotNull CriteriaBuilder cb,
                                               @NotNull Root<E> entityRoot,
                                               @NotNull EntityType<E> entityType) {
        JpaCriteriaBuilder jpaCriteriaBuilder = (JpaCriteriaBuilder)cb;

        List<String> args = Lists.newArrayList(matchPredicate.getMatchTerm().toString());

        Expression<Number> expr = jpaCriteriaBuilder.fromExpression(
                jpaCriteriaBuilder.toExpression(
                        cb.function("", String.class, matchPredicate.getAttributeNames().stream().map(entityRoot::get).toArray(Path[]::new))
                )
                .sql("MATCH ? AGAINST (?)", args)
        );

        return cb.gt(expr, 0);
    }

}
