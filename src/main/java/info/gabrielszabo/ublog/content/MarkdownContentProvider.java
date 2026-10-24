package info.gabrielszabo.ublog.content;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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

    private static final java.util.regex.Pattern UNLISTED_FILE_NAME =
            java.util.regex.Pattern.compile("^[0-9a-f]{32}$");

    private final File rootFolder;
    private final Path contentRoot;
    private final MarkdownRenderingService markdownRenderingService;
    
    // Cache for quick links, categories, and entries
    private final Map<String, EntryDescriptor> quickLinksCache = new ConcurrentHashMap<>();
    private final Map<String, Category> categoriesCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, EntryDescriptor>> entriesCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, EntryDescriptor>> unlistedEntriesCache = new ConcurrentHashMap<>();
    
    // Watchdog thread
    private Thread watchdogThread;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public MarkdownContentProvider(MarkdownRenderingService markdownRenderingService) {
        this.rootFolder = new File(Config.BLOG_FOLDER.value());
        this.contentRoot = resolveContentRoot(rootFolder.toPath());
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
        unlistedEntriesCache.clear();

        File[] rootFiles = rootFolder.listFiles();
        if (rootFiles == null) return;
        
        // Scan for quick links and categories
        for (File file : rootFiles) {
            if (Files.isSymbolicLink(file.toPath())) {
                continue;
            }
            if (file.isDirectory()) {
                // Cache category
                String categoryName = file.getName();
                categoriesCache.put(categoryName, new Category(categoryName));
                
                // Cache entries in this category
                Map<String, EntryDescriptor> categoryEntries = new ConcurrentHashMap<>();
                Map<String, EntryDescriptor> unlistedEntries = new ConcurrentHashMap<>();
                File[] categoryFiles = file.listFiles();
                if (categoryFiles != null) {
                    for (File entryFile : categoryFiles) {
                        if (!entryFile.getName().endsWith(".md") || !entryFile.isFile()) {
                            continue;
                        }
                        String entryName = stripExtension(entryFile.getName());
                        EntryDescriptor descriptor = new EntryDescriptor(entryName, new Category(categoryName));
                        if (Files.isSymbolicLink(entryFile.toPath())) {
                            unlistedEntries.put(entryName, descriptor);
                        } else {
                            categoryEntries.put(entryName, descriptor);
                        }
                    }
                }
                entriesCache.put(categoryName, categoryEntries);
                unlistedEntriesCache.put(categoryName, unlistedEntries);
                
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
        return categoriesCache.values().toArray(Category[]::new);
    }

    @Override
    public EntryDescriptor[] getEntries(Category category) {
        return getEntries(category, false);
    }

    @Override
    public EntryDescriptor[] getEntries(Category category, boolean includeUnlisted) {
        Map<String, EntryDescriptor> entries = entriesCache.get(category.getName());
        if (entries == null) {
            return new EntryDescriptor[0];
        }
        java.util.List<EntryDescriptor> result = new java.util.ArrayList<>(entries.values());
        if (includeUnlisted) {
            Map<String, EntryDescriptor> unlisted = unlistedEntriesCache.get(category.getName());
            if (unlisted != null) {
                result.addAll(unlisted.values());
            }
        }
        return result.toArray(EntryDescriptor[]::new);
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
        return getEntryContentAsHttp(entry, false);
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public String getEntryContentAsHttp(EntryDescriptor entry, boolean includeUnlisted) {
        String ret = "";
        Path entryFile = resolveEntryPath(entry, includeUnlisted);
        if (entryFile != null && Files.isSymbolicLink(entryFile)) {
            entryFile = unlistedTarget(entryFile);
        }
        if (entryFile != null && Files.isRegularFile(entryFile, LinkOption.NOFOLLOW_LINKS)) {
            try {
                ret = Files.readString(entryFile);
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

    private Path unlistedTarget(Path link) {
        try {
            Path linkTarget = Files.readSymbolicLink(link);
            if (!UNLISTED_FILE_NAME.matcher(linkTarget.toString()).matches()) {
                return null;
            }
            return link.resolveSibling(linkTarget);
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveEntryPath(EntryDescriptor entry, boolean includeUnlisted) {
        if (entry == null || entry.getName() == null || entry.getName().isEmpty()
                || entry.getName().contains("/") || entry.getName().contains("\\")) {
            return null;
        }
        Path candidate = contentRoot;
        if (entry.getCategory() != null) {
            String categoryName = entry.getCategory().getName();
            if (categoryName == null || categoryName.isEmpty() || categoryName.contains("/")
                    || categoryName.contains("\\")) {
                return null;
            }
            candidate = candidate.resolve(categoryName);
        }
        candidate = candidate.resolve(entry.getName() + ".md").normalize();
        if (!candidate.startsWith(contentRoot)) {
            return null;
        }

        Path current = contentRoot;
        for (Path part : contentRoot.relativize(candidate)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current) && !(includeUnlisted && current.equals(candidate))) {
                return null;
            }
        }
        return candidate;
    }

    @Override
    public String getUnlistedFileName(Category category, String entryName) {
        if (category == null || category.getName() == null || entryName == null
                || category.getName().contains("/") || category.getName().contains("\\")
                || entryName.contains("/") || entryName.contains("\\")) {
            return null;
        }
        Path link = contentRoot.resolve(category.getName()).resolve(entryName + ".md").normalize();
        if (!link.startsWith(contentRoot) || !Files.isSymbolicLink(link)) {
            return null;
        }
        Path target = unlistedTarget(link);
        return target == null ? null : target.getFileName().toString();
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public String getUnlistedContentAsHttp(Category category, String fileName) {
        if (fileName == null || !UNLISTED_FILE_NAME.matcher(fileName).matches()) {
            return null;
        }
        Path candidate = contentRoot;
        if (category != null) {
            String categoryName = category.getName();
            if (categoryName == null || categoryName.isEmpty() || categoryName.contains("/")
                    || categoryName.contains("\\")) {
                return null;
            }
            candidate = candidate.resolve(categoryName);
        }
        candidate = candidate.resolve(fileName).normalize();
        if (!candidate.startsWith(contentRoot)) {
            return null;
        }
        Path current = contentRoot;
        for (Path part : contentRoot.relativize(candidate)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                return null;
            }
        }
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        try {
            return markdownRenderingService.renderToHtml(Files.readString(candidate));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Path resolveContentRoot(Path configuredRoot) {
        Path absoluteRoot = configuredRoot.toAbsolutePath().normalize();
        try {
            return absoluteRoot.toRealPath();
        } catch (IOException e) {
            return absoluteRoot;
        }
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
        return quickLinksCache.values().toArray(EntryDescriptor[]::new);
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
