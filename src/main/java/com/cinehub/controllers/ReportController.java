package com.cinehub.controllers;

import com.cinehub.services.ReportService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Bộ điều khiển xử lý các yêu cầu Thống kê và Báo cáo doanh thu (/api/reports/dashboard).
 * Thuộc Phân hệ CSD05.
 *
 * @author Phạm Văn Quyết (202416331)
 * @version 1.0
 */
public class ReportController extends BaseController implements HttpHandler {

    private final ReportService reportService = new ReportService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Chỉ hỗ trợ phương thức GET.");
            return;
        }
        // RBAC: báo cáo doanh thu chỉ dành cho Quản lý rạp
        if (requireRole(exchange, ROLE_MANAGER) == null) {
            return;
        }
        try {
            String json = reportService.buildDashboardReportJson();
            sendJson(exchange, 200, json);
        } catch (Exception ex) {
            handleException(exchange, ex);
        }
    }
}

