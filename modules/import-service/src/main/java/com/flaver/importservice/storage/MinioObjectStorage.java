package com.flaver.importservice.storage;

import com.flaver.importservice.config.S3StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MinioObjectStorage implements ObjectStorage {
    private final MinioClient minioClient;
    private final S3StorageProperties properties;

    public MinioObjectStorage(MinioClient minioClient, S3StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public void put(String objectKey, MultipartFile file) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .contentType(file.getContentType())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to save import file", ex);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }
}
