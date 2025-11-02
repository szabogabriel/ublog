package info.gabrielszabo.ublog.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsoleLogService implements LogService {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final LogEntry POISON_PILL = new LogEntry(null, null, null);
    
    private final BlockingQueue<LogEntry> logQueue;
    private final Thread loggerThread;
    private volatile boolean running;

    public ConsoleLogService() {
        this.logQueue = new LinkedBlockingQueue<>();
        this.running = true;
        this.loggerThread = new Thread(this::processLogQueue, "ConsoleLogger-Thread");
        this.loggerThread.setDaemon(true);
        this.loggerThread.start();
    }

    private void processLogQueue() {
        while (running) {
            try {
                LogEntry entry = logQueue.take();
                if (entry == POISON_PILL) {
                    break;
                }
                String timestamp = entry.timestamp.format(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
                System.out.println("[" + timestamp + "] [" + entry.level + "] " + entry.message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void logMessage(LogLevel level, String message) {
        if (running) {
            logQueue.offer(new LogEntry(level, message, LocalDateTime.now()));
        }
    }

    @Override
    public void logInfo(String info) {
        logMessage(LogLevel.INFO, info);
    }

    @Override
    public void logError(String error) {
        logMessage(LogLevel.ERROR, error);
    }

    @Override
    public void logDebug(String debugInfo) {
        logMessage(LogLevel.DEBUG, debugInfo);
    }

    @Override
    public void logWarning(String warning) {
        logMessage(LogLevel.WARNING, warning);
    }

    @Override
    public void logTrace(String traceInfo) {
        logMessage(LogLevel.TRACE, traceInfo);
    }

    public void shutdown() {
        running = false;
        logQueue.offer(POISON_PILL);
        try {
            loggerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class LogEntry {
        final LogLevel level;
        final String message;
        final LocalDateTime timestamp;

        LogEntry(LogLevel level, String message, LocalDateTime timestamp) {
            this.level = level;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
