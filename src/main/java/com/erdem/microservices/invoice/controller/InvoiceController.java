package com.erdem.microservices.invoice.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

import java.util.HashMap;
import java.util.Map;

import com.erdem.microservices.invoice.service.InvoiceService;


@RestController
@CrossOrigin(origins = "*") 
@RequestMapping("/invoices")
public class InvoiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;
    @Value("${invoice.output-dir}")
    private String outputDir;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> getInvoiceHtml(
        @RequestParam("file") MultipartFile file, 
        @RequestParam("invoiceId") String invoiceId 
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".xml")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "Invalid file type. Only XML files are allowed."));
            }
    
            File invoiceXml = invoiceService.saveFile(file, invoiceId); 
            String htmlContent = invoiceService.convertInvoiceToHtml(invoiceXml, invoiceId);
    
            response.put("status", "success");
            response.put("invoiceId", invoiceId); 
            response.put("htmlContent", htmlContent);
    
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing invoice", e);
            response.put("status", "error");
            response.put("message", "Failed to convert invoice: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/download/{invoiceId}")
    public ResponseEntity<Resource> downloadInvoiceHtml(@PathVariable String invoiceId) {
        try {
            Path htmlPath = Paths.get(outputDir, invoiceId + ".html");
            FileSystemResource resource = new FileSystemResource(htmlPath.toFile());

            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + htmlPath.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading invoice HTML", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
