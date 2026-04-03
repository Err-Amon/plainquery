package com.plainquery.service;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryHistoryEntry;

import java.util.List;

public interface HistoryService {

    void record(String naturalLanguage, String generatedSql) throws QueryException;

    List<QueryHistoryEntry> search(String term) throws QueryException;

    List<QueryHistoryEntry> getRecent(int limit) throws QueryException;

    List<QueryHistoryEntry> getStarred() throws QueryException;

    List<QueryHistoryEntry> getAll() throws QueryException;

    QueryHistoryEntry getById(long id) throws QueryException;

    void toggleStar(long id) throws QueryException;

    void deleteById(long id) throws QueryException;

    void deleteAll() throws QueryException;
}
