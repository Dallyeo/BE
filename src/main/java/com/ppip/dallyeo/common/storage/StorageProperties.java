package com.ppip.dallyeo.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 업로드 파일 저장 설정.
 *
 * @param dir        저장 루트 디렉터리(로컬 파일시스템). 배포마다 갈아엎히지 않도록 배포 산출물 바깥 경로 권장.
 * @param publicPath 저장 파일을 공개 서빙할 URL prefix. 응답 imageUrl 생성/정적 리소스 핸들러 양쪽에서 사용.
 * @param maxBytes   파일 1개 허용 최대 크기(바이트).
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String dir, String publicPath, long maxBytes) {
}
