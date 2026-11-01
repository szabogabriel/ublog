package info.gabrielszabo.ublog.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class StringCache<T> {
    private final int ENTRY_CACHE_SIZE;
    
    private int currentCacheSize = 0;
    private final Object cacheLock = new Object();
    private final Map<T, String> contentCache = new ConcurrentHashMap<>();

    public StringCache(int size) {
        this.ENTRY_CACHE_SIZE = size;
    }

    public void clear() {
        synchronized (cacheLock) {
            contentCache.clear();
            currentCacheSize = 0;
        }
    }

    public String getCachedContent(T entryKey) {
        return contentCache.getOrDefault(entryKey, "");
    }

    public void remove(T entryKey) {
        synchronized (cacheLock) {
            String removed = contentCache.remove(entryKey);
            if (removed != null) {
                currentCacheSize -= removed.length();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void cacheContent(T entry, String content) {
        if (content == null) {
            return;
        }

        synchronized (cacheLock) {
            String previous = contentCache.remove(entry);
            
            if (previous != null) {
                currentCacheSize -= previous.length();
            }

            int contentSize = content.length();

            if (contentSize > ENTRY_CACHE_SIZE) {
                return;
            }

            while (contentSize + currentCacheSize > ENTRY_CACHE_SIZE) {
                Object[] cachedEntries = contentCache.keySet().toArray();
                if (cachedEntries.length == 0) {
                    break;
                }

                int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(cachedEntries.length);
                
                String removed = contentCache.remove((T)cachedEntries[randomIndex]);
                if (removed != null) {
                    currentCacheSize -= removed.length();
                }
            }
        }

        contentCache.put(entry, content);
        currentCacheSize += content.length();
    }
    
}
