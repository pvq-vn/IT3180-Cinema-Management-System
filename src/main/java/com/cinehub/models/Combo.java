package com.cinehub.models;

/**
 * Lớp thực thể biểu diễn Sản phẩm Đồ ăn, Thức uống hoặc Combo F&B tại rạp (Phân hệ CSD04).
 *
 * @author Nguyễn Xuân Hồng Quân (202416321)
 * @version 1.0
 */
public class Combo {

    private int comboId;
    private String comboCode;
    private String name;
    private String description;
    private String itemsIncluded;
    private double price;
    private int stockQuantity;
    private String status;

    /**
     * Khởi tạo mặc định đối tượng Combo F&B.
     */
    public Combo() {
    }

    /**
     * Khởi tạo đầy đủ thông tin một Combo F&B.
     *
     * @param comboId       Mã định danh Combo
     * @param comboCode     Mã hiển thị Combo (CB01, CB02...)
     * @param name          Tên gói Combo
     * @param description   Mô tả gói ưu đãi
     * @param itemsIncluded Thành phần chi tiết (Bắp + Nước)
     * @param price         Đơn giá bán (VNĐ)
     * @param stockQuantity Số lượng tồn kho khả dụng
     * @param status        Trạng thái kinh doanh (AVAILABLE, OUT_OF_STOCK, DISCONTINUED)
     */
    public Combo(int comboId, String comboCode, String name, String description,
                 String itemsIncluded, double price, int stockQuantity, String status) {
        this.comboId = comboId;
        this.comboCode = comboCode;
        this.name = name;
        this.description = description;
        this.itemsIncluded = itemsIncluded;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = status;
    }

    public int getComboId() {
        return comboId;
    }

    public void setComboId(int comboId) {
        this.comboId = comboId;
    }

    public String getComboCode() {
        return comboCode;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItemsIncluded() {
        return itemsIncluded;
    }

    public void setItemsIncluded(String itemsIncluded) {
        this.itemsIncluded = itemsIncluded;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

