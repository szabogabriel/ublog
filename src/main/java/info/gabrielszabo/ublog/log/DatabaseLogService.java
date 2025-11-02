package info.gabrielszabo.ublog.log;

import info.gabrielszabo.ublog.persistence.Database;

public class DatabaseLogService implements LogService {
    private Database database;

    public DatabaseLogService(Database database) {
        this.database = database;
    }

    @Override
    public void logMessage(LogLevel level, String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logMessage'");
    }

    @Override
    public void logInfo(String info) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logInfo'");
    }

    @Override
    public void logError(String error) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logError'");
    }

    @Override
    public void logDebug(String debugInfo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logDebug'");
    }

    @Override
    public void logWarning(String warning) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logWarning'");
    }

    @Override
    public void logTrace(String traceInfo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logTrace'");
    }
    
}
