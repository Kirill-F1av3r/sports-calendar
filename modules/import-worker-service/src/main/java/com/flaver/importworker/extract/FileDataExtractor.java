package com.flaver.importworker.extract;

public interface FileDataExtractor {
    ExtractedFileData extract(String fileName, String contentType, byte[] bytes);
}
