package com.revisaai.question;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<List<Question>> findAll(
            @RequestParam(required = false) String banca,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Integer ano) {
        return ResponseEntity.ok(questionService.findAll(banca, area, ano));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> findById(@PathVariable String id) {
        return ResponseEntity.ok(questionService.findById(id));
    }
}
