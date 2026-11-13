package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Ghế ngồi vật lý cố định trong một Phòng chiếu (Quy tắc BR02).
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public class Seat {

    private int seatId;
    private int auditoriumId;
    private String rowLabel;
    private int seatNumber;
    private String seatCode;
    private String seatType;
    private boolean active;

    /**
     * Khởi tạo mặc định đối tượng ghế vật lý.
     */
    public Seat() {
    }

    /**
     * Khởi tạo đầy đủ thông tin ghế vật lý.
     *
     * @param seatId       Mã ghế vật lý
     * @param auditoriumId Mã phòng chiếu sở hữu ghế
     * @param rowLabel     Nhãn hàng ghế (A, B, C, D, E, F)
     * @param seatNumber   Số thứ tự ghế trong hàng
     * @param seatCode     Mã ghế hiển thị (ví dụ: A01, C05)
     * @param seatType     Hạng ghế (STANDARD, VIP, COUPLE)
     * @param active       Trạng thái sử dụng của ghế vật lý
     */
    public Seat(int seatId, int auditoriumId, String rowLabel, int seatNumber,
                String seatCode, String seatType, boolean active) {
        this.seatId = seatId;
        this.auditoriumId = auditoriumId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatCode = seatCode;
        this.seatType = seatType;
        this.active = active;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getAuditoriumId() {
        return auditoriumId;
    }

    public void setAuditoriumId(int auditoriumId) {
        this.auditoriumId = auditoriumId;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

