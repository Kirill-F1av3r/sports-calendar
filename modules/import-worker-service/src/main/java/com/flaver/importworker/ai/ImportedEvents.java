package com.flaver.importworker.ai;

import com.flaver.importworker.dto.CompleteImportRequest;

import java.util.List;

public record ImportedEvents(List<CompleteImportRequest.ImportedDraftEventRequest> events) {
}
