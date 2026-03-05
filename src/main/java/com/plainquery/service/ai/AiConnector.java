package com.plainquery.service.ai;

import com.plainquery.exception.AiConnectorException;
import com.plainquery.exception.SqlExtractionException;
import com.plainquery.model.TableSchema;

import java.util.List;

public interface AiConnector {

    String translateToSql(String question, List<TableSchema> schemas)
        throws AiConnectorException, SqlExtractionException;
}
