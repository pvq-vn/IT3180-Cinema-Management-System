package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Trạng thái ghế ngồi trong một Suất chiếu cụ thể (Quy tắc BR02 & BR03).
 * Quản lý vòng đời trạng thái: AVAILABLE -> HELD (giữ tạm 5 phút) -> BOOKED.
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public class ShowtimeSeat {

    private int showtimeSeatId;
    private int showtimeId;
    private int seatId;
    private String seatCode;
    private String rowLabel;
    private int seatNumber;
    private String seatType;
    private String status;
    private Integer heldByUserId;
    private String lockedUntil;
    private double finalPrice;

    /**
     * Khởi tạo mặc định đối tượng ghế theo suất chiếu.
     */
    public ShowtimeSeat() {
    }

    /**
     * Khởi tạo đầy đủ thông tin ghế theo suất chiếu.
     *
     * @param showtimeSeatId Mã bản ghi trạng thái ghế theo suất chiếu
     * @param showtimeId     Mã suất chiếu
     * @param seatId         Mã ghế vật lý
     * @param seatCode       Mã ghế hiển thị (A01, B02...)
     * @param rowLabel       Nhãn hàng ghế
     * @param seatNumber     Số thứ tự ghế
     * @param seatType       Hạng ghế (STANDARD, VIP, COUPLE)
     * @param status         Trạng thái (AVAILABLE, HELD, BOOKED)
     * @param heldByUserId   Mã người dùng đang giữ ghế tạm thời (nếu có)
     * @param lockedUntil    Thời điểm hết hạn khóa ghế tạm thời
     * @param finalPrice     Giá bán thực tế của ghế đã cộng phụ thu hạng ghế (BR05)
     */
    public ShowtimeSeat(int showtimeSeatId, int showtimeId, int seatId, String seatCode,
                        String rowLabel, int seatNumber, String seatType, String status,
                        Integer heldByUserId, String lockedUntil, double finalPrice) {
        this.showtimeSeatId = showtimeSeatId;
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.seatCode = seatCode;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.status = status;
        this.heldByUserId = heldByUserId;
        this.lockedUntil = lockedUntil;
        this.finalPrice = finalPrice;
    }

    public int getShowtimeSeatId() {
        return showtimeSeatId;
    }

    public void setShowtimeSeatId(int showtimeSeatId) {
        this.showtimeSeatId = showtimeSeatId;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHeldByUserId() {
        return heldByUserId;
    }

    public void setHeldByUserId(Integer heldByUserId) {
        this.heldByUserId = heldByUserId;
    }

    public String getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(String lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }
}

