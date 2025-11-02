package info.gabrielszabo.ublog.content;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;
import info.gabrielszabo.ublog.markdown.MarkdownRenderingService;

public class MarkdownContentProvider implements ContentProvider {

    private final File rootFolder;
    private final MarkdownRenderingService markdownRenderingService;
    
    // Cache for quick links, categories, and entries
    private final Map<String, EntryDescriptor> quickLinksCache = new ConcurrentHashMap<>();
    private final Map<String, Category> categoriesCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, EntryDescriptor>> entriesCache = new ConcurrentHashMap<>();
    
    // Watchdog thread
    private Thread watchdogThread;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public MarkdownContentProvider(MarkdownRenderingService markdownRenderingService) {
        this.rootFolder = new File(Config.TARGET_FOLDER.value());
        this.markdownRenderingService = markdownRenderingService;
        
        // Initial cache population
        refreshCache();
        
        // Start the watchdog thread
        startWatchdog();
    }
    
    /**
     * Refresh the cache by scanning the filesystem
     */
    private void refreshCache() {
        // Clear existing caches
        quickLinksCache.clear();
        categoriesCache.clear();
        entriesCache.clear();
        
        File[] rootFiles = rootFolder.listFiles();
        if (rootFiles == null) return;
        
        // Scan for quick links and categories
        for (File file : rootFiles) {
            if (file.isDirectory()) {
                // Cache category
                String categoryName = file.getName();
                categoriesCache.put(categoryName, new Category(categoryName));
                
                // Cache entries in this category
                Map<String, EntryDescriptor> categoryEntries = new ConcurrentHashMap<>();
                File[] categoryFiles = file.listFiles();
                if (categoryFiles != null) {
                    for (File entryFile : categoryFiles) {
                        if (entryFile.isFile() && entryFile.getName().endsWith(".md")) {
                            String entryName = stripExtension(entryFile.getName());
                            categoryEntries.put(entryName, 
                                new EntryDescriptor(entryName, new Category(categoryName)));
                        }
                    }
                }
                entriesCache.put(categoryName, categoryEntries);
                
            } else if (file.isFile() && file.getName().endsWith(".md")) {
                String fileName = stripExtension(file.getName());
                // Cache quick links (excluding Index and Error)
                if (!fileName.equals("Index") && !fileName.equals("Error")) {
                    quickLinksCache.put(fileName, new EntryDescriptor(fileName, null));
                }
            }
        }
    }
    
    /**
     * Start the filesystem watchdog thread
     */
    private void startWatchdog() {
        watchdogThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                // Register the root folder
                Path rootPath = rootFolder.toPath();
                rootPath.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
                
                // Register subdirectories (categories)
                File[] subdirs = rootFolder.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        subdir.toPath().register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    }
                }
                
                System.out.println("Filesystem watchdog started for: " + rootFolder.getAbsolutePath());
                
                while (running.get()) {
                    WatchKey key;
                    try {
                        key = watchService.take(); // Blocking call
                    } catch (InterruptedException e) {
                        if (!running.get()) {
                            break;
                        }
                        continue;
                    }
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        
                        System.out.println("Filesystem change detected: " + kind.name() + " - " + filename);
                        
                        // Refresh cache on any change
                        refreshCache();
                        
                        // If a new directory was created, register it for watching
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path child = ((Path) key.watchable()).resolve(filename);
                            if (Files.isDirectory(child)) {
                                try {
                                    child.register(watchService,
                                        StandardWatchEventKinds.ENTRY_CREATE,
                                        StandardWatchEventKinds.ENTRY_DELETE,
                                        StandardWatchEventKinds.ENTRY_MODIFY);
                                    System.out.println("Registered new directory for watching: " + child);
                                } catch (IOException e) {
                                    System.err.println("Failed to register directory: " + child);
                                }
                            }
                        }
                    }
                    
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                
                System.out.println("Filesystem watchdog stopped");
                
            } catch (IOException e) {
                System.err.println("Error starting filesystem watchdog: " + rootFolder.getAbsolutePath());
                System.err.println("Reason: " + e.getMessage());
                System.err.println("Watchdog disabled - cache will not auto-refresh");
                // Don't print full stack trace, just the essential info
            }
        }, "FileSystemWatchdog");
        
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }
    
    /**
     * Stop the watchdog thread (for cleanup)
     */
    public void stopWatchdog() {
        running.set(false);
        if (watchdogThread != null) {
            watchdogThread.interrupt();
        }
    }

    @Override
    public Category[] getCategories() {
        return categoriesCache.values().toArray(new Category[0]);
    }

    @Override
    public EntryDescriptor[] getEntries(Category category) {
        Map<String, EntryDescriptor> entries = entriesCache.get(category.getName());
        if (entries == null) {
            return new EntryDescriptor[0];
        }
        return entries.values().toArray(new EntryDescriptor[0]);
    }

    private String stripExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName; // No extension found
        }
        return fileName.substring(0, lastDotIndex);
    }

    @Override
    public String getEntryContentAsHttp(EntryDescriptor entry) {
        File categoryFolder = new File(rootFolder + "/" + (entry.getCategory() == null ? "" : entry.getCategory().getName()));
        File entryFile = entry.getName() == null ? null : new File(categoryFolder, entry.getName() + ".md");
        String ret = "";
        if (entryFile != null && entryFile.exists() && entryFile.isFile()) {
            try {
                ret = Files.readString(entryFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Prevent infinite recursion - don't call getError() if we're already processing an error
            if (!"Error".equals(entry.getName())) {
                return getEntryContentAsHttp(getError());
            }
            // Return a default error message if Error.md itself is missing
            ret = "# Error\n\nRequested content not found.";
        }
        return markdownRenderingService.renderToHtml(ret);
    }

    @Override
    public EntryDescriptor getIndex() {
        return new EntryDescriptor("Index", null);
    }

    @Override
    public EntryDescriptor getError() {
        return new EntryDescriptor("Error", null);
    }

    @Override
    public EntryDescriptor[] getQuickLinks() {
        return quickLinksCache.values().toArray(new EntryDescriptor[0]);
    }

    @Override
    public boolean isQuickLink(String value) {
        return quickLinksCache.containsKey(value);
    }

    @Override
    public boolean isBlogCategory(String value) {
        return categoriesCache.containsKey(value);
    }

}
