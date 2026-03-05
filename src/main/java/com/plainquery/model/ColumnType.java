package com.plainquery.model;

public enum ColumnType {

    INTEGER {
        @Override
        public String sqlTypeName() { return "INTEGER"; }
    },
    REAL {
        @Override
        public String sqlTypeName() { return "REAL"; }
    },
    TEXT {
        @Override
        public String sqlTypeName() { return "TEXT"; }
    },
    DATE {
        @Override
        public String sqlTypeName() { return "TEXT"; }
    };

    public abstract String sqlTypeName();
}
