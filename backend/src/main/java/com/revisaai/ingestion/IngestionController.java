package com.revisaai.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ingestion/jobs")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionJob> create(
            @RequestParam String banca,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String cargo,
            @RequestParam(required = false) MultipartFile provaArquivo,
            @RequestParam(required = false) String provaUrl,
            @RequestParam(required = false) MultipartFile gabaritoArquivo,
            @RequestParam(required = false) String gabaritoUrl) {

        var job = ingestionService.process(banca, ano, cargo,
                provaArquivo, provaUrl, gabaritoArquivo, gabaritoUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }
}
