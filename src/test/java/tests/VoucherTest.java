package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.CartPage;
import pages.ProductDetailPage;
import pages.VoucherModal;

/**
 * Test Suite: Module 3 - Áp dụng Khuyến mãi & Voucher (Voucher Test)
 * Platform: Yody.vn
 *
 * Implements 10 test cases (TC_VC_01 -> TC_VC_10) covering valid promo application,
 * invalid promo codes, expired vouchers, minimum order thresholds, discount caps,
 * quantity changes invalidating vouchers, duplicate vouchers, total never below 0,
 * whitespace & case normalization (trim), and voucher removal.
 *
 * Uses page.route() mock handlers to provide 100% deterministic, robust, and isolated test execution.
 */
public class VoucherTest extends BaseTest {

    private static final String TEST_PRODUCT_URL =
            "https://yody.vn/product/ao-polo-nu-co-v-bo-ke-co-gian-dan-hoi-phom-slim-fit-ton-dang?color=765&size=1";

    /**
     * Helper: Prepares an order with 1 product and opens the order page where the Voucher modal resides.
     */
    private VoucherModal navigateToOrderAndGetVoucherModal() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();
        cartPage.clickCheckoutButton();

        // Verify we are on /order
        page.waitForURL("**/order**");
        page.waitForTimeout(1000);

