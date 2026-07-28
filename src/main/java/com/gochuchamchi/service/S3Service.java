package com.gochuchamchi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public String upload(MultipartFile file) throws Exception {
        // credentialsProvider를 명시하지 않으면 AWS SDK의 기본 자격증명 체인(DefaultCredentialsProvider)이
        // 적용됩니다. EC2 인스턴스에서는 인스턴스 프로파일을, EKS Pod에서는 Pod Identity를
        // 자동으로 인식하기 때문에 코드 변경 없이 두 환경 모두에서 동작합니다.
        S3Client s3 = S3Client.builder()
            .region(Region.of(region))
            .build();

        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "products/" + UUID.randomUUID() + ext;

        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromBytes(file.getBytes())
        );

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;
    }
}
