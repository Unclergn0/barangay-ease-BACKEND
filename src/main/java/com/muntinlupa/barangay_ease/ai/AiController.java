package com.muntinlupa.barangay_ease.ai;

import com.muntinlupa.barangay_ease.security.AppUser;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final ChatService chatService;
    private final ComplaintClassifierService complaintClassifierService;
    private final SlidingWindowLimiter rateLimiter;
    private final int chatRequestsPerMinute;
    private final int classifyRequestsPerMinute;

    public AiController(
            ChatService chatService,
            ComplaintClassifierService complaintClassifierService,
            SlidingWindowLimiter rateLimiter,
            @Value("${app.ai.chat-requests-per-minute:12}") int chatRequestsPerMinute,
            @Value("${app.ai.classify-requests-per-minute:8}") int classifyRequestsPerMinute) {

        this.chatService = chatService;
        this.complaintClassifierService = complaintClassifierService;
        this.rateLimiter = rateLimiter;
        this.chatRequestsPerMinute = Math.max(1, chatRequestsPerMinute);
        this.classifyRequestsPerMinute = Math.max(1, classifyRequestsPerMinute);
    }

    // =====================================================
    // AI CHAT
    // =====================================================

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody(required = false) AiChatRequest request,
            @AuthenticationPrincipal AppUser currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Authentication required."));
        }

        if (request == null ||
                request.getMessage() == null ||
                request.getMessage().isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("message", "Message is required."));
        }

        if (request.getMessage().length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message must be 2,000 characters or fewer."));
        }

        String userId = "chat:" + currentUser.role() + ":" + currentUser.id();

        if (!rateLimiter.tryAcquire(userId, chatRequestsPerMinute, 60_000L)) {
            return ResponseEntity.status(429).body(
                    Map.of(
                            "message",
                            "Too many requests. Please try again later."
                    ));
        }

        ChatService.ChatResult result =
                chatService.processChat(request, currentUser);

        return ResponseEntity.ok(
                Map.of(
                        "reply", result.reply(),
                        "source", result.source()
                )
        );
    }

    // =====================================================
    // AI COMPLAINT CLASSIFICATION
    // =====================================================

    @PostMapping("/classify-complaint")
    public ResponseEntity<?> classifyComplaint(
            @RequestBody(required = false) ComplaintClassifyRequest request,
            @AuthenticationPrincipal AppUser currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body(
                    Map.of("message", "Authentication required."));
        }

        if (!"RESIDENT".equals(currentUser.role())) {
            return ResponseEntity.status(403).body(
                    Map.of("message", "Resident access only."));
        }

        String userId = "classify:" + currentUser.role() + ":" + currentUser.id();

        if (!rateLimiter.tryAcquire(userId, classifyRequestsPerMinute, 60_000L)) {
            return ResponseEntity.status(429).body(
                    Map.of(
                            "message",
                            "Too many requests. Please try again later."
                    ));
        }

        if (request == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Complaint request is required."));
        }

        String combinedText = request.getCombinedText();

        if (combinedText == null || combinedText.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Complaint title or description is required."
                    ));
        }

        if (combinedText.length() > 10000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Complaint text must be 10,000 characters or fewer."));
        }

        try {

            Object classification =
                    complaintClassifierService.classify(combinedText);

            if (classification == null) {
                return ResponseEntity.ok(
                        Map.of(
                                "priority", "mild",
                                "message", "Default priority assigned."
                        ));
            }

            return ResponseEntity.ok(classification);

        } catch (Exception error) {

            log.error("AI complaint classification failed.", error);

            return ResponseEntity.status(503).body(
                    Map.of(
                            "message",
                            "AI classification is temporarily unavailable."
                    ));
        }
    }
}