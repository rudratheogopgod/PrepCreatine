package com.prepcreatine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Game controller — serves gamified quick-quiz sessions.
 * Returns stub data; actual Gemini-powered question generation is a planned feature.
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    /**
     * POST /api/game/generate — generates a quick-fire quiz session.
     * Body: { subject?: string, count?: number }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String sessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "questions", List.of(
                Map.of(
                    "id",      UUID.randomUUID().toString(),
                    "text",    "Which law states that the pressure of a gas is inversely proportional to its volume at constant temperature?",
                    "options", List.of("Boyle's Law", "Charles' Law", "Gay-Lussac's Law", "Avogadro's Law"),
                    "correct", 0
                ),
                Map.of(
                    "id",      UUID.randomUUID().toString(),
                    "text",    "What is the SI unit of electric current?",
                    "options", List.of("Volt", "Ohm", "Ampere", "Watt"),
                    "correct", 2
                ),
                Map.of(
                    "id",      UUID.randomUUID().toString(),
                    "text",    "The chemical formula for glucose is:",
                    "options", List.of("C6H12O6", "C12H22O11", "C6H6", "CH3COOH"),
                    "correct", 0
                )
            ),
            "timePerQuestion", 15,
            "totalQuestions",  3
        ));
    }

    /** GET /api/game/{id} — get an existing game session. */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getSession(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "Game session not found or expired."));
    }
}
