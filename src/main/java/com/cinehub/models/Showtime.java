package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn một Suất chiếu phim (Showtime) gắn với Phim và Phòng chiếu cụ thể.
 * Tuân thủ quy tắc BR01: end_time = start_time + duration_minutes + 15 phút dọn vệ sinh.
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class Showtime {

    private int showtimeId;
    private int movieId;
    private String movieTitle;
    private int durationMinutes;
    private String ageRating;
    private int auditoriumId;
    private String auditoriumName;
    private String roomType;
    private String startTime;
    private String endTime;
    private double basePrice;
    private String status;
    private int availableSeats;
    private int totalSeats;

    /**
     * Khởi tạo mặc định đối tượng suất chiếu.
     */
    public Showtime() {
    }

    /**
     * Khởi tạo đầy đủ thông tin một suất chiếu.
     *
     * @param showtimeId     Mã định danh suất chiếu
     * @param movieId        Mã phim
     * @param movieTitle     Tên bộ phim
     * @param auditoriumId   Mã phòng chiếu
     * @param auditoriumName Tên phòng chiếu
     * @param startTime      Thời điểm bắt đầu chiếu (YYYY-MM-DD HH:mm:ss)
     * @param endTime        Thời điểm kết thúc cả dọn vệ sinh (YYYY-MM-DD HH:mm:ss)
     * @param basePrice      Giá vé cơ sở (VNĐ)
     * @param status         Trạng thái suất chiếu (SCHEDULED, CANCELLED, COMPLETED)
     */
    public Showtime(int showtimeId, int movieId, String movieTitle,
                    int auditoriumId, String auditoriumName,
                    String startTime, String endTime, double basePrice, String status) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.auditoriumId = auditoriumId;
        this.auditoriumName = auditoriumName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.status = status;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public int getAuditoriumId() {
        return auditoriumId;
    }

    public void setAuditoriumId(int auditoriumId) {
        this.auditoriumId = auditoriumId;
    }

    public String getAuditoriumName() {
        return auditoriumName;
    }

    public void setAuditoriumName(String auditoriumName) {
        this.auditoriumName = auditoriumName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
}

