package com.flaver.importservice.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorage {
    void put(String objectKey, MultipartFile file);
}
