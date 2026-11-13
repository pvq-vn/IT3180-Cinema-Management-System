package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn thông tin một Bộ phim chiếu rạp (Phân hệ CSD03).
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class Movie {

    private int movieId;
    private String title;
    private String genre;
    private int durationMinutes;
    private String ageRating;
    private String director;
    private String castList;
    private String description;
    private String posterUrl;
    private String releaseDate;
    private String status;

    /**
     * Khởi tạo mặc định đối tượng phim.
     */
    public Movie() {
    }

    /**
     * Khởi tạo đầy đủ thông tin một bộ phim.
     *
     * @param movieId         Mã định danh phim
     * @param title           Tên bộ phim
     * @param genre           Thể loại phim
     * @param durationMinutes Thời lượng phim (phút)
     * @param ageRating       Giới hạn độ tuổi (P, T13, T16, T18)
     * @param director        Đạo diễn
     * @param castList        Danh sách diễn viên chính
     * @param description     Tóm tắt nội dung phim
     * @param posterUrl       Đường dẫn ảnh áp phích
     * @param releaseDate     Ngày khởi chiếu
     * @param status          Trạng thái (SHOWING, COMING_SOON, ENDED)
     */
    public Movie(int movieId, String title, String genre, int durationMinutes,
                 String ageRating, String director, String castList,
                 String description, String posterUrl, String releaseDate, String status) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.ageRating = ageRating;
        this.director = director;
        this.castList = castList;
        this.description = description;
        this.posterUrl = posterUrl;
        this.releaseDate = releaseDate;
        this.status = status;
    }

    /**
     * Lấy mã định danh phim.
     *
     * @return Mã phim
     */
    public int getMovieId() {
        return movieId;
    }

    /**
     * Thiết lập mã định danh phim.
     *
     * @param movieId Mã phim
     */
    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    /**
     * Lấy tên bộ phim.
     *
     * @return Tên phim
     */
    public String getTitle() {
        return title;
    }

    /**
     * Thiết lập tên bộ phim.
     *
     * @param title Tên phim
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Lấy thể loại phim.
     *
     * @return Thể loại phim
     */
    public String getGenre() {
        return genre;
    }

    /**
     * Thiết lập thể loại phim.
     *
     * @param genre Thể loại phim
     */
    public void setGenre(String genre) {
        this.genre = genre;
    }

    /**
     * Lấy thời lượng phim tính theo phút.
     *
     * @return Thời lượng (phút)
     */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * Thiết lập thời lượng phim tính theo phút.
     *
     * @param durationMinutes Thời lượng (phút)
     */
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * Lấy nhãn phân loại độ tuổi.
     *
     * @return Nhãn độ tuổi (P, T13, T16, T18)
     */
    public String getAgeRating() {
        return ageRating;
    }

    /**
     * Thiết lập nhãn phân loại độ tuổi.
     *
     * @param ageRating Nhãn độ tuổi
     */
    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    /**
     * Lấy tên đạo diễn.
     *
     * @return Tên đạo diễn
     */
    public String getDirector() {
        return director;
    }

    /**
     * Thiết lập tên đạo diễn.
     *
     * @param director Tên đạo diễn
     */
    public void setDirector(String director) {
        this.director = director;
    }

    /**
     * Lấy danh sách diễn viên chính.
     *
     * @return Danh sách diễn viên
     */
    public String getCastList() {
        return castList;
    }

    /**
     * Thiết lập danh sách diễn viên chính.
     *
     * @param castList Danh sách diễn viên
     */
    public void setCastList(String castList) {
        this.castList = castList;
    }

    /**
     * Lấy mô tả nội dung phim.
     *
     * @return Mô tả phim
     */
    public String getDescription() {
        return description;
    }

    /**
     * Thiết lập mô tả nội dung phim.
     *
     * @param description Mô tả phim
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Lấy đường dẫn ảnh áp phích.
     *
     * @return Đường dẫn poster
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Thiết lập đường dẫn ảnh áp phích.
     *
     * @param posterUrl Đường dẫn poster
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Lấy ngày khởi chiếu.
     *
     * @return Chuỗi ngày khởi chiếu (YYYY-MM-DD)
     */
    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Thiết lập ngày khởi chiếu.
     *
     * @param releaseDate Chuỗi ngày khởi chiếu
     */
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    /**
     * Lấy trạng thái công chiếu của phim.
     *
     * @return Trạng thái (SHOWING, COMING_SOON, ENDED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Thiết lập trạng thái công chiếu của phim.
     *
     * @param status Trạng thái
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
