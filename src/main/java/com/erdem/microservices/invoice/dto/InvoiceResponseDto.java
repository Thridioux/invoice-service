package com.erdem.microservices.invoice.dto;

public class InvoiceResponseDto {
    private String message;
    private String htmlContent;  

    // Constructor 
    public InvoiceResponseDto(String message, String htmlContent) {
        this.message = message;
        this.htmlContent = htmlContent;
    }

    public String getMessage() {
        return message;
    }

    public String getHtmlContent() {
        return htmlContent;
    }
}
