package com.flaver.importworker.storage;

import com.flaver.importworker.config.S3StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
public class MinioObjectStorage implements ObjectStorage {
    private final MinioClient minioClient;
    private final S3StorageProperties properties;

    public MinioObjectStorage(MinioClient minioClient, S3StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public byte[] get(String objectKey) {
        try (InputStream input = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to read import file", ex);
        }
    }
}
