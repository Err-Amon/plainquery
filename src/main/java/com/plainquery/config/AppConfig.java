package com.plainquery.config;

import java.util.prefs.Preferences;

public final class AppConfig {

    private static final String KEY_AI_PROVIDER       = "ai.provider";
    private static final String KEY_API_KEY           = "ai.api.key";
    private static final String KEY_DB_MODE           = "db.mode";
    private static final String KEY_PREVIEW_ROW_LIMIT = "query.preview.row.limit";
    private static final String KEY_HISTORY_DB_PATH   = "history.db.path";

    private static final int    DEFAULT_PREVIEW_ROW_LIMIT = 10000;
    private static final String DEFAULT_HISTORY_DB_PATH   = "";

    public enum DbMode { MEMORY, FILE }

    private final Preferences prefs;

    public AppConfig() {
        this.prefs = Preferences.userNodeForPackage(AppConfig.class);
    }

    public AiProvider getAiProvider() {
        String value = prefs.get(KEY_AI_PROVIDER, AiProvider.GROQ.name());
        try { return AiProvider.valueOf(value); }
        catch (IllegalArgumentException e) { return AiProvider.GROQ; }
    }

    public void setAiProvider(AiProvider provider) {
        if (provider == null) throw new IllegalArgumentException("AiProvider must not be null");
        prefs.put(KEY_AI_PROVIDER, provider.name());
    }

    public String getApiKey() { return prefs.get(KEY_API_KEY, ""); }

    public void setApiKey(String apiKey) {
        if (apiKey == null) throw new IllegalArgumentException("API key must not be null");
        prefs.put(KEY_API_KEY, apiKey.trim());
    }

    public DbMode getDbMode() {
        String value = prefs.get(KEY_DB_MODE, DbMode.MEMORY.name());
        try { return DbMode.valueOf(value); }
        catch (IllegalArgumentException e) { return DbMode.MEMORY; }
    }

    public void setDbMode(DbMode mode) {
        if (mode == null) throw new IllegalArgumentException("DbMode must not be null");
        prefs.put(KEY_DB_MODE, mode.name());
    }

    public int getPreviewRowLimit() {
        return prefs.getInt(KEY_PREVIEW_ROW_LIMIT, DEFAULT_PREVIEW_ROW_LIMIT);
    }

    public void setPreviewRowLimit(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Preview row limit must be at least 1");
        prefs.putInt(KEY_PREVIEW_ROW_LIMIT, limit);
    }

    public String getHistoryDbPath() {
        return prefs.get(KEY_HISTORY_DB_PATH, DEFAULT_HISTORY_DB_PATH);
    }

    public void setHistoryDbPath(String path) {
        if (path == null) throw new IllegalArgumentException("History DB path must not be null");
        prefs.put(KEY_HISTORY_DB_PATH, path.trim());
    }

    public void flush() {
        try { prefs.flush(); }
        catch (java.util.prefs.BackingStoreException e) {
            java.util.logging.Logger.getLogger(AppConfig.class.getName())
                .warning("Failed to flush preferences: " + e.getMessage());
        }
    }
}
