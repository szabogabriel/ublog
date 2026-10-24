package info.gabrielszabo.ublog.service.log;

public interface LogService {

    public static enum LogLevel {
        ERROR(100),
        WARNING(200),
        INFO(300),
        TRACE(400),
        DEBUG(500);

        private final int level;

        private LogLevel(int level) {
            this.level = level;
        }

        public static LogLevel fromName(String name) {
            for (LogLevel it : LogLevel.values()) {
                if (it.name().equalsIgnoreCase(name)) {
                    return it;
                }
            }
            return null; // or throw an IllegalArgumentException if preferred
        }

        public boolean matchesTargetLogLevel(LogLevel targetLevel) {
            return this.level <= targetLevel.level;
        }
    }

    void logMessage(LogLevel level, String message);

    void logInfo(String info);

    void logError(String error);

    void logDebug(String debugInfo);

    void logWarning(String warning);

    void logTrace(String traceInfo);
    
}
