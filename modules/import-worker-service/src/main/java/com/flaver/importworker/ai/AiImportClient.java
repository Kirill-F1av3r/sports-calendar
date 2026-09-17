package com.flaver.importworker.ai;

import com.flaver.dto.importing.ImportRequestedEvent;

public interface AiImportClient {
    ImportedEvents extractFromText(ImportRequestedEvent event, String extractedText);

    ImportedEvents extractFromImage(ImportRequestedEvent event, byte[] imageBytes, String sourceDescription);
}
