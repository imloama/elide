/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.operation.AsyncAPIOperation;
import com.yahoo.elide.async.operation.GraphQLAsyncQueryOperation;
import com.yahoo.elide.async.operation.JSONAPIAsyncQueryOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.thread.AsyncAPICallable;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.graphql.QueryRunner;

import java.util.Optional;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class AsyncQueryHook extends AsyncAPIHook<AsyncQuery> {

    public AsyncQueryHook (AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds) {
        super(asyncExecutorService, maxAsyncAfterSeconds);
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, AsyncQuery query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        AsyncAPICallable queryWorker = new AsyncAPICallable(query, getOperation(query, requestScope),
                (com.yahoo.elide.core.RequestScope) requestScope);
        executeHook(operation, phase, query, requestScope, queryWorker);
    }

    @Override
    public void validateOptions(AsyncAPI query, RequestScope requestScope) {
        super.validateOptions(query, requestScope);

        if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            QueryRunner runner = getAsyncExecutorService().getRunners().get(requestScope.getApiVersion());
            if (runner == null) {
                throw new InvalidOperationException("Invalid API Version");
            }
        }
    }

    @Override
    public AsyncAPIOperation<?> getOperation(AsyncAPI query, RequestScope requestScope) {
        AsyncAPIOperation<?> operation = null;
        if (query.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            operation = new JSONAPIAsyncQueryOperation(getAsyncExecutorService());
        } else {
            operation = new GraphQLAsyncQueryOperation(getAsyncExecutorService());
        }
        return operation;
    }
}
