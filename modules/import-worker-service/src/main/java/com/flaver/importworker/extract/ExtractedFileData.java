package com.flaver.importworker.extract;

import java.util.List;

public record ExtractedFileData(
        FileDataKind kind,
        String content,
        List<String> contentChunks,
        List<ExtractedImageChunk> imageChunks
) {
    public ExtractedFileData {
        contentChunks = contentChunks == null ? List.of() : List.copyOf(contentChunks);
        imageChunks = imageChunks == null ? List.of() : List.copyOf(imageChunks);
    }

    public static ExtractedFileData image(byte[] imageBytes, String sourceDescription) {
        return new ExtractedFileData(
                FileDataKind.IMAGE,
                null,
                List.of(),
                List.of(new ExtractedImageChunk(sourceDescription, imageBytes))
        );
    }

    public static ExtractedFileData text(String content) {
        return new ExtractedFileData(FileDataKind.TEXT, content, List.of(content), List.of());
    }

    public static ExtractedFileData textChunks(List<String> chunks) {
        String content = String.join("\n\n", chunks);
        return new ExtractedFileData(FileDataKind.TEXT, content, chunks, List.of());
    }

    public static ExtractedFileData mixed(List<String> textChunks, List<ExtractedImageChunk> imageChunks) {
        String content = String.join("\n\n", textChunks);
        FileDataKind kind = textChunks.isEmpty() ? FileDataKind.IMAGE : imageChunks.isEmpty() ? FileDataKind.TEXT : FileDataKind.MIXED;
        return new ExtractedFileData(kind, content, textChunks, imageChunks);
    }
}
