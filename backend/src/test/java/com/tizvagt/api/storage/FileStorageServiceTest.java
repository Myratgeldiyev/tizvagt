package com.tizvagt.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    @TempDir
    Path tempDir;

    private FileStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(new StorageProperties(tempDir.toString()));
    }

    @Test
    void storesPngWithRandomNameAndReturnsUrl() {
        String url = storage.storeImage(new MockMultipartFile("file", "../../evil.png", "image/png", PNG));

        assertThat(url).startsWith("/uploads/").endsWith(".png").doesNotContain("evil");
        assertThat(Files.exists(tempDir.resolve(url.substring("/uploads/".length())))).isTrue();
    }

    @Test
    void detectsJpegFromBytesNotFromFileName() {
        String url = storage.storeImage(new MockMultipartFile("file", "photo.png", "image/png", JPEG));

        assertThat(url).endsWith(".jpg");
    }

    @Test
    void rejectsNonImageEvenIfItClaimsToBeImage() {
        var fake = new MockMultipartFile("file", "virus.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> storage.storeImage(fake)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsEmptyFile() {
        var empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storage.storeImage(empty)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void deleteRemovesFileButIgnoresPathsOutsideUploadFolder() throws Exception {
        String url = storage.storeImage(new MockMultipartFile("file", "a.png", "image/png", PNG));
        Path outside = Files.writeString(tempDir.getParent().resolve("keep-me-" + System.nanoTime() + ".txt"), "x");

        storage.delete(url);
        storage.delete("/uploads/../" + outside.getFileName());

        assertThat(Files.exists(tempDir.resolve(url.substring("/uploads/".length())))).isFalse();
        assertThat(Files.exists(outside)).isTrue();
        Files.delete(outside);
    }
}
