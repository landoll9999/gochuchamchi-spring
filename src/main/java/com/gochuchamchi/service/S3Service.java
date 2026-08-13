package com.gochuchamchi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class S3Service {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Value("${cloud.aws.region.static}")
    private String region;

    public String upload(MultipartFile file) throws IOException {
        SanitizedImage image = validateAndReencode(file);
        // credentialsProvider를 명시하지 않으면 AWS SDK의 기본 자격증명 체인(DefaultCredentialsProvider)이
        // 적용됩니다. EC2 인스턴스에서는 인스턴스 프로파일을, EKS Pod에서는 Pod Identity를
        // 자동으로 인식하기 때문에 코드 변경 없이 두 환경 모두에서 동작합니다.
        S3Client s3 = S3Client.builder()
            .region(Region.of(region))
            .build();

        String fileName = "products/" + UUID.randomUUID() + image.extension();

        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                // 브라우저가 보낸 Content-Type이나 파일명은 신뢰하지 않는다.
                .contentType(image.contentType())
                .cacheControl("public, max-age=31536000, immutable")
                .build(),
            RequestBody.fromBytes(image.bytes())
        );

        return publicUrl(fileName);
    }

    /**
     * 상품 이미지는 서버가 실제 JPEG/PNG로 판별하고, 메타데이터를 제거한 새 이미지로만 저장한다.
     * SVG와 같은 스크립트 가능 형식, 확장자 위장 파일, 과도한 해상도의 이미지 폭탄은 거부한다.
     */
    private SanitizedImage validateAndReencode(MultipartFile file) throws IOException {
        if (file.getSize() <= 0 || file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("이미지는 10MB 이하만 업로드할 수 있습니다.");
        }

        byte[] source = file.getBytes();
        ImageFormat format = detectFormat(source);
        if (format == null) {
            throw new IllegalArgumentException("JPEG 또는 PNG 이미지만 업로드할 수 있습니다.");
        }

        BufferedImage decoded;
        try {
            decoded = readAndValidateImage(source, format);
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.", e);
        }
        return reencode(decoded);
    }

    private BufferedImage readAndValidateImage(byte[] source, ImageFormat expectedFormat) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (input == null) {
                throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String actualFormat = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!expectedFormat.readerFormat().equals(actualFormat)) {
                    throw new IllegalArgumentException("파일 형식이 올바르지 않습니다.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("이미지 해상도는 최대 4096px, 1600만 픽셀까지 가능합니다.");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private SanitizedImage reencode(BufferedImage source) throws IOException {
        boolean hasAlpha = source.getColorModel().hasAlpha();
        String format = hasAlpha ? "png" : "jpeg";
        int imageType = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage sanitized = new BufferedImage(source.getWidth(), source.getHeight(), imageType);

        Graphics2D graphics = sanitized.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(sanitized, format, output)) {
                throw new IOException("이미지 변환기를 찾을 수 없습니다.");
            }
            return new SanitizedImage(output.toByteArray(), "." + format, "image/" + format);
        }
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return ImageFormat.PNG;
        }
        return null;
    }

    private enum ImageFormat {
        JPEG("jpeg"),
        PNG("png");

        private final String readerFormat;

        ImageFormat(String readerFormat) {
            this.readerFormat = readerFormat;
        }

        String readerFormat() {
            return readerFormat;
        }
    }

    private record SanitizedImage(byte[] bytes, String extension, String contentType) {
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
