package com.revisaai.ingestion;

import com.revisaai.question.Banca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final IngestionJobRepository repository;
    private final ProvaRepository provaRepository;
    private final DocumentDownloader downloader;
    private final PdfTextExtractor extractor;
    private final QuestionParserService questionParserService;

    public IngestionService(IngestionJobRepository repository,
                            ProvaRepository provaRepository,
                            DocumentDownloader downloader,
                            PdfTextExtractor extractor,
                            QuestionParserService questionParserService) {
        this.repository = repository;
        this.provaRepository = provaRepository;
        this.downloader = downloader;
        this.extractor = extractor;
        this.questionParserService = questionParserService;
    }

    public IngestionJob process(String banca, Integer ano, String orgao, String cargo,
                                MultipartFile provaArquivo, String provaUrl,
                                MultipartFile gabaritoArquivo, String gabaritoUrl) {
        var bancaEnum = Banca.valueOf(banca.toUpperCase());

        boolean semProva = isEmpty(provaArquivo) && isBlank(provaUrl);
        boolean semGabarito = isEmpty(gabaritoArquivo) && isBlank(gabaritoUrl);

        if (semProva) {
            throw new IllegalArgumentException(
                    "É necessário fornecer arquivo ou URL para a prova");
        }
        if (semGabarito) {
            throw new IllegalArgumentException(
                    "É necessário fornecer arquivo ou URL para o gabarito");
        }

        var fileName = !isEmpty(provaArquivo) ? provaArquivo.getOriginalFilename() : provaUrl;
        var prova = provaRepository.save(new Prova(bancaEnum, ano != null ? ano : 0, orgao, cargo, fileName));

        var job = repository.save(new IngestionJob(bancaEnum, ano, cargo));
        job.setProvaId(prova.getId());
        repository.save(job);
        log.debug("IngestionJob criado: {} — banca={}, provaId={}", job.getId(), bancaEnum, prova.getId());

        try {
            var provaBytes = isEmpty(provaArquivo)
                    ? downloader.download(provaUrl)
                    : provaArquivo.getBytes();

            var gabaritoBytes = isEmpty(gabaritoArquivo)
                    ? downloader.download(gabaritoUrl)
                    : gabaritoArquivo.getBytes();

            job.setTextProva(extractor.extract(provaBytes));
            job.setTextGabarito(extractor.extract(gabaritoBytes));

            var result = questionParserService.parse(
                    job.getTextProva(), job.getTextGabarito(), bancaEnum, ano, cargo, prova.getId());
            job.setQuestoesSalvas(result.questoesSalvas());
            job.setQuestoesInvalidas(result.questoesInvalidas());

            if (result.partialErrorMessage() != null) {
                job.setErrorMessage(result.partialErrorMessage());
            }

            boolean algumaSalva = result.questoesSalvas() + result.questoesInvalidas() > 0;
            job.setStatus(algumaSalva ? IngestionStatus.COMPLETED : IngestionStatus.FAILED);
            log.info("IngestionJob {} concluído — questoesSalvas={}, questoesInvalidas={}, parcial={}",
                    job.getId(), result.questoesSalvas(), result.questoesInvalidas(),
                    result.partialErrorMessage() != null);

        } catch (Exception e) {
            log.error("Falha ao processar IngestionJob {}: {}", job.getId(), e.getMessage(), e);
            job.setStatus(IngestionStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        }

        return repository.save(job);
    }

    public Optional<IngestionJob> findById(String id) {
        return repository.findById(id);
    }

    private boolean isEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private boolean isBlank(String url) {
        return url == null || url.isBlank();
    }
}
