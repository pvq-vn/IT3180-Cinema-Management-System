package com.cinehub.controllers;

import com.cinehub.models.Auditorium;
import com.cinehub.models.Movie;
import com.cinehub.services.AuditoriumService;
import com.cinehub.services.MovieService;
import com.cinehub.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Bộ điều khiển xử lý các yêu cầu Quản lý Danh mục Phim và Phòng chiếu (/api/movies, /api/auditoriums).
 * Thuộc Phân hệ CSD03.
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class MovieController extends BaseController implements HttpHandler {

    private final MovieService movieService = new MovieService();
    private final AuditoriumService auditoriumService = new AuditoriumService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.startsWith("/api/auditoriums") && "GET".equalsIgnoreCase(method)) {
                List<Auditorium> list = auditoriumService.getAllAuditoriums();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    Auditorium a = list.get(i);
                    sb.append("{\"auditoriumId\":").append(a.getAuditoriumId())
                            .append(",\"name\":\"").append(JsonUtils.escape(a.getName())).append("\"")
                            .append(",\"roomType\":\"").append(JsonUtils.escape(a.getRoomType())).append("\"")
                            .append(",\"totalRows\":").append(a.getTotalRows())
                            .append(",\"seatsPerRow\":").append(a.getSeatsPerRow())
                            .append(",\"totalSeats\":").append(a.getTotalSeats())
                            .append(",\"status\":\"").append(JsonUtils.escape(a.getStatus())).append("\"}");
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> params = parseQueryParams(exchange);
                List<Movie> movies = movieService.getAllMovies(params.get("keyword"), params.get("status"));
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < movies.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    Movie m = movies.get(i);
                    sb.append(movieToJson(m));
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                // RBAC: chỉ Quản lý rạp được thêm/sửa/ngừng chiếu phim
                if (requireRole(exchange, ROLE_MANAGER) == null) {
                    return;
                }
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                String action = body.getOrDefault("action", "save");
                if ("delete".equalsIgnoreCase(action)) {
                    int movieId = Integer.parseInt(body.getOrDefault("movieId", "0"));
                    String msg = movieService.discontinueOrDeleteMovie(movieId);
                    sendJson(exchange, 200, JsonUtils.statusResponse(true, "UPDATED", msg));
                    return;
                }
                Movie m = new Movie();
                m.setMovieId(Integer.parseInt(body.getOrDefault("movieId", "0")));
                m.setTitle(body.get("title"));
                m.setGenre(body.get("genre"));
                m.setDurationMinutes(Integer.parseInt(body.getOrDefault("durationMinutes", "120")));
                m.setAgeRating(body.getOrDefault("ageRating", "T13"));
                m.setDirector(body.getOrDefault("director", ""));
                m.setCastList(body.getOrDefault("castList", ""));
                m.setDescription(body.getOrDefault("description", ""));
                m.setReleaseDate(body.getOrDefault("releaseDate", "2026-05-20"));
                m.setStatus(body.getOrDefault("status", "SHOWING"));

                Movie saved = movieService.saveMovie(m);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Đã lưu thông tin phim '"
                        + JsonUtils.escape(saved.getTitle()) + "' thành công!\",\"movie\":" + movieToJson(saved) + "}");
            } else {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
            }
        } catch (Exception ex) {
            handleException(exchange, ex);
        }
    }

    private String movieToJson(Movie m) {
        return "{\"movieId\":" + m.getMovieId()
                + ",\"title\":\"" + JsonUtils.escape(m.getTitle()) + "\""
                + ",\"genre\":\"" + JsonUtils.escape(m.getGenre()) + "\""
                + ",\"durationMinutes\":" + m.getDurationMinutes()
                + ",\"ageRating\":\"" + JsonUtils.escape(m.getAgeRating()) + "\""
                + ",\"director\":\"" + JsonUtils.escape(m.getDirector()) + "\""
                + ",\"castList\":\"" + JsonUtils.escape(m.getCastList()) + "\""
                + ",\"description\":\"" + JsonUtils.escape(m.getDescription()) + "\""
                + ",\"posterUrl\":\"" + JsonUtils.escape(m.getPosterUrl()) + "\""
                + ",\"releaseDate\":\"" + JsonUtils.escape(m.getReleaseDate()) + "\""
                + ",\"status\":\"" + JsonUtils.escape(m.getStatus()) + "\"}";
    }
}

