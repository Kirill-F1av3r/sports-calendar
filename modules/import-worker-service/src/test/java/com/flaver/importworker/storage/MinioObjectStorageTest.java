package com.flaver.importworker.storage;

import com.flaver.importworker.config.S3StorageProperties;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioObjectStorageTest {
    @Test
    void readsCompleteObject() throws Exception {
        MinioClient client = mock(MinioClient.class);
        byte[] content = {1, 2, 3};
        when(client.getObject(any())).thenReturn(new GetObjectResponse(
                new Headers.Builder().build(), "imports", null, "key", new ByteArrayInputStream(content)));

        assertThat(new MinioObjectStorage(client, properties()).get("key")).containsExactly(content);
    }

    @Test
    void wrapsReadFailures() throws Exception {
        MinioClient client = mock(MinioClient.class);
        when(client.getObject(any())).thenThrow(new IllegalStateException("offline"));

        assertThatThrownBy(() -> new MinioObjectStorage(client, properties()).get("key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to read import file")
                .hasRootCauseMessage("offline");
    }

    private S3StorageProperties properties() {
        return new S3StorageProperties("http://minio", "access", "secret", "imports", false);
    }
}
