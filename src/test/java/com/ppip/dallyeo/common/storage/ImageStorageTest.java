package com.ppip.dallyeo.common.storage;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageTest {

    @TempDir
    Path tempDir;

    private ImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ImageStorage(new StorageProperties(tempDir.toString(), "/uploads", 10 * 1024 * 1024));
    }

    private MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("image", "photo.jpg", contentType, content);
    }

    @Test
    void storesFileAndReturnsPublicPath() throws Exception {
        String url = storage.store(file("image/jpeg", "bytes".getBytes()), "runs");

        assertThat(url).startsWith("/uploads/runs/").endsWith(".jpg");
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.readString(saved)).isEqualTo("bytes");
    }

    @Test
    void generatesUniqueNameIgnoringClientFilename() {
        // 클라이언트가 보낸 이름(경로 순회 시도 포함)은 쓰지 않고 UUID로 새로 짓는다.
        MockMultipartFile evil = new MockMultipartFile(
                "image", "../../etc/passwd.png", "image/png", "x".getBytes());

        String url = storage.store(evil, "runs");

        assertThat(url).doesNotContain("..").doesNotContain("passwd");
        assertThat(tempDir.resolve(url.substring("/uploads/".length()))).exists();
    }

    @Test
    void rejectsNonImageContentType() {
        assertThatThrownBy(() -> storage.store(file("application/pdf", "x".getBytes()), "runs"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void acceptsContentTypeWithParameters() {
        assertThat(storage.store(file("image/jpeg; charset=binary", "x".getBytes()), "runs"))
                .endsWith(".jpg");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> storage.store(file("image/png", new byte[0]), "runs"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsOversizedFile() {
        ImageStorage tiny = new ImageStorage(new StorageProperties(tempDir.toString(), "/uploads", 4));

        assertThatThrownBy(() -> tiny.store(file("image/png", "12345".getBytes()), "runs"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void deleteQuietlyRemovesStoredFile() {
        String url = storage.store(file("image/png", "x".getBytes()), "runs");
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));

        storage.deleteQuietly(url);

        assertThat(saved).doesNotExist();
    }

    @Test
    void deleteQuietlyIgnoresForeignOrTraversalPaths() {
        // 저장소가 만든 URL이 아니면 무시 — 임의 경로 삭제로 번지지 않는다.
        storage.deleteQuietly("https://example.com/a.png");
        storage.deleteQuietly("/uploads/../../etc/passwd");
        storage.deleteQuietly(null);

        assertThat(tempDir).exists();
    }
}
