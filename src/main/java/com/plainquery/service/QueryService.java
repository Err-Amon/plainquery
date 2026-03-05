package com.plainquery.service;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryResult;

public interface QueryService {

    QueryResult executeNaturalLanguage(String question) throws QueryException;

    QueryResult executeSql(String sql) throws QueryException;
}
