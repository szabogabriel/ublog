package info.gabrielszabo.ublog.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.server.http.MultipartForm.UploadedFile;

/** Manages the images stored under [IMAGE_FOLDER]/[TOPIC]/[ENTRY]. */
public class ImageService {

    // SVG is excluded on purpose: served from this origin it could carry scripts.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Pattern FILE_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$");
    private static final Pattern ENTRY_NAME = Pattern.compile("^[a-zA-Z0-9_ -]+$");

    private final ContentService contentService;

    public ImageService(ContentService contentService) {
        this.contentService = contentService;
    }

    /** Returns the image names of an entry, or null if topic or entry name is invalid. */
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
                        .filter(ImageService::isValidFileName)
                        .sorted()
                        .forEach(names::add);
            } catch (IOException e) {
                return null;
            }
        }
        return names;
    }

    /** Returns an error message, or null on success. */
    public String save(String topic, String entry, UploadedFile file) {
        Path folder = entryFolder(topic, entry);
        if (folder == null) {
            return "Unknown topic or invalid entry name.";
        }
        if (file == null || file.fileName().isEmpty() || file.content().length == 0) {
            return "Please choose an image file.";
        }
        if (!isValidFileName(file.fileName())) {
            return "Invalid file name. Use letters, digits, '.', '-' or '_' and a supported image extension.";
        }
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(file.fileName());
            if (Files.isSymbolicLink(target)) {
                return "Failed to store the image.";
            }
            Files.write(target, file.content());
            return null;
        } catch (IOException e) {
            return "Failed to store the image.";
        }
    }

    /** Returns an error message, or null on success. */
    public String delete(String topic, String entry, String fileName) {
        Path folder = entryFolder(topic, entry);
        if (folder == null || !isValidFileName(fileName)) {
            return "Unknown image.";
        }
        Path target = folder.resolve(fileName);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            return "Unknown image.";
        }
        try {
            Files.delete(target);
            return null;
        } catch (IOException e) {
            return "Failed to delete the image.";
        }
    }

    public static String normalizeEntry(String entry) {
        return entry == null ? null : entry.trim().replaceAll(" +", "_");
    }

    private static boolean isValidFileName(String name) {
        if (name == null || !FILE_NAME.matcher(name).matches() || name.contains("..")) {
            return false;
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 && ALLOWED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private Path entryFolder(String topic, String entry) {
        String normalized = normalizeEntry(entry);
        if (!contentService.topicExists(topic) || normalized == null
                || !ENTRY_NAME.matcher(normalized).matches()) {
            return null;
        }
        return Path.of(Config.IMAGE_FOLDER.value()).resolve(topic).resolve(normalized);
    }
}
