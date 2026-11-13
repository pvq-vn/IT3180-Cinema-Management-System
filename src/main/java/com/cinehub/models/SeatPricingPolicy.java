package com.cinehub.models;

/**
 * Lớp trừu tượng định nghĩa Chính sách tính giá vé theo hạng ghế (Quy tắc BR05).
 * Áp dụng kỹ thuật Tái cấu trúc mã nguồn (Refactoring - Chương 8 IT3180):
 * 1. Replace Conditional with Polymorphism (Thay thế cấu trúc rẽ nhánh switch-case bằng Đa hình).
 * 2. Introduce Null Object (Giới thiệu Đối tượng Rỗng cho lựa chọn Combo không tồn tại).
 *
 * @author Hà Sỹ Bách (202416129)
 * @version 1.0
 */
public abstract class SeatPricingPolicy {

    /** Phụ thu cố định cho ghế hạng VIP (+20.000 VNĐ). */
    public static final double VIP_SURCHARGE = 20000.0;

    /** Hệ số nhân giá cho ghế đôi Couple (2 ghế liền kề + phụ thu tiện ích = 2.2x giá cơ sở). */
    public static final double COUPLE_MULTIPLIER = 2.2;

    /**
     * Tính giá vé cuối cùng cho một ghế dựa trên giá cơ sở của suất chiếu (BR05).
     *
     * @param basePrice Giá cơ sở của suất chiếu (VNĐ)
     * @return Giá vé cuối cùng của ghế (VNĐ)
     */
    public abstract double calculateSeatPrice(double basePrice);

    /**
     * Lấy nhãn mô tả hạng ghế hiển thị trên vé.
     *
     * @return Chuỗi mô tả hạng ghế
     */
    public abstract String getSeatTierLabel();

    /**
     * Phương thức khởi tạo (Factory Method) trả về đối tượng chiến lược giá phù hợp theo mã hạng ghế.
     *
     * @param seatType Mã hạng ghế (STANDARD, VIP, COUPLE)
     * @return Đối tượng thực thi {@link SeatPricingPolicy}
     */
    public static SeatPricingPolicy forSeatType(String seatType) {
        if (seatType == null) {
            return new StandardSeatPricing();
        }
        String normalized = seatType.trim().toUpperCase();
        if ("VIP".equals(normalized)) {
            return new VipSeatPricing();
        }
        if ("COUPLE".equals(normalized)) {
            return new CoupleSeatPricing();
        }
        return new StandardSeatPricing();
    }

    /**
     * Chính sách giá cho ghế Tiêu chuẩn (STANDARD): Giữ nguyên giá cơ sở.
     */
    public static class StandardSeatPricing extends SeatPricingPolicy {

        @Override
        public double calculateSeatPrice(double basePrice) {
            return Math.round(basePrice);
        }

        @Override
        public String getSeatTierLabel() {
            return "Ghế Thường (Standard)";
        }
    }

    /**
     * Chính sách giá cho ghế VIP: Giá cơ sở cộng phụ thu khu vực trung tâm (+20.000 VNĐ).
     */
    public static class VipSeatPricing extends SeatPricingPolicy {

        @Override
        public double calculateSeatPrice(double basePrice) {
            return Math.round(basePrice + VIP_SURCHARGE);
        }

        @Override
        public String getSeatTierLabel() {
            return "Ghế VIP Trung Tâm (+20.000đ)";
        }
    }

    /**
     * Chính sách giá cho ghế Đôi (COUPLE): Bằng 2.2 lần giá cơ sở (dành cho 2 khách).
     */
    public static class CoupleSeatPricing extends SeatPricingPolicy {

        @Override
        public double calculateSeatPrice(double basePrice) {
            return Math.round((basePrice * COUPLE_MULTIPLIER) / 1000.0) * 1000.0;
        }

        @Override
        public String getSeatTierLabel() {
            return "Ghế Đôi Sweetbox (2.2x)";
        }
    }

    /**
     * Biểu diễn một mục chọn Combo trong giỏ hàng thanh toán.
     * Minh họa mẫu thiết kế Introduce Null Object (Chương 8 - Refactoring) để tránh kiểm tra null.
     */
    public static class ComboOrderItem {

        private final Combo combo;
        private final int quantity;

        /**
         * Khởi tạo mục chọn Combo hợp lệ.
         *
         * @param combo    Đối tượng Combo
         * @param quantity Số lượng đặt mua
         */
        public ComboOrderItem(Combo combo, int quantity) {
            this.combo = combo;
            this.quantity = Math.max(0, quantity);
        }

        /**
         * Kiểm tra có phải đối tượng rỗng (Null Object) hay không.
         *
         * @return true nếu không có Combo nào được chọn
         */
        public boolean isNull() {
            return false;
        }

        /**
         * Tính thành tiền của mục chọn Combo (Đơn giá * Số lượng).
         *
         * @return Thành tiền (VNĐ)
         */
        public double getSubtotal() {
            return combo.getPrice() * quantity;
        }

        public Combo getCombo() {
            return combo;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    /**
     * Đối tượng Rỗng (NullComboOrderItem) đại diện cho trường hợp khách hàng không mua kèm F&B.
     */
    public static class NullComboOrderItem extends ComboOrderItem {

        /**
         * Khởi tạo đối tượng Combo rỗng.
         */
        public NullComboOrderItem() {
            super(new Combo(0, "NONE", "Không chọn Combo", "", "", 0.0, 0, "AVAILABLE"), 0);
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public double getSubtotal() {
            return 0.0;
        }
    }
}

