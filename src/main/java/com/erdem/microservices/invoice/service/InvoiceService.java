package com.erdem.microservices.invoice.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Serializer;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.stereotype.Service;
import org.htmlcleaner.PrettyXmlSerializer;

@Service
public class InvoiceService {
    
    private static final String TARGET_DIR = "/Users/erdem/invoice-service/outputs/";
    public File convertInvoiceToHtml(File invoiceXml,String invoiceId) throws Exception {
        
        File xsltFile = extractXsltFromXml(invoiceXml);
        return performTransformation(invoiceXml, xsltFile, invoiceId);
    }

    private File extractXsltFromXml(File invoiceXml) throws Exception{
        //parse the xml
        
        File xsltFile = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(invoiceXml);

            //extract <cbc:EmbeddedDocumentBinaryObject> tag
            NodeList nodeList = document.getElementsByTagName("cbc:EmbeddedDocumentBinaryObject");
            if (nodeList.getLength() > 0) {
                Element element = (Element) nodeList.item(0);

                // Get the Base64 encoded XSLT content
                String base64Xslt = element.getTextContent();
                byte[] decodedXslt = Base64.getDecoder().decode(base64Xslt);

                // Write the decoded XSLT into temp file
                String xsltFileName = TARGET_DIR + invoiceXml.getName() + ".xslt";
                xsltFile = new File(xsltFileName);
                try (OutputStream fos = new FileOutputStream(xsltFile)){
                    fos.write(decodedXslt);
                }
            }
    }   catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error extracting XSLT from XML: " + e.getMessage());
        }
        return xsltFile;
    }
    
    
    private File performTransformation(File invoiceXml, File xsltFile, String invoiceId) throws Exception {
        File directory = new File(TARGET_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String htmlFileName = TARGET_DIR + invoiceId + ".html";
        File htmlFile = new File(htmlFileName);

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable executable = compiler.compile(new StreamSource(xsltFile));

        try (OutputStream outputStream = new FileOutputStream(htmlFile)) {
            Serializer serializer = processor.newSerializer(outputStream);
            serializer.setOutputProperty(Serializer.Property.ENCODING, "UTF-8");
            serializer.setOutputProperty(Serializer.Property.METHOD, "html");

            try (InputStream inputStream = new FileInputStream(invoiceXml)) {
                XsltTransformer transformer = executable.load();
                transformer.setSource(new StreamSource(inputStream));
                transformer.setDestination(serializer);
                transformer.transform();
            }
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
            if (src != null ) {
                
                imgTag.addAttribute("src", reConvertToBase64(src, invoiceId));
            }
        }
        //modify title
        for (Object titleNode: rootNode.getElementsByName("title", true)){
            TagNode titleTag = (TagNode) titleNode;
            titleTag.removeAllChildren();
            titleTag.addChild(new TagNode(invoiceId));
        }
        try (FileWriter writer = new FileWriter(htmlFile)){
            new PrettyXmlSerializer(cleaner.getProperties()).writeToFile(rootNode, htmlFile.getAbsolutePath(), "UTF-8");
        }
    }

    private String reConvertToBase64(String src, String invoiceId) {
        try {
            File imagFile = new File(src);

            if (!imagFile.exists()) {
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
            e.printStackTrace();
            return "Error: Unable to convert image to Base64. " + e.getMessage();

        }        
        
    }
    
    
    public File saveFile(MultipartFile multipartFile, String fileType) throws IOException {
        File directory;
        directory = new File(TARGET_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    
        if ("xml".equals(fileType)) {
            directory = new File(TARGET_DIR + "xml/");
        } else if ("xslt".equals(fileType)) {
            directory = new File(TARGET_DIR + "xslt/");
        } 
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(Paths.get(directory.getPath(), multipartFile.getOriginalFilename()).toString());
        Files.copy(multipartFile.getInputStream(), file.toPath());
        return file;
    }

}
