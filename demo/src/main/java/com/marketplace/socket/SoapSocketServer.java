package com.marketplace.socket;

import com.marketplace.entity.*;
import com.marketplace.entity.OtpCode.OtpPurpose;
import com.marketplace.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SOAP Web Services implemented using raw Java Sockets.
 * Listens on port 9091 and manually parses HTTP POST requests
 * containing XML SOAP envelopes. Builds XML responses by hand.
 *
 * SOAP Operations:
 *   1. getTransactionReport — Transaction reports by date range
 *   2. purchaseItem         — Execute purchase with 2FA OTP
 *   3. getUserInfo           — User account information
 */
@Component
public class SoapSocketServer {

    private static final Logger LOG = LoggerFactory.getLogger(SoapSocketServer.class);
    private static final int PORT = 9091;
    private static final String NS = "http://marketplace.com/soap";

    private final ReportService reportService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final WalletService walletService;
    private final TwoFactorService twoFactorService;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public SoapSocketServer(ReportService reportService, TransactionService transactionService,
                            UserService userService, WalletService walletService,
                            TwoFactorService twoFactorService) {
        this.reportService = reportService;
        this.transactionService = transactionService;
        this.userService = userService;
        this.walletService = walletService;
        this.twoFactorService = twoFactorService;
    }

