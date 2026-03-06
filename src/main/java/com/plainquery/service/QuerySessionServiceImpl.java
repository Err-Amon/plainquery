package com.plainquery.service;

import com.plainquery.model.QuerySession;
import com.plainquery.model.QueryHistoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class QuerySessionServiceImpl implements QuerySessionService {
    
    private static final Logger LOG = Logger.getLogger(QuerySessionServiceImpl.class.getName());
    private static final String SESSIONS_DIR = System.getProperty("user.home") + File.separator + ".plainquery" + File.separator + "sessions";
    private static final String ACTIVE_SESSION_FILE = "active_session.txt";
    private static final ObjectMapper MAPPER;
    
    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
    }
    
    private final ConcurrentMap<String, QuerySession> sessions;
    private String activeSessionId;
    
    public QuerySessionServiceImpl() {
        this.sessions = new ConcurrentHashMap<>();
        this.activeSessionId = null;
        initializeStorage();
        loadAllSessionsInternal();
    }
    
    private void initializeStorage() {
        File sessionsDir = new File(SESSIONS_DIR);
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
            LOG.log(Level.INFO, "Created sessions directory: " + sessionsDir.getAbsolutePath());
        }
    }
    
    private void loadAllSessionsInternal() {
        try {
            File sessionsDir = new File(SESSIONS_DIR);
            if (!sessionsDir.exists() || !sessionsDir.isDirectory()) {
                return;
            }
            
            File[] files = sessionsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null) {
                return;
            }
            
            for (File file : files) {
                try {
                    QuerySession session = MAPPER.readValue(file, QuerySession.class);
                    sessions.put(session.getId(), session);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to load session from file: " + file.getName(), e);
                }
            }
            
            loadActiveSession();
            
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load query sessions", e);
        }
    }
    
    private void loadActiveSession() {
        try {
            File activeFile = new File(SESSIONS_DIR, ACTIVE_SESSION_FILE);
            if (activeFile.exists()) {
                String activeId = new String(Files.readAllBytes(activeFile.toPath())).trim();
                if (!activeId.isEmpty() && sessions.containsKey(activeId)) {
                    activeSessionId = activeId;
                    LOG.log(Level.FINE, "Active session loaded: " + activeId);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load active session", e);
        }
    }
    
    private void saveActiveSession() {
        try {
            File activeFile = new File(SESSIONS_DIR, ACTIVE_SESSION_FILE);
            if (activeSessionId != null) {
                Files.write(activeFile.toPath(), activeSessionId.getBytes(), 
                          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else if (activeFile.exists()) {
                activeFile.delete();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save active session", e);
        }
    }
    
    @Override
    public QuerySession createSession(String sessionName) {
        String sessionId = UUID.randomUUID().toString();
        QuerySession session = new QuerySession(sessionId, sessionName);
        session.setHistoryEntries(new ArrayList<>());
        
        sessions.put(sessionId, session);
        saveSessionInternal(session);
        
        if (activeSessionId == null) {
            activeSessionId = sessionId;
            saveActiveSession();
        }
        
        LOG.log(Level.INFO, "Session created: " + session.getName() + " (" + sessionId + ")");
        return session;
    }
    
    @Override
    public List<QuerySession> loadAllSessions() {
        return Collections.unmodifiableList(new ArrayList<>(sessions.values()));
    }
    
    @Override
    public Optional<QuerySession> loadSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
    
    @Override
    public void saveSession(QuerySession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        sessions.put(session.getId(), session);
        saveSessionInternal(session);
    }
    
    private void saveSessionInternal(QuerySession session) {
        try {
            File sessionFile = new File(SESSIONS_DIR, session.getId() + ".json");
            MAPPER.writeValue(sessionFile, session);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save session: " + session.getName(), e);
        }
    }
    
    @Override
    public void deleteSession(String sessionId) {
        QuerySession session = sessions.remove(sessionId);
        if (session != null) {
            File sessionFile = new File(SESSIONS_DIR, sessionId + ".json");
            if (sessionFile.exists()) {
                sessionFile.delete();
            }
            
            if (sessionId.equals(activeSessionId)) {
                activeSessionId = null;
                saveActiveSession();
            }
            
            LOG.log(Level.INFO, "Session deleted: " + session.getName());
        }
    }
    
    @Override
    public void renameSession(String sessionId, String newName) {
        QuerySession session = sessions.get(sessionId);
        if (session != null) {
            session.setName(newName);
            saveSessionInternal(session);
            LOG.log(Level.INFO, "Session renamed: " + newName);
        }
    }
    
    @Override
    public void addEntryToSession(String sessionId, QueryHistoryEntry entry) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        
        QuerySession session = sessions.get(sessionId);
        if (session != null) {
            session.addHistoryEntry(entry);
            saveSessionInternal(session);
            LOG.log(Level.FINE, "Entry added to session: " + session.getName());
        }
    }
    
    @Override
    public void removeEntryFromSession(String sessionId, long entryId) {
        QuerySession session = sessions.get(sessionId);
        if (session != null) {
            session.removeHistoryEntry(entryId);
            saveSessionInternal(session);
            LOG.log(Level.FINE, "Entry removed from session: " + session.getName());
        }
    }
    
    @Override
    public List<QueryHistoryEntry> getSessionHistory(String sessionId) {
        QuerySession session = sessions.get(sessionId);
        if (session != null && session.getHistoryEntries() != null) {
            return Collections.unmodifiableList(session.getHistoryEntries());
        }
        return Collections.emptyList();
    }
    
    @Override
    public Optional<QuerySession> getActiveSession() {
        if (activeSessionId != null) {
            return Optional.ofNullable(sessions.get(activeSessionId));
        }
        return Optional.empty();
    }
    
    @Override
    public void setActiveSession(String sessionId) {
        if (sessions.containsKey(sessionId)) {
            activeSessionId = sessionId;
            saveActiveSession();
            LOG.log(Level.INFO, "Active session set: " + sessions.get(sessionId).getName());
        }
    }
    
    @Override
    public void clearActiveSession() {
        activeSessionId = null;
        saveActiveSession();
    }
    
    @Override
    public void deleteAllSessions() {
        sessions.clear();
        activeSessionId = null;
        
        File sessionsDir = new File(SESSIONS_DIR);
        if (sessionsDir.exists()) {
            File[] files = sessionsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        
        saveActiveSession();
        LOG.log(Level.INFO, "All sessions deleted");
    }
    
    @Override
    public int getSessionCount() {
        return sessions.size();
    }
    
    @Override
    public boolean isEmpty() {
        return sessions.isEmpty();
    }
    
    public List<QueryHistoryEntry> getAllHistoryEntries() {
        return sessions.values().stream()
                .filter(session -> session.getHistoryEntries() != null)
                .flatMap(session -> session.getHistoryEntries().stream())
                .collect(Collectors.toList());
    }
}
