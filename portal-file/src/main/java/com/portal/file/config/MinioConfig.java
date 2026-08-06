package com.portal.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${portal.minio.endpoint}")
    private String endpoint;
    @Value("${portal.minio.access-key}")
    private String accessKey;
    @Value("${portal.minio.secret-key}")
    private String secretKey;
    @Value("${portal.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        // 自动创建 bucket (若不存在); 创建失败必须明确暴露, 否则运行期上传会报 bucket 不存在
        ensureBucket(client);
        return client;
    }

    private void ensureBucket(MinioClient client) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (exists) {
                return;
            }
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            System.out.println("[MinioConfig] 已自动创建 bucket: " + bucket);
        } catch (io.minio.errors.ErrorResponseException e) {
            // 并发或重复创建可能报 bucket already exists, 视为已存在
            String code = e.errorResponse() != null ? e.errorResponse().code() : "";
            if ("BucketAlreadyExists".equals(code) || "BucketAlreadyOwnedByYou".equals(code)) {
                System.out.println("[MinioConfig] bucket 已存在: " + bucket);
                return;
            }
            throw new IllegalStateException("MinIO bucket 创建失败 (" + bucket + "): " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO bucket 创建失败 (" + bucket + "): " + e.getMessage(), e);
        }
    }
}