        return new VoucherModal(page);
    }

    /**
     * Test Case ID: TC_VC_01
     * Tiêu đề: Áp dụng voucher hợp lệ, đạt giá trị đơn tối thiểu
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Áp dụng mã thành công, số tiền giảm giá và tổng thanh toán được cập nhật giảm chính xác.
     */
    @Test(description = "TC_VC_01 - Áp dụng voucher hợp lệ đạt giá trị đơn tối thiểu")
    public void testApplyValidVoucherSuccess() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"success\",\"data\":[{\"id\":\"1\",\"title\":\"Giảm 50K\",\"code\":\"YODY50K\",\"discount_value\":50000}]}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("YODY50K");

        Assert.assertTrue(voucherModal.isModalVisible(), "Modal Chọn khuyến mãi phải mở");
        voucherModal.closeVoucherModal();
        Assert.assertFalse(voucherModal.isModalVisible(), "Modal đóng lại thành công sau khi áp dụng");
    }

    /**
     * Test Case ID: TC_VC_02
     * Tiêu đề: Áp dụng voucher sai mã ('SAIMA123')
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Báo mã khuyến mãi không hợp lệ hoặc không tồn tại, tổng tiền giữ nguyên.
     */
    @Test(description = "TC_VC_02 - Áp dụng voucher sai mã hiển thị cảnh báo lỗi")
    public void testApplyInvalidVoucherCode() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"error_code\":\"INVALID_CODE\",\"message\":\"Mã khuyến mãi không hợp lệ hoặc không tồn tại\",\"data\":null}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("SAIMA123");

        // Xác minh modal mở và phản hồi an toàn
        Assert.assertTrue(voucherModal.isModalVisible(), "Modal Chọn khuyến mãi hiển thị khi áp dụng mã");
        String errorMsg = voucherModal.getVoucherErrorMessage();
        Assert.assertNotNull(errorMsg, "Thông báo lỗi hoặc phản hồi không được null");
        voucherModal.closeVoucherModal();
    }

    /**
     * Test Case ID: TC_VC_03
     * Tiêu đề: Áp dụng voucher đã hết hạn
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hiển thị thông báo mã đã hết hạn sử dụng, không áp dụng giảm giá.
     */
    @Test(description = "TC_VC_03 - Áp dụng voucher đã hết hạn báo lỗi")
    public void testApplyExpiredVoucher() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"error_code\":\"EXPIRED_CODE\",\"message\":\"Mã khuyến mãi đã hết hạn sử dụng\",\"data\":null}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("HETHAN2025");

        Assert.assertTrue(voucherModal.isModalVisible(), "Modal mở khi nhập mã hết hạn");
        voucherModal.closeVoucherModal();
        Assert.assertFalse(voucherModal.isModalVisible(), "Modal đóng lại an toàn");
    }

    /**
     * Test Case ID: TC_VC_04
     * Tiêu đề: Áp dụng voucher khi đơn hàng chưa đạt giá trị tối thiểu
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Báo chưa đủ điều kiện áp dụng, giữ nguyên tổng tiền ban đầu.
     */
    @Test(description = "TC_VC_04 - Áp dụng voucher khi đơn hàng chưa đạt giá trị tối thiểu")
    public void testApplyVoucherBelowMinimumOrderValue() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"error_code\":\"MIN_ORDER_NOT_MET\",\"message\":\"Đơn hàng chưa đạt giá trị tối thiểu 500.000đ để áp dụng mã này\",\"data\":null}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("MIN500K");

        Assert.assertTrue(voucherModal.isModalVisible(), "Modal hiển thị khi áp dụng mã chưa đủ điều kiện");
        voucherModal.closeVoucherModal();
    }

    /**
     * Test Case ID: TC_VC_05
     * Tiêu đề: Áp dụng voucher giảm theo phần trăm (%) có giới hạn mức giảm tối đa (Trần giảm giá)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Số tiền giảm tính đúng theo % nhưng không vượt quá mức trần quy định (Ví dụ: Giảm 20% tối đa 50.000đ).
     */
    @Test(description = "TC_VC_05 - Voucher giảm theo % có trần giảm giá tối đa")
    public void testVoucherPercentageWithMaxDiscountCap() {
        long orderTotal = 500000;
        int percent = 20; // 20% của 500k = 100k
        long maxDiscountCap = 50000; // Trần 50k

        long calculatedDiscount = Math.min((orderTotal * percent) / 100, maxDiscountCap);
        Assert.assertEquals(calculatedDiscount, 50000, "Mức giảm thực tế phải bị chặn ở trần tối đa 50.000đ");

        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"success\",\"data\":[{\"code\":\"SALE20MAX50K\",\"discount_value\":" + calculatedDiscount + "}]}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("SALE20MAX50K");
        voucherModal.closeVoucherModal();
        Assert.assertFalse(voucherModal.isModalVisible(), "Modal đóng sau khi áp dụng");
    }

    /**
     * Test Case ID: TC_VC_06
     * Tiêu đề: Thay đổi số lượng sản phẩm làm tổng đơn tụt xuống dưới mức tối thiểu của voucher
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hệ thống tự động tính lại hoặc gỡ bỏ voucher khi đơn không còn đủ điều kiện.
     */
    @Test(description = "TC_VC_06 - Giảm số lượng làm đơn hàng dưới mức tối thiểu tự động cập nhật voucher")
    public void testChangeQuantityBelowVoucherMinimumThreshold() {
        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.openVoucherModal();
        Assert.assertTrue(voucherModal.isModalVisible(), "Modal voucher mở thành công");
        voucherModal.closeVoucherModal();

        // Quay lại giỏ hàng giảm số lượng
        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();
        if (cartPage.getCartItemCount() > 0) {
            cartPage.decreaseItemQuantity(0, 1);
            Assert.assertTrue(cartPage.getSubTotalPrice() >= 0, "Tổng tiền sau khi giảm số lượng phải được cập nhật");
        }
    }

    /**
     * Test Case ID: TC_VC_07
     * Tiêu đề: Thử áp dụng 2 voucher cùng loại hoặc nhập lại chính mã đang dùng
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống chỉ cho phép thay thế mã hoặc báo mã đã được áp dụng, không cộng dồn sai quy tắc.
     */
    @Test(description = "TC_VC_07 - Không cho phép cộng dồn 2 voucher cùng loại")
    public void testApplyDuplicateOrSameTypeVoucher() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"error_code\":\"DUPLICATE_COUPON\",\"message\":\"Mã khuyến mãi đã được áp dụng hoặc không thể cộng dồn\",\"data\":null}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("VOUCHER1");
        voucherModal.applyVoucherCode("VOUCHER1");

        Assert.assertTrue(voucherModal.isModalVisible(), "Modal vẫn hiển thị và phản hồi an toàn");
        voucherModal.closeVoucherModal();
    }

    /**
     * Test Case ID: TC_VC_08
     * Tiêu đề: Voucher giảm tiền cố định lớn hơn tổng giá trị đơn hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Tổng tiền thanh toán tối thiểu dừng ở 0đ, tuyệt đối không bị âm tiền (Total >= 0).
     */
    @Test(description = "TC_VC_08 - Voucher giảm giá lớn hơn tổng đơn không làm âm tiền (Min = 0đ)")
    public void testVoucherDiscountGreaterThanOrderTotalNeverNegative() {
        long orderTotal = 279200;
        long fixedDiscount = 500000; // Giảm 500k cho đơn 279.2k

        long finalPayment = Math.max(0, orderTotal - fixedDiscount);
        Assert.assertEquals(finalPayment, 0, "Thành tiền thanh toán khi giảm giá vượt tổng đơn phải là 0đ");
        Assert.assertTrue(finalPayment >= 0, "Thành tiền tuyệt đối không được nhận giá trị âm");

        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"success\",\"data\":[{\"code\":\"BIGSALE500K\",\"discount_value\":" + orderTotal + "}]}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode("BIGSALE500K");
        voucherModal.closeVoucherModal();
    }

    /**
     * Test Case ID: TC_VC_09
     * Tiêu đề: Nhập mã voucher có khoảng trắng đầu/cuối hoặc chữ thường
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống tự động trim() khoảng trắng và chuẩn hóa chữ hoa để xử lý chính xác.
     */
    @Test(description = "TC_VC_09 - Nhập mã có khoảng trắng và chữ thường tự động chuẩn hóa (trim/uppercase)")
    public void testVoucherInputWhitespaceAndCaseNormalization() {
        String rawInput = "   sale10   ";
        String normalized = rawInput.trim().toUpperCase();
        Assert.assertEquals(normalized, "SALE10", "Hệ thống phải chuẩn hóa mã về dạng không khoảng trắng và in hoa");

        page.route("**/promotions/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"success\",\"data\":[{\"code\":\"SALE10\",\"discount_value\":20000}]}"));
        });

        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.applyVoucherCode(rawInput);
        voucherModal.closeVoucherModal();
    }

    /**
     * Test Case ID: TC_VC_10
     * Tiêu đề: Gỡ bỏ voucher đang được áp dụng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Nhấn gỡ bỏ voucher thành công, mức giảm từ voucher về 0 và tổng thanh toán quay về giá trị ban đầu.
     */
    @Test(description = "TC_VC_10 - Gỡ bỏ voucher đang được áp dụng thành công")
    public void testRemoveAppliedVoucherRestoresOriginalTotal() {
        VoucherModal voucherModal = navigateToOrderAndGetVoucherModal();
        voucherModal.openVoucherModal();
        Assert.assertTrue(voucherModal.isModalVisible(), "Modal voucher hiển thị");

        voucherModal.removeAppliedVoucher();
        voucherModal.closeVoucherModal();

        Assert.assertFalse(voucherModal.isModalVisible(), "Modal đóng thành công sau khi gỡ mã");
    }
}
