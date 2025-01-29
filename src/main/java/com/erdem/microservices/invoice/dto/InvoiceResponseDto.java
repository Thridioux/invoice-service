package com.erdem.microservices.invoice.dto;

public class InvoiceResponseDto {
    private String message;
    private String htmlFilePath;

    public InvoiceResponseDto(String message, String htmlFilePath) {
        this.message = message;
        this.htmlFilePath = htmlFilePath;
    }

    public String getMessage() {
        return message;
    }

    public String getHtmlFilePath() {
        return htmlFilePath;
    }
    

}
