package com.erdem.microservices.invoice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import java.io.File;

import com.erdem.microservices.invoice.dto.InvoiceResponseDto;
import com.erdem.microservices.invoice.service.InvoiceService;



@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {
    
    private InvoiceService invoiceService;
    
    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/convert")
    public ResponseEntity<InvoiceResponseDto> convertInvoice(
            @RequestParam("invoiceXml") MultipartFile invoiceXml,
            @RequestParam("invoiceId") String invoiceId) {
        try {
            // save file locally
            File xmlFile = invoiceService.saveFile(invoiceXml, invoiceId);
            File htmlFile = invoiceService.convertInvoiceToHtml(xmlFile, invoiceId);
            return ResponseEntity.ok(new InvoiceResponseDto("Invoice converted successfully", htmlFile.getAbsolutePath()));

        }catch (Exception e) {
            return ResponseEntity.status(500).body(new InvoiceResponseDto(e.getMessage(), null));
        }        
}}

// private String extractErrorDetails(Exception e) {
//     //parsing-related exceptions 
//     if (e.getCause() instanceof org.xml.sax.SAXParseException) {
//         org.xml.sax.SAXParseException parseException = (org.xml.sax.SAXParseException) e.getCause();
//         return "Line: " + parseException.getLineNumber() + ", Column: " + parseException.getColumnNumber() + ", Message: " + parseException.getMessage();
//     }
    
//     return e.getMessage();
// }
// }

