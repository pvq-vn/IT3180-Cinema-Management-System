package com.cinehub.services;

import com.cinehub.models.Movie;
import com.cinehub.utils.DatabaseConnection;
import com.cinehub.utils.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp dịch vụ quản lý danh mục Phim chiếu rạp (Phân hệ CSD03).
 * Cung cấp các chức năng tra cứu, lọc, thêm mới, cập nhật và ngừng chiếu phim.
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class MovieService {

    /**
     * Lấy danh sách tất cả các bộ phim có hỗ trợ lọc theo từ khóa và trạng thái.
     *
     * @param keyword      Từ khóa tìm kiếm theo tên phim hoặc thể loại (có thể null)
     * @param statusFilter Bộ lọc trạng thái SHOWING, COMING_SOON, ENDED (có thể null hoặc ALL)
     * @return Danh sách các bộ phim thỏa mãn điều kiện
     */
    public List<Movie> getAllMovies(String keyword, String statusFilter) {
        List<Movie> movies = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT movie_id, title, genre, duration_minutes, age_rating, director, "
                + "cast_list, description, poster_url, release_date, status FROM movies WHERE 1=1 ");
        List<String> params = new ArrayList<>();

        if (!PasswordUtils.isBlank(keyword)) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(genre) LIKE ? OR LOWER(director) LIKE ?) ");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (!PasswordUtils.isBlank(statusFilter) && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            sql.append("AND UPPER(status) = UPPER(?) ");
            params.add(statusFilter.trim());
        }
        sql.append("ORDER BY movie_id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapRowToMovie(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn danh sách phim: " + ex.getMessage(), ex);
        }
        return movies;
    }

    /**
     * Truy xuất thông tin chi tiết một bộ phim theo mã định danh.
     *
     * @param movieId Mã bộ phim
     * @return Đối tượng {@link Movie} hoặc null nếu không tồn tại
     */
    public Movie getMovieById(int movieId) {
        String sql = "SELECT movie_id, title, genre, duration_minutes, age_rating, director, "
                + "cast_list, description, poster_url, release_date, status FROM movies WHERE movie_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMovie(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn chi tiết phim: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Thêm mới hoặc cập nhật thông tin một bộ phim.
     *
     * @param movie Đối tượng phim cần lưu
     * @return Đối tượng {@link Movie} sau khi đã lưu
     * @throws IllegalArgumentException Nếu tên phim rỗng hoặc thời lượng không hợp lệ
     */
    public Movie saveMovie(Movie movie) {
        if (movie == null || PasswordUtils.isBlank(movie.getTitle())) {
            throw new IllegalArgumentException("Tên bộ phim không được để trống.");
        }
        if (movie.getDurationMinutes() < 30 || movie.getDurationMinutes() > 360) {
            throw new IllegalArgumentException("Thời lượng phim phải nằm trong khoảng từ 30 đến 360 phút.");
        }
        String genre = PasswordUtils.isBlank(movie.getGenre()) ? "Hành động / Tâm lý" : movie.getGenre().trim();
        String ageRating = PasswordUtils.isBlank(movie.getAgeRating()) ? "T13" : movie.getAgeRating().trim();
        String status = PasswordUtils.isBlank(movie.getStatus()) ? "SHOWING" : movie.getStatus().trim();
        String releaseDate = PasswordUtils.isBlank(movie.getReleaseDate()) ? "2026-05-20" : movie.getReleaseDate().trim();

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (movie.getMovieId() <= 0) {
                String insertSql = "INSERT INTO movies (title, genre, duration_minutes, age_rating, director, "
                        + "cast_list, description, poster_url, release_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, movie.getTitle().trim());
                    ps.setString(2, genre);
                    ps.setInt(3, movie.getDurationMinutes());
                    ps.setString(4, ageRating);
                    ps.setString(5, movie.getDirector() == null ? "Đang cập nhật" : movie.getDirector().trim());
                    ps.setString(6, movie.getCastList() == null ? "Đang cập nhật" : movie.getCastList().trim());
                    ps.setString(7, movie.getDescription() == null ? "" : movie.getDescription().trim());
                    ps.setString(8, movie.getPosterUrl() == null ? "/posters/default.svg" : movie.getPosterUrl().trim());
                    ps.setString(9, releaseDate);
                    ps.setString(10, status);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            movie.setMovieId(keys.getInt(1));
                        }
                    }
                }
            } else {
                String updateSql = "UPDATE movies SET title = ?, genre = ?, duration_minutes = ?, age_rating = ?, "
                        + "director = ?, cast_list = ?, description = ?, release_date = ?, status = ? WHERE movie_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, movie.getTitle().trim());
                    ps.setString(2, genre);
                    ps.setInt(3, movie.getDurationMinutes());
                    ps.setString(4, ageRating);
                    ps.setString(5, movie.getDirector() == null ? "" : movie.getDirector().trim());
                    ps.setString(6, movie.getCastList() == null ? "" : movie.getCastList().trim());
                    ps.setString(7, movie.getDescription() == null ? "" : movie.getDescription().trim());
                    ps.setString(8, releaseDate);
                    ps.setString(9, status);
                    ps.setInt(10, movie.getMovieId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi lưu thông tin phim: " + ex.getMessage(), ex);
        }
        return getMovieById(movie.getMovieId());
    }

    /**
     * Ngừng chiếu hoặc xóa phim nếu chưa phát sinh lịch chiếu.
     *
     * @param movieId Mã bộ phim cần ngừng chiếu
     * @return Thông báo kết quả xử lý
     */
    public String discontinueOrDeleteMovie(int movieId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int showtimeCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM showtimes WHERE movie_id = ?")) {
                ps.setInt(1, movieId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        showtimeCount = rs.getInt(1);
                    }
                }
            }
            if (showtimeCount > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE movies SET status = 'ENDED' WHERE movie_id = ?")) {
                    ps.setInt(1, movieId);
                    ps.executeUpdate();
                }
                return "Phim đã có " + showtimeCount + " suất chiếu nên được chuyển sang trạng thái Ngừng chiếu (ENDED).";
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM movies WHERE movie_id = ?")) {
                    ps.setInt(1, movieId);
                    ps.executeUpdate();
                }
                return "Đã xóa bộ phim chưa phát sinh suất chiếu khỏi danh mục.";
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi xử lý ngừng chiếu phim: " + ex.getMessage(), ex);
        }
    }

    private Movie mapRowToMovie(ResultSet rs) throws SQLException {
        return new Movie(
                rs.getInt("movie_id"),
                rs.getString("title"),
                rs.getString("genre"),
                rs.getInt("duration_minutes"),
                rs.getString("age_rating"),
                rs.getString("director"),
                rs.getString("cast_list"),
                rs.getString("description"),
                rs.getString("poster_url"),
                rs.getString("release_date") != null ? rs.getString("release_date") : "2026-05-01",
                rs.getString("status")
        );
    }
}

