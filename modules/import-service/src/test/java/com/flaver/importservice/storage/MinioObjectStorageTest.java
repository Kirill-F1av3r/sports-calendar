package com.flaver.importservice.storage;

import com.flaver.importservice.config.S3StorageProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioObjectStorageTest {
    @Test
    void createsMissingBucketAndUploadsFile() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenReturn(false);
        MinioObjectStorage storage = new MinioObjectStorage(client, properties());

        storage.put("imports/file.csv", new MockMultipartFile("file", "file.csv", "text/csv", "a,b".getBytes()));

        verify(client).makeBucket(any());
        verify(client).putObject(any());
    }

    @Test
    void wrapsStorageFailures() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any())).thenThrow(new IllegalStateException("offline"));

        assertThatThrownBy(() -> new MinioObjectStorage(client, properties()).put(
                "key", new MockMultipartFile("file", "x.csv", "text/csv", new byte[]{1})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to save import file")
                .hasRootCauseMessage("offline");
    }

    private S3StorageProperties properties() {
        return new S3StorageProperties("http://minio", "access", "secret", "imports", false);
    }
}
