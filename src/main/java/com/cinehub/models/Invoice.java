package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Hóa đơn thanh toán đặt vé và Combo (Quy tắc BR04 & BR05).
 * Hỗ trợ cả hai kênh giao dịch: Đặt vé trực tuyến (ONLINE) và Bán vé tại quầy (POS).
 *
 * @author Phạm Văn Quyết (202416331)
 * @version 1.0
 */
public class Invoice {

    private int invoiceId;
    private String invoiceCode;
    private Integer userId;
    private String customerName;
    private String customerPhone;
    private Integer staffId;
    private int showtimeId;
    private String movieTitle;
    private String auditoriumName;
    private String startTime;
    private String bookingChannel;
    private String paymentMethod;
    private double seatTotal;
    private double comboTotal;
    private double discountAmount;
    private double totalAmount;
    private String comboSummary;
    private String paymentStatus;
    private String createdAt;

    /**
     * Khởi tạo mặc định đối tượng Hóa đơn.
     */
    public Invoice() {
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getAuditoriumName() {
        return auditoriumName;
    }

    public void setAuditoriumName(String auditoriumName) {
        this.auditoriumName = auditoriumName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getBookingChannel() {
        return bookingChannel;
    }

    public void setBookingChannel(String bookingChannel) {
        this.bookingChannel = bookingChannel;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getSeatTotal() {
        return seatTotal;
    }

    public void setSeatTotal(double seatTotal) {
        this.seatTotal = seatTotal;
    }

    public double getComboTotal() {
        return comboTotal;
    }

    public void setComboTotal(double comboTotal) {
        this.comboTotal = comboTotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getComboSummary() {
        return comboSummary;
    }

    public void setComboSummary(String comboSummary) {
        this.comboSummary = comboSummary;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

