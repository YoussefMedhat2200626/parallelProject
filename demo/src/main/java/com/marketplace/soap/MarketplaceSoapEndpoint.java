package com.marketplace.soap;

import com.marketplace.entity.Transaction;
import com.marketplace.entity.User;
import com.marketplace.service.*;
import com.marketplace.entity.OtpCode.OtpPurpose;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import jakarta.xml.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SOAP Web Service Endpoint for Marketplace Operations.
 * Namespace: http://marketplace.com/soap
 * 
 * Operations:
 * 1. getTransactionReport - Generate transaction reports
 * 2. purchaseItem - Execute a purchase with 2FA
 * 3. getUserInfo - Get user account information
 */
@Endpoint
public class MarketplaceSoapEndpoint {

    private static final String NAMESPACE = "http://marketplace.com/soap";

    private final ReportService reportService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final WalletService walletService;
    private final TwoFactorService twoFactorService;

    public MarketplaceSoapEndpoint(ReportService reportService, TransactionService transactionService,
                                    UserService userService, WalletService walletService,
                                    TwoFactorService twoFactorService) {
        this.reportService = reportService;
        this.transactionService = transactionService;
        this.userService = userService;
        this.walletService = walletService;
        this.twoFactorService = twoFactorService;
    }

    // ===================== SOAP Operation 1: Transaction Report =====================

    @PayloadRoot(namespace = NAMESPACE, localPart = "getTransactionReportRequest")
    @ResponsePayload
    public GetTransactionReportResponse getTransactionReport(
            @RequestPayload GetTransactionReportRequest request) {

        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        List<Transaction> transactions = reportService.getTransactionsByDateRange(startDate, endDate);

        GetTransactionReportResponse response = new GetTransactionReportResponse();
        response.setTotalCount(transactions.size());

        List<TransactionSoapDto> dtos = new ArrayList<>();
        for (Transaction t : transactions) {
            TransactionSoapDto dto = new TransactionSoapDto();
            dto.setTransactionId(t.getTransactionId());
            dto.setBuyerId(t.getBuyerId());
            dto.setSellerId(t.getSellerId());
            dto.setItemId(t.getItemId());
            dto.setTotalCents(t.getTotalCents());
            dto.setType(t.getType().name());
            dto.setStatus(t.getStatus().name());
            dto.setCreatedAt(t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            dtos.add(dto);
        }
        response.setTransactions(dtos);
        return response;
    }

    // ===================== SOAP Operation 2: Purchase Item =====================

    @PayloadRoot(namespace = NAMESPACE, localPart = "purchaseItemRequest")
    @ResponsePayload
    public PurchaseItemResponse purchaseItem(@RequestPayload PurchaseItemRequest request) {
        PurchaseItemResponse response = new PurchaseItemResponse();
        try {
            boolean otpValid = twoFactorService.validateOtp(
                    request.getBuyerId(), request.getOtpCode(), OtpPurpose.PURCHASE);
            if (!otpValid) {
                response.setSuccess(false);
                response.setMessage("Invalid or expired OTP code");
                return response;
            }

            Transaction txn = transactionService.purchaseItem(
                    request.getBuyerId(), request.getItemId(), request.getQuantity());
            response.setSuccess(true);
            response.setMessage("Purchase completed successfully");
            response.setTransactionId(txn.getTransactionId());
            response.setReferenceCode(txn.getReferenceCode());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    // ===================== SOAP Operation 3: Get User Info =====================

    @PayloadRoot(namespace = NAMESPACE, localPart = "getUserInfoRequest")
    @ResponsePayload
    public GetUserInfoResponse getUserInfo(@RequestPayload GetUserInfoRequest request) {
        GetUserInfoResponse response = new GetUserInfoResponse();
        userService.findById(request.getUserId()).ifPresentOrElse(
                user -> {
                    response.setUserId(user.getUserId());
                    response.setUsername(user.getUsername());
                    response.setFullName(user.getFullName());
                    response.setEmail(user.getEmail());
                    response.setBalanceCents(walletService.getBalance(user.getUserId()));
                    response.setFound(true);
                },
                () -> response.setFound(false)
        );
        return response;
    }

    // ===================== JAXB DTOs =====================

    @XmlRootElement(name = "getTransactionReportRequest", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetTransactionReportRequest {
        private String startDate;
        private String endDate;
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    @XmlRootElement(name = "getTransactionReportResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetTransactionReportResponse {
        private int totalCount;
        @XmlElement(name = "transaction")
        private List<TransactionSoapDto> transactions;
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public List<TransactionSoapDto> getTransactions() { return transactions; }
        public void setTransactions(List<TransactionSoapDto> transactions) { this.transactions = transactions; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TransactionSoapDto {
        private Long transactionId;
        private Long buyerId;
        private Long sellerId;
        private Long itemId;
        private Long totalCents;
        private String type;
        private String status;
        private String createdAt;
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public Long getBuyerId() { return buyerId; }
        public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
        public Long getSellerId() { return sellerId; }
        public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Long getTotalCents() { return totalCents; }
        public void setTotalCents(Long totalCents) { this.totalCents = totalCents; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @XmlRootElement(name = "purchaseItemRequest", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PurchaseItemRequest {
        private Long buyerId;
        private Long itemId;
        private int quantity;
        private String otpCode;
        public Long getBuyerId() { return buyerId; }
        public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    }

    @XmlRootElement(name = "purchaseItemResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PurchaseItemResponse {
        private boolean success;
        private String message;
        private Long transactionId;
        private String referenceCode;
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public String getReferenceCode() { return referenceCode; }
        public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    }

    @XmlRootElement(name = "getUserInfoRequest", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetUserInfoRequest {
        private Long userId;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    @XmlRootElement(name = "getUserInfoResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetUserInfoResponse {
        private boolean found;
        private Long userId;
        private String username;
        private String fullName;
        private String email;
        private Long balanceCents;
        public boolean isFound() { return found; }
        public void setFound(boolean found) { this.found = found; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Long getBalanceCents() { return balanceCents; }
        public void setBalanceCents(Long balanceCents) { this.balanceCents = balanceCents; }
    }
}
