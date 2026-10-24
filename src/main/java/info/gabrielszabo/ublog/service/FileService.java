package info.gabrielszabo.ublog.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import info.gabrielszabo.ublog.config.Config;

/** Manages the files stored under [FILE_FOLDER]/[TOPIC]/[ENTRY]. Any file type is accepted. */
public class FileService {

    private static final Pattern FILE_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$");
    private static final Pattern ENTRY_NAME = Pattern.compile("^[a-zA-Z0-9_ -]+$");

    private final ContentService contentService;

    public FileService(ContentService contentService) {
        this.contentService = contentService;
    }

    /** Returns the file names of an entry, or null if topic or entry name is invalid. */
    public List<String> list(String topic, String entry) {
        Path folder = entryFolder(topic, entry);
        if (folder == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(file -> Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                        .map(file -> file.getFileName().toString())
                        .filter(FileService::isValidFileName)
                        .sorted()
                        .forEach(names::add);
            } catch (IOException e) {
                return null;
            }
        }
        return names;
    }

    /**
     * Creates an empty temp file next to the final location for an incoming upload.
     * Throws IllegalArgumentException with a user-facing message if the target is not acceptable.
     */
    public Path createUploadFile(String topic, String entry, String fileName) throws IOException {
        Path folder = entryFolder(topic, entry);
        if (folder == null) {
            throw new IllegalArgumentException("Unknown topic or invalid entry name.");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Please choose a file.");
        }
        if (!isValidFileName(fileName)) {
            throw new IllegalArgumentException(
                    "Invalid file name. Use letters, digits, '.', '-' or '_' (max. 100 characters).");
        }
        Files.createDirectories(folder);
        return Files.createTempFile(folder, ".upload-", ".tmp");
    }

    /** Moves a completed upload into place, replacing a file of the same name. Returns an error message, or null. */
    public String commitUpload(String topic, String entry, String fileName, Path uploadFile) {
        Path folder = entryFolder(topic, entry);
        try {
            if (folder == null || Files.size(uploadFile) == 0) {
                return "Please choose a file.";
            }
            Path target = folder.resolve(fileName);
            if (Files.isSymbolicLink(target)) {
                return "Failed to store the file.";
            }
            try {
                Files.move(uploadFile, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(uploadFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return null;
        } catch (IOException e) {
            return "Failed to store the file.";
        }
    }

    public void discardUpload(Path uploadFile) {
        try {
            Files.deleteIfExists(uploadFile);
        } catch (IOException ignored) {
            // Best effort; a stray hidden temp file is not listed or served.
        }
    }

    /** Returns an error message, or null on success. */
    public String delete(String topic, String entry, String fileName) {
        Path folder = entryFolder(topic, entry);
        if (folder == null || !isValidFileName(fileName)) {
            return "Unknown file.";
        }
        Path target = folder.resolve(fileName);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            return "Unknown file.";
        }
        try {
            Files.delete(target);
            return null;
        } catch (IOException e) {
            return "Failed to delete the file.";
        }
    }

    public static String normalizeEntry(String entry) {
        return ImageService.normalizeEntry(entry);
    }

    private static boolean isValidFileName(String name) {
        return name != null && FILE_NAME.matcher(name).matches() && !name.contains("..");
    }

    private Path entryFolder(String topic, String entry) {
        String normalized = normalizeEntry(entry);
        if (!contentService.topicExists(topic) || normalized == null
                || !ENTRY_NAME.matcher(normalized).matches()) {
            return null;
        }
        return Path.of(Config.FILE_FOLDER.value()).resolve(topic).resolve(normalized);
    }
}
