package com.revisaai.study;

import com.revisaai.study.dto.AnswerRequest;
import com.revisaai.study.dto.AnswerResponse;
import com.revisaai.study.dto.CreateSessionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/study/sessions")
public class StudyController {

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @PostMapping
    public ResponseEntity<StudySession> create(
            @RequestBody CreateSessionRequest request,
            Authentication authentication) {
        var session = studyService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudySession> findById(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(studyService.findById(id, authentication.getName()));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<AnswerResponse> answer(
            @PathVariable String id,
            @Valid @RequestBody AnswerRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(studyService.answer(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<StudySession> finish(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(studyService.finish(id, authentication.getName()));
    }
}
