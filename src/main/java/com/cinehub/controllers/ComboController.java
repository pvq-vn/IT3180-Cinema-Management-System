package com.cinehub.controllers;

import com.cinehub.models.Combo;
import com.cinehub.services.ComboService;
import com.cinehub.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Bộ điều khiển xử lý các yêu cầu Quản lý Đồ ăn, Thức uống và Combo F&B (/api/combos).
 * Thuộc Phân hệ CSD04.
 *
 * @author Nguyễn Xuân Hồng Quân (202416321)
 * @version 1.0
 */
public class ComboController extends BaseController implements HttpHandler {

    private final ComboService comboService = new ComboService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> params = parseQueryParams(exchange);
                boolean activeOnly = "true".equalsIgnoreCase(params.get("activeOnly"));
                List<Combo> combos = comboService.getAllCombos(activeOnly);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < combos.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    Combo c = combos.get(i);
                    sb.append(comboToJson(c));
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                // RBAC: Quản lý rạp và Nhân viên quầy F&B được cập nhật danh mục Combo
                if (requireRole(exchange, ROLE_MANAGER, ROLE_STAFF) == null) {
                    return;
                }
                Map<String, String> body = JsonUtils.parseFlatObject(readRequestBody(exchange));
                String action = body.getOrDefault("action", "save");
                if ("toggle".equalsIgnoreCase(action)) {
                    int comboId = Integer.parseInt(body.getOrDefault("comboId", "0"));
                    String msg = comboService.toggleComboStatus(comboId);
                    sendJson(exchange, 200, JsonUtils.statusResponse(true, "TOGGLED", msg));
                    return;
                }
                Combo c = new Combo();
                c.setComboId(Integer.parseInt(body.getOrDefault("comboId", "0")));
                c.setComboCode(body.get("comboCode"));
                c.setName(body.get("name"));
                c.setDescription(body.get("description"));
                c.setItemsIncluded(body.get("itemsIncluded"));
                c.setPrice(Double.parseDouble(body.getOrDefault("price", "0")));
                c.setStockQuantity(Integer.parseInt(body.getOrDefault("stockQuantity", "0")));
                c.setStatus(body.get("status"));

                Combo saved = comboService.saveCombo(c);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Đã lưu gói Combo '"
                        + JsonUtils.escape(saved.getName()) + "' thành công!\",\"combo\":" + comboToJson(saved) + "}");
            } else {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
            }
        } catch (Exception ex) {
            handleException(exchange, ex);
        }
    }

    private String comboToJson(Combo c) {
        return "{\"comboId\":" + c.getComboId()
                + ",\"comboCode\":\"" + JsonUtils.escape(c.getComboCode()) + "\""
                + ",\"name\":\"" + JsonUtils.escape(c.getName()) + "\""
                + ",\"description\":\"" + JsonUtils.escape(c.getDescription()) + "\""
                + ",\"itemsIncluded\":\"" + JsonUtils.escape(c.getItemsIncluded()) + "\""
                + ",\"price\":" + c.getPrice()
                + ",\"stockQuantity\":" + c.getStockQuantity()
                + ",\"status\":\"" + JsonUtils.escape(c.getStatus()) + "\"}";
    }
}

