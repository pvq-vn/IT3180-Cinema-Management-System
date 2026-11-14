package com.cinehub;

import com.cinehub.controllers.AuthController;
import com.cinehub.controllers.BookingController;
import com.cinehub.controllers.ComboController;
import com.cinehub.controllers.MovieController;
import com.cinehub.controllers.ReportController;
import com.cinehub.controllers.ShowtimeController;
import com.cinehub.utils.AppConfig;
import com.cinehub.utils.DatabaseConnection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.concurrent.Executors;

/**
 * Điểm khởi chạy chính của Hệ thống Website Quản lý và Đặt vé Rạp chiếu phim CineHub (CMS).
 * Thiết kế theo kiến trúc Web MVC (Model - View - Controller - Service) chuẩn học phần IT3180.
 *
 * @author Phạm Văn Quyết (202416331)
 * @author Vũ Việt Tiến (202416362)
 * @author Hà Sỹ Bách (202416129)
 * @author Lê Anh Tuấn (202416639)
 * @author Nguyễn Xuân Hồng Quân (202416321)
 * @version 1.0
 */
public class CineHubApplication {

    /** Cổng mặc định của máy chủ HTTP Web MVC. */
    public static final int DEFAULT_PORT = 8080;

    private static HttpServer server;

    /**
     * Phương thức main khởi động Cơ sở dữ liệu và Máy chủ Web MVC.
     *
     * @param args Tham số dòng lệnh (truyền cổng hoặc --verify để kiểm tra khởi tạo nhanh)
     * @throws Exception Nếu không thể mở cổng mạng hoặc khởi tạo CSDL
     */
    public static void main(String[] args) throws Exception {
        int port = AppConfig.getInt("server.port", DEFAULT_PORT);
        boolean verifyOnly = false;
        if (args != null) {
            for (String arg : args) {
                if ("--verify".equalsIgnoreCase(arg)) {
                    verifyOnly = true;
                } else if (arg.matches("\\d+")) {
                    port = Integer.parseInt(arg);
                }
            }
        }

        // Khởi tạo CSDL và nạp dữ liệu mẫu chuẩn 3NF
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("======================================================================");
            System.out.println("  CINEHUB CMS - HỆ THỐNG WEBSITE QUẢN LÝ & ĐẶT VÉ RẠP CHIẾU PHIM");
            System.out.println("  Học phần: IT3180 - Nhập môn Công nghệ Phần mềm (Nhóm 3 - HUST)");
            System.out.println("  Giảng viên hướng dẫn: TS. Nguyễn Nhất Hải");
            System.out.println("======================================================================");
            System.out.println("[DB] Đã khởi tạo thành công CSDL quan hệ (10 bảng + dữ liệu mẫu).");
        }

        startServer(port);
        System.out.println("[WEB] Máy chủ Web MVC đã sẵn sàng tại: http://localhost:" + port);
        System.out.println("[RBAC] Tài khoản dùng thử:");
        System.out.println("  - Quản lý rạp (MANAGER) : admin / admin123");
        System.out.println("  - Nhân viên POS/Soát vé : staff / staff123");
        System.out.println("  - Khách hàng thành viên : customer / khach123");

        if (verifyOnly) {
            System.out.println("[VERIFY] Kiểm tra khởi động máy chủ thành công. Đang đóng máy chủ...");
            stopServer();
        }
    }

    /**
     * Khởi chạy máy chủ HTTP nhúng trên cổng chỉ định và đăng ký toàn bộ định tuyến MVC.
     *
     * @param port Cổng lắng nghe HTTP
     * @throws IOException Nếu cổng bị chiếm dụng
     */
    public static synchronized void startServer(int port) throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);

        MovieController movieController = new MovieController();
        server.createContext("/api/auth", new AuthController());
        server.createContext("/api/movies", movieController);
        server.createContext("/api/auditoriums", movieController);
        server.createContext("/api/showtimes", new ShowtimeController());
        server.createContext("/api/combos", new ComboController());
        server.createContext("/api/booking", new BookingController());
        server.createContext("/api/reports", new ReportController());
        server.createContext("/", new StaticViewHandler());

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    /**
     * Dừng máy chủ HTTP nhúng.
     */
    public static synchronized void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /**
     * Bộ phục vụ tầng View (HTML5, CSS3, JavaScript) từ thư mục resources.
     */
    private static class StaticViewHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if ("/".equals(uriPath) || "/index.html".equals(uriPath)) {
                serveResource(exchange, "templates/index.html", "text/html; charset=UTF-8");
            } else if ("/css/style.css".equals(uriPath)) {
                serveResource(exchange, "static/css/style.css", "text/css; charset=UTF-8");
            } else if ("/js/app.js".equals(uriPath)) {
                serveResource(exchange, "static/js/app.js", "application/javascript; charset=UTF-8");
            } else {
                byte[] notFound = "404 Not Found - CineHub CMS".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound);
                }
            }
        }

        private void serveResource(HttpExchange exchange, String resourceRelPath,
                                   String contentType) throws IOException {
            byte[] content = loadResourceBytes(resourceRelPath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }

        private byte[] loadResourceBytes(String resourceRelPath) throws IOException {
            Path p1 = Paths.get("src/main/resources", resourceRelPath);
            if (Files.exists(p1)) {
                return Files.readAllBytes(p1);
            }
            Path p2 = Paths.get("03-SourceCodes/src/main/resources", resourceRelPath);
            if (Files.exists(p2)) {
                return Files.readAllBytes(p2);
            }
            try (InputStream is = CineHubApplication.class.getClassLoader()
                    .getResourceAsStream(resourceRelPath)) {
                if (is != null) {
                    return is.readAllBytes();
                }
            }
            return ("Missing resource: " + resourceRelPath).getBytes(StandardCharsets.UTF_8);
        }
    }
}
