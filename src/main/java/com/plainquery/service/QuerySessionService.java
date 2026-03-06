package com.plainquery.service;

import com.plainquery.model.QuerySession;
import com.plainquery.model.QueryHistoryEntry;

import java.util.List;
import java.util.Optional;

public interface QuerySessionService {

    QuerySession createSession(String sessionName);

    List<QuerySession> loadAllSessions();

    Optional<QuerySession> loadSession(String sessionId);
    

    void saveSession(QuerySession session);
    

    void deleteSession(String sessionId);
    

    void renameSession(String sessionId, String newName);
    
 
    void addEntryToSession(String sessionId, QueryHistoryEntry entry);
    
  
    void removeEntryFromSession(String sessionId, long entryId);
    

    List<QueryHistoryEntry> getSessionHistory(String sessionId);
    
    Optional<QuerySession> getActiveSession();
    
    void setActiveSession(String sessionId);
    
    
    void clearActiveSession();
    
  
    void deleteAllSessions();
    
    int getSessionCount();
    boolean isEmpty();
}
