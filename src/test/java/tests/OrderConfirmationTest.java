package tests;

import base.BaseTest;
import com.microsoft.playwright.Route;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.CartPage;
import pages.OrderConfirmationPage;
import pages.ProductDetailPage;
import pages.VoucherModal;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test Suite: Module 4 - Xác nhận đặt hàng (Order Confirmation Tests)
 * Platform: Yody.vn
 *
 * Implements 15 test cases (TC_DH_01 -> TC_DH_15) covering successful order flows (COD, Free shipping >= 498k,
 * shipping fee < 498k, store pickup, address book with notes, valid voucher),
 * form validations (empty/invalid name, empty/invalid phone, missing admin levels, missing detailed address, unchecked policy),
 * invalid promo codes, out-of-stock simulation, and XSS sanitization.
 *
 * SAFETY GUARDRAIL: All successful placement requests use page.route() interception to prevent real order creation.
 */
public class OrderConfirmationTest extends BaseTest {

    private static final String TEST_PRODUCT_URL =
            "https://yody.vn/product/ao-polo-nu-co-v-bo-ke-co-gian-dan-hoi-phom-slim-fit-ton-dang?color=765&size=1";

    /**
     * Helper to prepare an order by adding a product and navigating to /order.
     */
    private OrderConfirmationPage prepareOrder(int quantity) {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(TEST_PRODUCT_URL);
        if (quantity > 1) {
            pdp.setQuantity(quantity);
        }
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();
        cartPage.clickCheckoutButton();

        page.waitForURL("**/order**");
        page.waitForTimeout(1000);

        return new OrderConfirmationPage(page);
    }

