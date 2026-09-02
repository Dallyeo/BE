package com.ppip.dallyeo.common.storage;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 이미지 파일 로컬 디스크 저장 (러닝 기록 이미지).
 *
 * <p>파일명은 서버가 UUID로 새로 짓는다 — 클라이언트가 보낸 이름은 쓰지 않으므로 경로 순회(../)와
 * 이름 충돌이 원천 차단된다. 확장자도 화이트리스트로 고정한다(실행 가능 파일 업로드 차단).
 *
 * <p>매직 바이트 검사까지는 하지 않는다. 저장 파일은 정적 리소스로만 서빙되고 실행되지 않으며,
 * 확장자·Content-Type 화이트리스트로 위험 타입을 걸러낸다.
 */
@Component
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    /** 허용 Content-Type → 저장 확장자. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic",
            "image/heif", "heif");

    private final StorageProperties properties;
    private final Path root;

    public ImageStorage(StorageProperties properties) {
        this.properties = properties;
        this.root = Paths.get(properties.dir()).toAbsolutePath().normalize();
    }

    /**
     * 이미지를 {@code {dir}/{category}/} 아래에 저장하고 공개 URL 경로를 반환한다.
     *
     * @param category 하위 디렉터리(예: "runs")
     * @return 공개 URL 경로 (예: {@code /uploads/runs/{uuid}.jpg})
     */
    public String store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "이미지 파일이 비어 있습니다.");
        }
        if (file.getSize() > properties.maxBytes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "이미지 크기는 " + (properties.maxBytes() / (1024 * 1024)) + "MB를 넘을 수 없습니다.");
        }
        String extension = resolveExtension(file.getContentType());
        String filename = UUID.randomUUID() + "." + extension;

        Path directory = root.resolve(category).normalize();
        if (!directory.startsWith(root)) {   // category가 코드 상수라 도달 불가 — 방어적 확인
            throw new BusinessException(ErrorCode.BAD_REQUEST, "잘못된 저장 경로입니다.");
        }
        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store image (category={}, filename={}): {}", category, filename, e.toString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "이미지 저장에 실패했습니다.");
        }
        return properties.publicPath() + "/" + category + "/" + filename;
    }

    /**
     * 이전에 저장한 이미지를 삭제한다(교체 시 고아 파일 방지). 실패해도 예외를 던지지 않는다 —
     * 파일 정리 실패가 본 요청(기록 저장)을 깨뜨릴 이유는 없다.
     */
    public void deleteQuietly(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(properties.publicPath() + "/")) {
            return;
        }
        String relative = publicUrl.substring(properties.publicPath().length() + 1);
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete old image {}: {}", publicUrl, e.toString());
        }
    }

    private String resolveExtension(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        int paramIndex = normalized.indexOf(';');   // "image/jpeg; charset=..." 형태 방어
        if (paramIndex > -1) {
            normalized = normalized.substring(0, paramIndex).trim();
        }
        String extension = ALLOWED_TYPES.get(normalized);
        if (extension == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "지원하지 않는 이미지 형식입니다: " + (contentType == null ? "(없음)" : contentType));
        }
        return extension;
    }
}
