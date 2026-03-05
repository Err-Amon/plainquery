package com.plainquery.service;

import com.plainquery.exception.CsvLoadException;
import com.plainquery.exception.InsufficientMemoryException;
import com.plainquery.model.CsvLoadResult;

import java.io.File;
import java.sql.Connection;

public interface CsvLoaderService {

    CsvLoadResult load(File csvFile, Connection connection)
        throws CsvLoadException, InsufficientMemoryException;
}
