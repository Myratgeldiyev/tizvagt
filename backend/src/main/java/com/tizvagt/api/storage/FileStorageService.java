package com.tizvagt.api.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Saves uploaded images to a folder on disk and returns the public URL ("/uploads/<file>").
 */
@Service
public class FileStorageService {

    public static final String URL_PREFIX = "/uploads/";

    private final Path root;

    public FileStorageService(StorageProperties properties) {
        this.root = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory " + root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        // Never trust the file name or Content-Type sent by the client: check the real bytes.
        ImageType type = detectType(file)
                .orElseThrow(() -> new InvalidFileException("Only JPEG, PNG and WEBP images are allowed"));

        // A random name means users can't overwrite other files or use "../" tricks (path traversal).
        String fileName = UUID.randomUUID() + "." + type.extension;
        Path target = root.resolve(fileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save file", e);
        }
        return URL_PREFIX + fileName;
    }

    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path file = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!file.startsWith(root)) {
            return; // outside the upload folder, never delete
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A leftover file is not worth failing the request for.
        }
    }

    private static Optional<ImageType> detectType(MultipartFile file) {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file", e);
        }
        for (ImageType type : ImageType.values()) {
            if (type.matches(header, read)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Each image format starts with fixed "magic bytes". */
    enum ImageType {
        JPEG("jpg") {
            boolean matches(byte[] h, int n) {
                return n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            }
        },
        PNG("png") {
            boolean matches(byte[] h, int n) {
                return n >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                        && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
            }
        },
        WEBP("webp") {
            boolean matches(byte[] h, int n) {
                return n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            }
        };

        final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }

        abstract boolean matches(byte[] header, int length);
    }
}
