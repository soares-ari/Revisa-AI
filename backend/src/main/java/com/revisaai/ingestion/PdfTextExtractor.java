package com.revisaai.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    public String extract(byte[] pdfBytes) throws IOException {
        log.debug("Extraindo texto de PDF ({} bytes)", pdfBytes.length);
        try (var doc = Loader.loadPDF(pdfBytes)) {
            var stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}
