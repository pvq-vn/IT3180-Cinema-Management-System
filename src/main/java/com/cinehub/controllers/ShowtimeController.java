package com.cinehub.controllers;

import com.cinehub.models.Showtime;
import com.cinehub.services.ShowtimeService;
import com.cinehub.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Bộ điều khiển xử lý các yêu cầu Tra cứu và Lập lịch chiếu phim (/api/showtimes).
 * Thực thi Quy tắc nghiệp vụ BR01 chống chồng lấn thời gian chiếu trong cùng phòng.
 *
 * @author Vũ Việt Tiến (202416362)
 * @version 1.0
 */
public class ShowtimeController extends BaseController implements HttpHandler {

    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> params = parseQueryParams(exchange);
                Integer movieId = params.containsKey("movieId") && !params.get("movieId").isEmpty()
                        ? Integer.parseInt(params.get("movieId")) : null;
                String dateFilter = params.get("date");

                List<Showtime> list = showtimeService.getShowtimes(movieId, dateFilter);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    Showtime s = list.get(i);
                    sb.append(showtimeToJson(s));
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                int movieId = Integer.parseInt(body.getOrDefault("movieId", "0"));
                int auditoriumId = Integer.parseInt(body.getOrDefault("auditoriumId", "0"));
                String startTime = body.get("startTime");
                double basePrice = Double.parseDouble(body.getOrDefault("basePrice", "0"));

                ShowtimeService.ScheduleResult res =
                        showtimeService.createShowtime(movieId, auditoriumId, startTime, basePrice);
                if (!res.isSuccess()) {
                    sendJson(exchange, 200, JsonUtils.statusResponse(false, res.getCode(), res.getMessage()));
                    return;
                }
                sendJson(exchange, 200, "{\"success\":true,\"code\":\"" + JsonUtils.escape(res.getCode())
                        + "\",\"message\":\"" + JsonUtils.escape(res.getMessage())
                        + "\",\"showtime\":" + showtimeToJson(res.getShowtime()) + "}");
            } else {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
            }
        } catch (Exception ex) {
            sendError(exchange, 200, "SCHEDULE_ERROR", ex.getMessage());
        }
    }

    private String showtimeToJson(Showtime s) {
        return "{\"showtimeId\":" + s.getShowtimeId()
                + ",\"movieId\":" + s.getMovieId()
                + ",\"movieTitle\":\"" + JsonUtils.escape(s.getMovieTitle()) + "\""
                + ",\"durationMinutes\":" + s.getDurationMinutes()
                + ",\"ageRating\":\"" + JsonUtils.escape(s.getAgeRating()) + "\""
                + ",\"auditoriumId\":" + s.getAuditoriumId()
                + ",\"auditoriumName\":\"" + JsonUtils.escape(s.getAuditoriumName()) + "\""
                + ",\"roomType\":\"" + JsonUtils.escape(s.getRoomType()) + "\""
                + ",\"startTime\":\"" + JsonUtils.escape(s.getStartTime()) + "\""
                + ",\"endTime\":\"" + JsonUtils.escape(s.getEndTime()) + "\""
                + ",\"basePrice\":" + s.getBasePrice()
                + ",\"status\":\"" + JsonUtils.escape(s.getStatus()) + "\""
                + ",\"availableSeats\":" + s.getAvailableSeats()
                + ",\"totalSeats\":" + s.getTotalSeats() + "}";
    }
}

