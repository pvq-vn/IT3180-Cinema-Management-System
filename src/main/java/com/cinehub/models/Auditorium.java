package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Phòng chiếu phim vật lý tại rạp (Phân hệ CSD03).
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public class Auditorium {

    private int auditoriumId;
    private String name;
    private String roomType;
    private int totalRows;
    private int seatsPerRow;
    private int totalSeats;
    private String status;

    /**
     * Khởi tạo mặc định đối tượng phòng chiếu.
     */
    public Auditorium() {
    }

    /**
     * Khởi tạo đầy đủ thông tin một phòng chiếu.
     *
     * @param auditoriumId Mã định danh phòng chiếu
     * @param name         Tên phòng chiếu
     * @param roomType     Loại công nghệ phòng chiếu (IMAX Laser, 2D Dolby Atmos, Gold Class)
     * @param totalRows    Tổng số hàng ghế
     * @param seatsPerRow  Số lượng ghế mỗi hàng
     * @param totalSeats   Tổng sức chứa ghế ngồi
     * @param status       Trạng thái hoạt động (ACTIVE, MAINTENANCE)
     */
    public Auditorium(int auditoriumId, String name, String roomType,
                      int totalRows, int seatsPerRow, int totalSeats, String status) {
        this.auditoriumId = auditoriumId;
        this.name = name;
        this.roomType = roomType;
        this.totalRows = totalRows;
        this.seatsPerRow = seatsPerRow;
        this.totalSeats = totalSeats;
        this.status = status;
    }

    public int getAuditoriumId() {
        return auditoriumId;
    }

    public void setAuditoriumId(int auditoriumId) {
        this.auditoriumId = auditoriumId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public void setSeatsPerRow(int seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

