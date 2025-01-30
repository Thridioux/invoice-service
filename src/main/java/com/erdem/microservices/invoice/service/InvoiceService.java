package com.erdem.microservices.invoice.service;

import java.io.*;
import java.nio.file.*;
import org.w3c.dom.*;
import net.sf.saxon.s9api.*;
import java.util.Base64;

import org.springframework.web.multipart.MultipartFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import net.sf.saxon.s9api.Serializer;

import org.htmlcleaner.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {
    
     private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    @Value("${invoice.output-dir}")
    private String outputDir;

    private void ensureDirectoryExists(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    @Cacheable(value = "invoiceHtmlCache", key = "#invoiceId")
    public String convertInvoiceToHtml(File invoiceXml, String invoiceId) throws Exception {
        File xsltFile = null;
        File htmlFile;
    
        try {
            xsltFile = extractXsltFromXml(invoiceXml);
            htmlFile = performTransformation(invoiceXml, xsltFile, invoiceId);
            return new String(Files.readAllBytes(htmlFile.toPath())); // Read HTML as string
        } finally {
            if (xsltFile != null && xsltFile.exists()) {
                xsltFile.delete();
            }
            if (invoiceXml.exists()) {
                invoiceXml.delete();
            }
        }
    }
    
    
    

    private File extractXsltFromXml(File invoiceXml) throws Exception{
        //parse the xml
        
        File xsltFile = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(invoiceXml);

                 // Extract XSLT using XPath to handle namespaces
                 String base64Xslt = extractBase64(document);
                 if (base64Xslt != null && !base64Xslt.isEmpty()) {
                    byte[] decodedXslt = Base64.getDecoder().decode(base64Xslt);
                    
                    // Store XSLT file
                    Path xsltPath = Paths.get(outputDir, invoiceXml.getName() + ".xslt");
                    Files.write(xsltPath, decodedXslt);
                    xsltFile = xsltPath.toFile();
                    
                    if (!xsltFile.exists()) {
                        throw new IOException("Failed to create XSLT file at: " + xsltPath);
                    }
            }
        }catch (Exception e) {
            logger.error("Error extracting XSLT from XML", e);
            throw new Exception("Error extracting XSLT from XML: " + e.getMessage(), e);
        }
        return xsltFile;
    }
    
    private String extractBase64(Document document) throws XPathExpressionException{
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xPath.compile("//*[local-name()='EmbeddedDocumentBinaryObject']");
        Node node = (Node) expr.evaluate(document, XPathConstants.NODE);
        return node != null ? node.getTextContent().trim() : null;
    
    }
    
    private File performTransformation(File invoiceXml, File xsltFile, String invoiceId) throws Exception {
        ensureDirectoryExists(outputDir);
        Path htmlPath = Paths.get(outputDir, invoiceId + ".html");
        File htmlFile = htmlPath.toFile();

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable executable = compiler.compile(new StreamSource(xsltFile));

        try (OutputStream outputStream = new FileOutputStream(htmlFile);
             InputStream inputStream = new FileInputStream(invoiceXml)) {

            Serializer serializer = processor.newSerializer(outputStream);
            serializer.setOutputProperty(Serializer.Property.ENCODING, "UTF-8");
            serializer.setOutputProperty(Serializer.Property.METHOD, "html");

            XsltTransformer transformer = executable.load();
            transformer.setSource(new StreamSource(inputStream));
            transformer.setDestination(serializer);
            transformer.transform();
        }

        // Modify HTML to handle embedded images 
        modifyHtmlFile(htmlFile, invoiceId);

        return htmlFile;
    }
   
    private void modifyHtmlFile(File htmlFile, String invoiceId) throws Exception {
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode rootNode = cleaner.clean(htmlFile);
        
        //modify img source
        for (Object imgNode: rootNode.getElementsByName("img", true)){
            TagNode imgTag = (TagNode) imgNode;
            String src = imgTag.getAttributeByName("src");
            if (src != null && !src.isEmpty()) {
                
                imgTag.addAttribute("src", reConvertToBase64(src));
            }
        }
        //modify title
        for (Object titleNode: rootNode.getElementsByName("title", true)){
            TagNode titleTag = (TagNode) titleNode;
            titleTag.removeAllChildren();
            titleTag.addChild(new ContentNode(invoiceId));
        }
        try (FileWriter writer = new FileWriter(htmlFile)){
            new PrettyXmlSerializer(cleaner.getProperties()).writeToFile(rootNode, htmlFile.getAbsolutePath(), "UTF-8");
        }
    }

    private String reConvertToBase64(String src) {
        try {
            File imagFile = new File(src);

            if (!imagFile.exists() || !imagFile.isFile()) {
                throw new IOException("Image file not found at: " + src);
            }
            //read the image as bytes
            byte[] imageBytes = Files.readAllBytes(Paths.get(src));

            //encode the byte array to base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Determine the image's MIME type (e.g., png, jpg, etc.)
            String mimeType = Files.probeContentType(Paths.get(src));

            // Construct and return the Data URI
            return "data:" + mimeType + ";base64," + base64Image;

        } catch (IOException e) {
            logger.error("Unable to convert image to Base64: " + e.getMessage(), e);
            return src;

        }        
        
    }
    
    
    public File saveFile(MultipartFile multipartFile, String fileType) throws IOException {
        ensureDirectoryExists(outputDir);
        Path filePath = Paths.get(outputDir, multipartFile.getOriginalFilename());
        Files.copy(multipartFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toFile();
    }

}
