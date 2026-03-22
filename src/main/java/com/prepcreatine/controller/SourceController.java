package com.prepcreatine.controller;

import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.SourceResponse;
import com.prepcreatine.service.SourceService;
import com.prepcreatine.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Source management endpoints per BSDD §6.
 *
 * GET    /api/sources
 * GET    /api/sources/{id}
 * POST   /api/sources/import-url
 * POST   /api/sources/import-text
 * POST   /api/sources/import-pdf
 * DELETE /api/sources/{id}
 */
@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping
    public ResponseEntity<List<SourceResponse>> list() {
        return ResponseEntity.ok(sourceService.listForUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SourceResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(sourceService.getById(id, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/import-url")
    public ResponseEntity<SourceResponse> importUrl(@Valid @RequestBody ImportUrlRequest req) {
        SourceResponse response = sourceService.importUrl(req, SecurityUtil.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/import-text")
    public ResponseEntity<SourceResponse> importText(@Valid @RequestBody ImportTextRequest req) {
        SourceResponse response = sourceService.importText(req, SecurityUtil.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/import-pdf")
    public ResponseEntity<SourceResponse> importPdf(@RequestParam("file") MultipartFile file) throws IOException {
        SourceResponse response = sourceService.importPdf(file, SecurityUtil.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        sourceService.deleteSource(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Source deleted."));
    }
}
