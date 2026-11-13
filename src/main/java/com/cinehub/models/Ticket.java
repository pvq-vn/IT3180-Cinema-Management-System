package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Vé xem phim điện tử kèm mã QR để soát vé Check-in (Quy tắc BR04 & CSD05).
 * Quản lý vòng đời trạng thái vé: ISSUED (mới phát hành) -> CHECKED_IN (đã soát vé) hoặc CANCELLED (đã hủy).
 *
 * @author Lê Anh Tuấn (202416639)
 * @version 1.0
 */
public class Ticket {

    private int ticketId;
    private String ticketCode;
    private String qrPayload;
    private int invoiceId;
    private String invoiceCode;
    private String customerName;
    private int showtimeId;
    private int showtimeSeatId;
    private String seatCode;
    private String seatType;
    private String movieTitle;
    private String auditoriumName;
    private String startTime;
    private double ticketPrice;
    private String ticketStatus;
    private String checkedInAt;
    private Integer checkedInBy;

    /**
     * Khởi tạo mặc định đối tượng Vé điện tử.
     */
    public Ticket() {
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getShowtimeSeatId() {
        return showtimeSeatId;
    }

    public void setShowtimeSeatId(int showtimeSeatId) {
        this.showtimeSeatId = showtimeSeatId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
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

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public String getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(String checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Integer getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(Integer checkedInBy) {
        this.checkedInBy = checkedInBy;
    }
}

