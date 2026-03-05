package com.plainquery.service;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryResult;

import java.sql.Connection;

public interface SqliteExecutor {

    QueryResult execute(String sql, Connection connection, int maxRows)
        throws QueryException;
}
