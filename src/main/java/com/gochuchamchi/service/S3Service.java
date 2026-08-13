package com.gochuchamchi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Value("${cloud.aws.region.static}")
    private String region;

    // 허용하는 이미지 MIME → 확장자. SVG(image/svg+xml)는 일부러 뺐다 — 스크립트를
    // 품을 수 있어 CloudFront(신뢰 도메인)에서 그대로 열리면 XSS 벡터가 된다.
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif"
    );

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024; // 10MB

    public String upload(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        if (bytes.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("이미지는 최대 10MB까지 업로드할 수 있습니다.");
        }

        // 확장자·Content-Type 을 클라이언트가 준 값에 맡기지 않는다. 선언된 타입을
        // 화이트리스트로 검증하고, 실제 바이트(매직 넘버)가 그 타입과 맞는지까지 본다.
        // 이미지인 척하는 HTML/SVG 를 신뢰 도메인(CloudFront)에 올려 XSS·피싱에
        // 쓰는 경로를 막는다.
        String declaredType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String ext = ALLOWED_IMAGE_TYPES.get(declaredType);
        if (ext == null || !matchesMagicBytes(bytes, declaredType)) {
            throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다. (JPEG/PNG/WEBP/GIF만 가능)");
        }

        // credentialsProvider를 명시하지 않으면 AWS SDK의 기본 자격증명 체인(DefaultCredentialsProvider)이
        // 적용됩니다. EC2 인스턴스에서는 인스턴스 프로파일을, EKS Pod에서는 Pod Identity를
        // 자동으로 인식하기 때문에 코드 변경 없이 두 환경 모두에서 동작합니다.
        S3Client s3 = S3Client.builder()
            .region(Region.of(region))
            .build();

        String fileName = "products/" + UUID.randomUUID() + ext;

        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(declaredType) // 화이트리스트로 검증된 값만 저장
                .cacheControl("public, max-age=31536000, immutable")
                .build(),
            RequestBody.fromBytes(bytes)
        );

        return publicUrl(fileName);
    }

    /** 파일 앞부분 매직 넘버가 선언된 이미지 타입과 일치하는지 확인한다(위조 방지). */
    private boolean matchesMagicBytes(byte[] b, String contentType) {
        if (b.length < 12) {
            return false;
        }
        switch (contentType) {
            case "image/jpeg":
                return (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "image/png":
                return (b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 'P' && (b[2] & 0xFF) == 'N' && (b[3] & 0xFF) == 'G';
            case "image/gif":
                return (b[0] & 0xFF) == 'G' && (b[1] & 0xFF) == 'I' && (b[2] & 0xFF) == 'F' && (b[3] & 0xFF) == '8';
            case "image/webp":
                return (b[0] & 0xFF) == 'R' && (b[1] & 0xFF) == 'I' && (b[2] & 0xFF) == 'F' && (b[3] & 0xFF) == 'F'
                    && (b[8] & 0xFF) == 'W' && (b[9] & 0xFF) == 'E' && (b[10] & 0xFF) == 'B' && (b[11] & 0xFF) == 'P';
            default:
                return false;
        }
    }

    /**
     * 기존 DB의 S3 직접 URL과 새 객체 키를 비공개 S3 앞의 CloudFront URL로 변환한다.
     * 로컬 환경처럼 public-base-url이 없으면 기존 S3 URL 형식을 유지한다.
     */
    public String publicUrl(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }

        String key = objectKey(storedValue);
        if (key == null) {
            return storedValue;
        }

        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        }

        return publicBaseUrl.replaceFirst("/+$", "") + "/" + key;
    }

    private String objectKey(String storedValue) {
        if (!storedValue.startsWith("http://") && !storedValue.startsWith("https://")) {
            return storedValue.replaceFirst("^/+", "");
        }

        try {
            URI uri = URI.create(storedValue);
            String host = uri.getHost();
            String regionalHost = bucket + ".s3." + region + ".amazonaws.com";
            String globalHost = bucket + ".s3.amazonaws.com";
            if (!regionalHost.equalsIgnoreCase(host) && !globalHost.equalsIgnoreCase(host)) {
                return null;
            }
            return uri.getPath().replaceFirst("^/+", "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
