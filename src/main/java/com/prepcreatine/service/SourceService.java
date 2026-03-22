package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.*;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import com.prepcreatine.util.SanitizationUtil;
import com.prepcreatine.util.TextChunkingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Source ingestion & RAG pipeline per BSDD §6.
 * Handles URL, text, and PDF ingestion.
 * Study guide generation and chunking/embedding run @Async to prevent timeouts.
 * Source status transitions: PENDING → PROCESSING → READY | FAILED
 */
@Service
@Transactional
public class SourceService {

    private static final Logger log = LoggerFactory.getLogger(SourceService.class);
    private static final long   MAX_TEXT_CHARS = 500_000;

    private final SourceRepository         sourceRepo;
    private final SourceChunkRepository    chunkRepo;
    private final GeminiService            gemini;
    private final YouTubeTranscriptService youtubeService;

    public SourceService(SourceRepository sourceRepo,
                         SourceChunkRepository chunkRepo,
                         GeminiService gemini,
                         YouTubeTranscriptService youtubeService) {
        this.sourceRepo      = sourceRepo;
        this.chunkRepo       = chunkRepo;
        this.gemini          = gemini;
        this.youtubeService  = youtubeService;
    }

    @Transactional(readOnly = true)
    public List<SourceResponse> listForUser(UUID userId) {
        return sourceRepo.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SourceResponse getById(UUID sourceId, UUID userId) {
        return sourceRepo.findByIdAndUserId(sourceId, userId)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Source not found."));
    }

    public SourceResponse importUrl(ImportUrlRequest req, UUID userId) {
        String url = SanitizationUtil.sanitizeUrl(req.url());

        Source source = new Source();
        source.setUserId(userId);
        source.setType("URL");
        source.setUrl(url);
        source.setTitle(req.title() != null ? req.title().trim() : url);
        source.setStatus("PENDING");
        source = sourceRepo.save(source);

        processSourceAsync(source.getId());
        return toResponse(source);
    }

    public SourceResponse importText(ImportTextRequest req, UUID userId) {
        String text = req.text();
        if (text.length() > MAX_TEXT_CHARS) {
            throw new ValidationException("Text exceeds the 500,000 character limit.");
        }

        Source source = new Source();
        source.setUserId(userId);
        source.setType("TEXT");
        source.setTitle(req.title().trim());
        source.setRawText(text);
        source.setStatus("PENDING");
        source = sourceRepo.save(source);

        processSourceAsync(source.getId());
        return toResponse(source);
    }

    public SourceResponse importPdf(MultipartFile file, UUID userId) throws IOException {
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new ValidationException("PDF file exceeds the 20MB limit.");
        }
        String filename = file.getOriginalFilename();

        // Extract text from PDF bytes (requires Apache PDFBox on classpath)
        String text;
        try (var doc = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            text = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        } catch (Exception e) {
            throw new ValidationException("Failed to read the PDF. Ensure it contains selectable text.");
        }

        Source source = new Source();
        source.setUserId(userId);
        source.setType("PDF");
        source.setTitle(filename != null ? filename : "Uploaded PDF");
        source.setRawText(text);
        source.setStatus("PENDING");
        source = sourceRepo.save(source);

        processSourceAsync(source.getId());
        return toResponse(source);
    }

    /**
     * POST /api/sources/import-youtube
     * Extracts and processes YouTube content as study notes via YouTubeTranscriptService + Gemini.
     */
    public SourceResponse importYoutube(String url, String examId, String subjectId,
                                        String topicId, UUID userId) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("YouTube URL is required.");
        }
        YouTubeTranscriptService.YouTubeResult ytResult = youtubeService.processUrl(url);

        Source source = new Source();
        source.setUserId(userId);
        source.setType("YOUTUBE");
        source.setUrl(url);
        source.setTitle(ytResult.title());
        source.setRawText(ytResult.rawText());
        source.setStatus("PENDING");
        source = sourceRepo.save(source);

        processSourceAsync(source.getId());
        return toResponse(source);
    }

    public void deleteSource(UUID sourceId, UUID userId) {
        Source source = sourceRepo.findByIdAndUserId(sourceId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Source not found."));
        chunkRepo.deleteBySourceId(sourceId);
        sourceRepo.delete(source);
    }

    // ── Async RAG Pipeline ──────────────────────────────────────────────────

    /**
     * Background processing: fetch text (if URL), chunk, embed, generate study guide.
     * Runs in a thread pool managed by Spring's @Async executor.
     */
    @Async
    protected void processSourceAsync(UUID sourceId) {
        sourceRepo.findById(sourceId).ifPresent(source -> {
            try {
                source.setStatus("PROCESSING");
                sourceRepo.save(source);

                String text = resolveText(source);

                // 1. Chunk text
                List<String> chunks = TextChunkingUtil.chunk(text);
                log.info("[Source] {} chunks for sourceId={}", chunks.size(), sourceId);

                // 2. Embed each chunk and save
                for (int i = 0; i < chunks.size(); i++) {
                    String content = chunks.get(i);
                    float[] vec    = gemini.embedText(content);

                    SourceChunk chunk = new SourceChunk();
                    chunk.setSourceId(sourceId);
                    chunk.setContent(content);
                    chunk.setChunkIndex(i);
                    chunk.setEmbedding(vec);
                    chunkRepo.save(chunk);
                }

                // 3. Generate study guide
                String studyGuideJson = gemini.generateContent(
                    STUDY_GUIDE_SYSTEM_PROMPT,
                    "Generate a comprehensive study guide for the following content:\n\n" + text.substring(0, Math.min(text.length(), 8000))
                );
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    source.setStudyGuide(mapper.readTree(studyGuideJson));
                } catch (Exception ex) {
                    log.warn("Failed to parse study guide JSON", ex);
                }
                source.setTopicCount(chunks.size()); // Storing chunk count mapping to topicCount for now
                source.setStatus("READY");
                sourceRepo.save(source);

                log.info("[Source] Processing complete for sourceId={}", sourceId);
            } catch (Exception e) {
                log.error("[Source] Processing failed for sourceId={}: {}", sourceId, e.getMessage());
                sourceRepo.findById(sourceId).ifPresent(s -> {
                    s.setStatus("FAILED");
                    sourceRepo.save(s);
                });
            }
        });
    }

    private String resolveText(Source source) {
        if ("URL".equals(source.getType()) && source.getUrl() != null) {
            // Fetch URL content via HTTP
            try {
                return org.jsoup.Jsoup.connect(source.getUrl())
                    .timeout(10_000)
                    .get()
                    .body()
                    .text();
            } catch (Exception e) {
                throw new ExternalServiceException("URL", "Failed to fetch URL: " + source.getUrl());
            }
        }
        return source.getRawText() != null ? source.getRawText() : "";
    }

    private SourceResponse toResponse(Source source) {
        return new SourceResponse(
            source.getId(),
            source.getTitle(),
            source.getType(),
            source.getUrl(),
            source.getStatus(),
            source.getTopicCount(),
            source.getStudyGuide(),
            source.getCreatedAt(),
            source.getUpdatedAt()
        );
    }

    private static final String STUDY_GUIDE_SYSTEM_PROMPT = """
        You are an expert academic tutor. Analyse the provided study material and return a JSON study guide.
        Return ONLY valid JSON with this structure:
        {
          "summary": "...",
          "keyTopics": ["topic1", "topic2"],
          "conceptMap": { "concept": ["related", "concepts"] },
          "flashcards": [{"front": "...", "back": "..."}],
          "practiceQuestions": ["Q1", "Q2"]
        }
        """;
}