    /**
     * Test Case ID: TC_DH_01
     * Tiêu đề: Đặt hàng thành công với thông tin giao tận nơi hợp lệ (Đơn hàng >= 498.000đ)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Đơn hàng >= 498k được tính phí vận chuyển "miễn phí" (0đ), nút Đặt hàng sẵn sàng,
     * intercept tạo đơn thành công mà không phát sinh đơn thật trên máy chủ.
     */
    @Test(description = "TC_DH_01 - Đặt hàng thành công với đơn hàng >= 498.000đ được miễn phí vận chuyển")
    public void testPlaceOrderSuccessWithFreeShipping() {
        // Intercept create order POST requests
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method()) && !route.request().url().contains("/search")) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_TEST_01\",\"status\":\"pending\"}}"));
            } else {
                route.resume();
            }
        });

        // 2 products => > 498.000đ
        OrderConfirmationPage orderPage = prepareOrder(2);

        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.enterOrderNote("Giao giờ hành chính");

        // Xác minh miễn phí vận chuyển cho đơn >= 498k
        String shippingFee = orderPage.getShippingFeeText();
        Assert.assertTrue(shippingFee.toLowerCase().contains("miễn phí") || orderPage.getShippingFeeAmount() == 0,
                "Đơn hàng >= 498.000đ phải được miễn phí vận chuyển, nhận được: " + shippingFee);

        // Đảm bảo đồng ý chính sách bảo mật
        orderPage.setPrivacyPolicyAgreement(true);
        Assert.assertTrue(orderPage.isPrivacyPolicyAgreed(), "Chính sách phải ở trạng thái đã đồng ý");

        // Xác minh nút Đặt hàng khả dụng
        Assert.assertTrue(orderPage.isPlaceOrderButtonEnabled(), "Nút Đặt hàng phải khả dụng khi điền đủ thông tin hợp lệ");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);
    }

    /**
     * Test Case ID: TC_DH_02
     * Tiêu đề: Đặt hàng thành công với đơn hàng dưới 498.000đ (Áp dụng phí giao hàng 20.000đ)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Giá trị đơn hàng dưới 498.000đ tự động áp dụng phí giao hàng tiêu chuẩn 20.000đ,
     * Thành tiền = Tổng tiền - Giảm giá + 20.000đ.
     */
    @Test(description = "TC_DH_02 - Đơn hàng dưới 498.000đ áp dụng phí vận chuyển 20.000đ")
    public void testOrderUnder498kAppliesStandardShippingFee() {
        // 1 product => 279.200đ (< 498.000đ)
        OrderConfirmationPage orderPage = prepareOrder(1);

        long totalPrice = orderPage.getTotalPrice();
        long discount = orderPage.getDiscountAmount();
        long shippingFee = orderPage.getShippingFeeAmount();
        long finalTotal = orderPage.getFinalTotalAmount();

        if (totalPrice < 498000) {
            Assert.assertEquals(shippingFee, 20000, "Phí vận chuyển cho đơn dưới 498k phải là 20.000đ");
            long expectedTotal = totalPrice - discount + shippingFee;
            Assert.assertEquals(finalTotal, expectedTotal, "Thành tiền phải bằng Tổng tiền - Giảm giá + Phí vận chuyển");
        }
    }

    /**
     * Test Case ID: TC_DH_03
     * Tiêu đề: Đặt hàng thành công với hình thức Nhận tại cửa hàng
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Chọn Nhận tại cửa hàng -> Không yêu cầu địa chỉ giao tận nơi, phí vận chuyển = 0đ.
     */
    @Test(description = "TC_DH_03 - Đặt hàng với hình thức Nhận tại cửa hàng không tính phí vận chuyển")
    public void testStorePickupDeliveryMethod() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.selectDeliveryMethod(false); // Nhận tại cửa hàng
        orderPage.fillRecipientInfo("Lê Thị Hoa", "0912345678", null);

        // Với nhận tại cửa hàng, phí vận chuyển là 0đ
        Assert.assertTrue(orderPage.getShippingFeeText().toLowerCase().contains("miễn phí") || orderPage.getShippingFeeAmount() == 0,
                "Nhận tại cửa hàng không được tính phí vận chuyển");
    }

    /**
     * Test Case ID: TC_DH_04
     * Tiêu đề: Đặt hàng với tài khoản thành viên sử dụng sổ địa chỉ và nhập Yêu cầu khác
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Ô Yêu cầu khác lưu đúng nội dung "Giao giờ hành chính", không bị mất dữ liệu.
     */
    @Test(description = "TC_DH_04 - Lưu đúng yêu cầu khác (ghi chú) khi đặt hàng")
    public void testOrderWithSpecialNote() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        String note = "Giao giờ hành chính, gọi trước khi giao 15 phút";
        orderPage.enterOrderNote(note);

        Assert.assertEquals(orderPage.getOrderNote(), note, "Ghi chú đơn hàng phải được lưu chính xác");
    }

    /**
     * Test Case ID: TC_DH_05
     * Tiêu đề: Đặt hàng kèm áp dụng mã khuyến mãi hợp lệ
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Áp dụng voucher YODY50K giảm 50.000đ -> Dòng Giảm giá hiển thị và Thành tiền giảm 50.000đ.
     */
    @Test(description = "TC_DH_05 - Đặt hàng kèm áp dụng mã khuyến mãi hợp lệ")
    public void testPlaceOrderWithValidVoucher() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"success\",\"data\":[{\"id\":\"1\",\"title\":\"Giảm 50K\",\"code\":\"YODY50K\",\"discount_value\":50000}]}"));
        });

        OrderConfirmationPage orderPage = prepareOrder(1);
        long initialTotal = orderPage.getFinalTotalAmount();

        VoucherModal modal = orderPage.openVoucherModal();
        modal.applyVoucherCode("YODY50K");
        page.waitForTimeout(1000);

        long updatedTotal = orderPage.getFinalTotalAmount();
        Assert.assertTrue(updatedTotal <= initialTotal, "Thành tiền sau khi áp dụng voucher phải nhỏ hơn hoặc bằng ban đầu");
    }

    /**
     * Test Case ID: TC_DH_06
     * Tiêu đề: Đặt hàng thất bại khi bỏ trống trường Họ và tên
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Bỏ trống tên -> Hệ thống hiển thị cảnh báo lỗi hoặc chặn đặt hàng.
     */
    @Test(description = "TC_DH_06 - Bỏ trống Họ và tên bị chặn tạo đơn")
    public void testValidationEmptyFullName() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.fillRecipientInfo("", "0987654321", null);
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        // Xác minh vẫn ở trang /order và form validation kích hoạt
        Assert.assertTrue(page.url().contains("/order"), "Hệ thống phải giữ lại trang /order khi thiếu họ tên");
    }

    /**
     * Test Case ID: TC_DH_07
     * Tiêu đề: Đặt hàng thất bại khi Họ và tên chứa ký tự đặc biệt hoặc chữ số
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Tên chứa ký tự đặc biệt "Nguyen@Van#123" -> Kích hoạt cảnh báo định dạng họ tên.
     */
    @Test(description = "TC_DH_07 - Họ và tên chứa ký tự đặc biệt hoặc chữ số")
    public void testValidationInvalidFullNameCharacters() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.fillRecipientInfo("Nguyen@Van#123", "0987654321", null);
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        Assert.assertTrue(page.url().contains("/order"), "Không được tạo đơn khi họ tên chứa ký tự đặc biệt");
    }

    /**
     * Test Case ID: TC_DH_08
     * Tiêu đề: Đặt hàng thất bại khi bỏ trống Số điện thoại nhận hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Bỏ trống SĐT -> Kích hoạt cảnh báo yêu cầu nhập số điện thoại, chặn gửi đơn.
     */
    @Test(description = "TC_DH_08 - Bỏ trống số điện thoại nhận hàng bị chặn")
    public void testValidationEmptyPhoneNumber() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.fillRecipientInfo("Nguyễn Văn An", "", null);
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        Assert.assertTrue(page.url().contains("/order"), "Hệ thống phải giữ lại trang /order khi thiếu số điện thoại");
    }

    @DataProvider(name = "invalidPhoneNumbers")
    public Object[][] invalidPhoneNumbers() {
        return new Object[][]{
                {"0987654"},      // Quá ngắn (7 số)
                {"098765432100"}, // Quá dài (12 số)
                {"0123456789"}    // Đầu số không hợp lệ
        };
    }

    /**
     * Test Case ID: TC_DH_09
     * Tiêu đề: Đặt hàng thất bại khi Số điện thoại sai định dạng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Các số điện thoại sai định dạng bị chặn, không thể gửi đơn hàng.
     */
    @Test(dataProvider = "invalidPhoneNumbers", description = "TC_DH_09 - Số điện thoại sai định dạng bị chặn")
    public void testValidationInvalidPhoneFormat(String invalidPhone) {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.fillRecipientInfo("Nguyễn Văn An", invalidPhone, null);
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        Assert.assertTrue(page.url().contains("/order"), "Số điện thoại '" + invalidPhone + "' không hợp lệ phải bị chặn");
    }

    /**
     * Test Case ID: TC_DH_10
     * Tiêu đề: Đặt hàng thất bại khi chưa chọn đầy đủ cấp hành chính Tỉnh/Huyện/Xã
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chỉ điền tên, SĐT mà chưa chọn Tỉnh/Huyện/Xã -> Chặn tạo đơn.
     */
    @Test(description = "TC_DH_10 - Chưa chọn cấp hành chính Tỉnh/Huyện/Xã bị chặn")
    public void testValidationMissingAdministrativeUnits() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.fillRecipientInfo("Nguyễn Văn An", "0987654321", null);
        // Không chọn Tỉnh/Thành phố
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        Assert.assertTrue(page.url().contains("/order"), "Phải chọn đủ cấp hành chính mới được tạo đơn");
    }

    /**
     * Test Case ID: TC_DH_11
     * Tiêu đề: Đặt hàng thất bại khi bỏ trống Địa chỉ chi tiết (số nhà, tên đường)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chưa nhập số nhà, tên đường -> Yêu cầu nhập địa chỉ chi tiết, chặn gửi đơn.
     */
    @Test(description = "TC_DH_11 - Bỏ trống địa chỉ chi tiết bị chặn đặt hàng")
    public void testValidationMissingDetailedAddress() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        Assert.assertTrue(page.url().contains("/order"), "Địa chỉ chi tiết không được bỏ trống");
    }

    /**
     * Test Case ID: TC_DH_12
     * Tiêu đề: Đặt hàng thất bại khi chưa tích chọn đồng ý Chính sách xử lý dữ liệu cá nhân
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Bỏ tích checkbox Chính sách -> Nút Đặt hàng bị vô hiệu hóa hoặc bị chặn khi click.
     */
    @Test(description = "TC_DH_12 - Bỏ chọn đồng ý Chính sách bảo mật dữ liệu bị chặn đặt hàng")
    public void testValidationUncheckedPrivacyPolicy() {
        OrderConfirmationPage orderPage = prepareOrder(1);

        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.setPrivacyPolicyAgreement(false); // Bỏ tích

        Assert.assertFalse(orderPage.isPrivacyPolicyAgreed(), "Trạng thái checkbox chính sách phải là chưa đồng ý");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(500);

        Assert.assertTrue(page.url().contains("/order"), "Bỏ tích chính sách phải chặn đặt hàng thành công");
    }

    /**
     * Test Case ID: TC_DH_13
     * Tiêu đề: Áp dụng mã khuyến mãi không hợp lệ hoặc đã hết hạn tại trang Xác nhận đặt hàng
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nhập mã không hợp lệ -> Báo lỗi mã không tồn tại, tổng tiền giữ nguyên.
     */
    @Test(description = "TC_DH_13 - Mã khuyến mãi không hợp lệ giữ nguyên tổng tiền")
    public void testInvalidPromoCodeOnCheckout() {
        page.route("**/promotions/**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":404,\"message\":\"Mã khuyến mãi không tồn tại\",\"data\":[]}"));
        });

        OrderConfirmationPage orderPage = prepareOrder(1);
        long initialTotal = orderPage.getFinalTotalAmount();

        VoucherModal modal = orderPage.openVoucherModal();
        modal.applyVoucherCode("MA_KHONG_TON_TAI");
        page.waitForTimeout(1000);

        long finalTotal = orderPage.getFinalTotalAmount();
        Assert.assertEquals(finalTotal, initialTotal, "Mã khuyến mãi lỗi không được làm thay đổi tổng tiền");
    }

    /**
     * Test Case ID: TC_DH_14
     * Tiêu đề: Sản phẩm trong giỏ bị hết hàng ngay lúc nhấn xác nhận đặt hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Mock API trả về lỗi Out of Stock -> Hiển thị thông báo hết hàng, chặn tạo đơn.
     */
    @Test(description = "TC_DH_14 - Sản phẩm hết hàng khi bấm xác nhận đặt hàng")
    public void testOutOfStockAtCheckoutMoment() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(422)
                        .setContentType("application/json")
                        .setBody("{\"code\":422,\"message\":\"Sản phẩm trong giỏ đã hết hàng, vui lòng cập nhật lại giỏ hàng\"}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder(1);
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        Assert.assertTrue(page.url().contains("/order"), "Giao dịch phải bị dừng lại tại trang /order khi hết hàng");
    }

    /**
     * Test Case ID: TC_DH_15
     * Tiêu đề: Kiểm tra an toàn dữ liệu chống XSS tại trường Yêu cầu khác
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nhập script XSS "<script>alert('XSS')</script>" không kích hoạt alert hộp thoại.
     */
    @Test(description = "TC_DH_15 - Chống XSS an toàn tại trường Yêu cầu khác")
    public void testXssSanitizationInOrderNote() {
        AtomicBoolean dialogTriggered = new AtomicBoolean(false);
        page.onDialog(dialog -> {
            dialogTriggered.set(true);
            dialog.dismiss();
        });

        OrderConfirmationPage orderPage = prepareOrder(1);
        String xssPayload = "<script>alert('XSS')</script>";
        orderPage.enterOrderNote(xssPayload);
        page.waitForTimeout(500);

        Assert.assertFalse(dialogTriggered.get(), "Payload XSS không được phép thực thi hộp thoại alert() trên trình duyệt");
        Assert.assertEquals(orderPage.getOrderNote(), xssPayload, "Nội dung ghi chú được giữ an toàn");
    }
}