    @PostConstruct
    public void start() {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                LOG.info("SOAP Socket Server started on port {}", PORT);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Thread handler = new Thread(() -> handleClient(clientSocket));
                        handler.setDaemon(true);
                        handler.start();
                    } catch (IOException e) {
                        if (running) LOG.error("Error accepting SOAP connection", e);
                    }
                }
            } catch (IOException e) {
                LOG.error("Failed to start SOAP Socket Server on port {}", PORT, e);
            }
        }, "soap-socket-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            LOG.error("Error closing SOAP server socket", e);
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            // ---- 1. Parse HTTP request line ----
            String requestLine = in.readLine();
            if (requestLine == null) return;

            // ---- 2. Parse headers ----
            int contentLength = 0;
            String soapAction = "";
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                String lower = headerLine.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.substring(15).trim());
                } else if (lower.startsWith("soapaction:")) {
                    soapAction = headerLine.substring(11).trim().replace("\"", "");
                }
            }

            // ---- 3. Read XML body ----
            String xmlBody = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = in.read(buf, totalRead, contentLength - totalRead);
                    if (read < 0) break;
                    totalRead += read;
                }
                xmlBody = new String(buf, 0, totalRead);
            }

            // Handle GET requests for WSDL
            if (requestLine.startsWith("GET")) {
                String wsdl = generateWsdl();
                sendHttpResponse(out, 200, "text/xml", wsdl);
                return;
            }

            LOG.debug("SOAP Socket: received {} bytes, soapAction={}", xmlBody.length(), soapAction);

            // ---- 4. Detect operation from XML body ----
            String responseXml;
            if (xmlBody.contains("getTransactionReportRequest")) {
                responseXml = handleGetTransactionReport(xmlBody);
            } else if (xmlBody.contains("purchaseItemRequest")) {
                responseXml = handlePurchaseItem(xmlBody);
            } else if (xmlBody.contains("getUserInfoRequest")) {
                responseXml = handleGetUserInfo(xmlBody);
            } else {
                responseXml = buildSoapFault("Unknown operation");
            }

            // ---- 5. Wrap in SOAP Envelope and send ----
            String soapResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                    " xmlns:ns=\"" + NS + "\">\n" +
                    "  <soap:Body>\n" +
                    responseXml + "\n" +
                    "  </soap:Body>\n" +
                    "</soap:Envelope>";

            sendHttpResponse(out, 200, "text/xml", soapResponse);

        } catch (Exception e) {
            LOG.error("Error handling SOAP socket client", e);
        }
    }

    // ===================== SOAP Operation 1: Transaction Report =====================

    private String handleGetTransactionReport(String xmlBody) {
        String startDate = extractXmlValue(xmlBody, "startDate");
        String endDate = extractXmlValue(xmlBody, "endDate");

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<Transaction> transactions = reportService.getTransactionsByDateRange(start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("    <ns:getTransactionReportResponse>\n");
        sb.append("      <ns:totalCount>").append(transactions.size()).append("</ns:totalCount>\n");

        for (Transaction t : transactions) {
            sb.append("      <ns:transaction>\n");
            sb.append("        <ns:transactionId>").append(t.getTransactionId()).append("</ns:transactionId>\n");
            sb.append("        <ns:buyerId>").append(t.getBuyerId()).append("</ns:buyerId>\n");
            sb.append("        <ns:sellerId>").append(t.getSellerId()).append("</ns:sellerId>\n");
            sb.append("        <ns:itemId>").append(t.getItemId()).append("</ns:itemId>\n");
            sb.append("        <ns:totalCents>").append(t.getTotalCents()).append("</ns:totalCents>\n");
            sb.append("        <ns:type>").append(t.getType().name()).append("</ns:type>\n");
            sb.append("        <ns:status>").append(t.getStatus().name()).append("</ns:status>\n");
            sb.append("        <ns:createdAt>").append(t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</ns:createdAt>\n");
            sb.append("      </ns:transaction>\n");
        }

        sb.append("    </ns:getTransactionReportResponse>");
        return sb.toString();
    }

    // ===================== SOAP Operation 2: Purchase Item =====================

    private String handlePurchaseItem(String xmlBody) {
        try {
            Long buyerId = Long.parseLong(extractXmlValue(xmlBody, "buyerId"));
            Long itemId = Long.parseLong(extractXmlValue(xmlBody, "itemId"));
            int quantity = Integer.parseInt(extractXmlValue(xmlBody, "quantity"));
            String otpCode = extractXmlValue(xmlBody, "otpCode");

            // Validate OTP
            boolean otpValid = twoFactorService.validateOtp(buyerId, otpCode, OtpPurpose.PURCHASE);
            if (!otpValid) {
                return buildOperationResponse("purchaseItemResponse", false,
                        "Invalid or expired OTP code", null, null);
            }

            Transaction txn = transactionService.purchaseItem(buyerId, itemId, quantity);
            return buildOperationResponse("purchaseItemResponse", true,
                    "Purchase completed successfully",
                    txn.getTransactionId(), txn.getReferenceCode());

        } catch (Exception e) {
            return buildOperationResponse("purchaseItemResponse", false,
                    escapeXml(e.getMessage()), null, null);
        }
    }

    private String buildOperationResponse(String tag, boolean success, String message,
                                          Long transactionId, String referenceCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <ns:").append(tag).append(">\n");
        sb.append("      <ns:success>").append(success).append("</ns:success>\n");
        sb.append("      <ns:message>").append(escapeXml(message)).append("</ns:message>\n");
        if (transactionId != null)
            sb.append("      <ns:transactionId>").append(transactionId).append("</ns:transactionId>\n");
        if (referenceCode != null)
            sb.append("      <ns:referenceCode>").append(escapeXml(referenceCode)).append("</ns:referenceCode>\n");
        sb.append("    </ns:").append(tag).append(">");
        return sb.toString();
    }

    // ===================== SOAP Operation 3: Get User Info =====================

    private String handleGetUserInfo(String xmlBody) {
        Long userId = Long.parseLong(extractXmlValue(xmlBody, "userId"));
        StringBuilder sb = new StringBuilder();
        sb.append("    <ns:getUserInfoResponse>\n");

        userService.findById(userId).ifPresentOrElse(
                user -> {
                    sb.append("      <ns:found>true</ns:found>\n");
                    sb.append("      <ns:userId>").append(user.getUserId()).append("</ns:userId>\n");
                    sb.append("      <ns:username>").append(escapeXml(user.getUsername())).append("</ns:username>\n");
                    sb.append("      <ns:fullName>").append(escapeXml(user.getFullName())).append("</ns:fullName>\n");
                    sb.append("      <ns:email>").append(escapeXml(user.getEmail())).append("</ns:email>\n");
                    sb.append("      <ns:balanceCents>").append(walletService.getBalance(user.getUserId())).append("</ns:balanceCents>\n");
                },
                () -> sb.append("      <ns:found>false</ns:found>\n")
        );

        sb.append("    </ns:getUserInfoResponse>");
        return sb.toString();
    }

    // ===================== WSDL Generation =====================

    private String generateWsdl() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"\n" +
               "  xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
               "  xmlns:tns=\"" + NS + "\"\n" +
               "  targetNamespace=\"" + NS + "\">\n" +
               "\n" +
               "  <wsdl:types>\n" +
               "    <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"" + NS + "\">\n" +
               "      <!-- See marketplace.xsd for full schema -->\n" +
               "    </xs:schema>\n" +
               "  </wsdl:types>\n" +
               "\n" +
               "  <wsdl:portType name=\"MarketplacePort\">\n" +
               "    <wsdl:operation name=\"getTransactionReport\"/>\n" +
               "    <wsdl:operation name=\"purchaseItem\"/>\n" +
               "    <wsdl:operation name=\"getUserInfo\"/>\n" +
               "  </wsdl:portType>\n" +
               "\n" +
               "  <wsdl:binding name=\"MarketplaceBinding\" type=\"tns:MarketplacePort\">\n" +
               "    <soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
               "  </wsdl:binding>\n" +
               "\n" +
               "  <wsdl:service name=\"MarketplaceService\">\n" +
               "    <wsdl:port name=\"MarketplacePort\" binding=\"tns:MarketplaceBinding\">\n" +
               "      <soap:address location=\"http://localhost:9091/ws\"/>\n" +
               "    </wsdl:port>\n" +
               "  </wsdl:service>\n" +
               "</wsdl:definitions>";
    }

    // ===================== XML Helpers =====================

    /**
     * Manually extracts the text content of a simple XML element.
     * e.g. extractXmlValue("<foo>bar</foo>", "foo") → "bar"
     */
    private String extractXmlValue(String xml, String tagName) {
        // Try with namespace prefix first
        String[] prefixes = {"ns:", "soap:", ""};
        for (String prefix : prefixes) {
            String openTag = "<" + prefix + tagName + ">";
            String closeTag = "</" + prefix + tagName + ">";
            int start = xml.indexOf(openTag);
            if (start >= 0) {
                start += openTag.length();
                int end = xml.indexOf(closeTag, start);
                if (end >= 0) return xml.substring(start, end).trim();
            }
        }
        // Try any namespace prefix like <xyz:tagName>
        int tagStart = xml.indexOf(":" + tagName + ">");
        if (tagStart < 0) tagStart = xml.indexOf("<" + tagName + ">");
        if (tagStart >= 0) {
            int contentStart = xml.indexOf(">", tagStart) + 1;
            int contentEnd = xml.indexOf("<", contentStart);
            if (contentEnd > contentStart) return xml.substring(contentStart, contentEnd).trim();
        }
        return "";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String buildSoapFault(String message) {
        return "    <soap:Fault>\n" +
               "      <faultcode>soap:Server</faultcode>\n" +
               "      <faultstring>" + escapeXml(message) + "</faultstring>\n" +
               "    </soap:Fault>";
    }

    // ===================== HTTP Helpers =====================

    private void sendHttpResponse(OutputStream out, int statusCode, String contentType, String body)
            throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Type: " + contentType + "; charset=utf-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }
}
