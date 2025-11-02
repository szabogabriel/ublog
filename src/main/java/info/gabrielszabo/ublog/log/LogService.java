package info.gabrielszabo.ublog.log;

public interface LogService {

    public static enum LogLevel {
        INFO,
        ERROR,
        DEBUG,
        WARNING,
        TRACE
    }

    void logMessage(LogLevel level, String message);

    void logInfo(String info);

    void logError(String error);

    void logDebug(String debugInfo);

    void logWarning(String warning);

    void logTrace(String traceInfo);
    
}
