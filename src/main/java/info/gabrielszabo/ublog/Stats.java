package info.gabrielszabo.ublog;

import java.util.concurrent.atomic.LongAdder;

import javax.swing.Timer;

public class Stats {

    public static LongAdder mustacheTemplateCacheHit = new LongAdder();
    public static LongAdder mustacheTemplateCacheMiss = new LongAdder();

    public static void run() {
        new Timer(60000, e -> printMustacheCacheStats()).start();
    }

    private static void printMustacheCacheStats() {
        System.out.println("Template hit: " + mustacheTemplateCacheHit + ", miss: " + mustacheTemplateCacheMiss);
    }
    
}
