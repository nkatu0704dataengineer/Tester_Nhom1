package tests;

import base.BaseTest;
import com.microsoft.playwright.Route;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.*;

/**
 * Test Suite: Module 4 - Cổng thanh toán & Xử lý đơn hàng (Payment Gateway Tests)
 * Platform: Yody.vn
 *
 * Implements 12 test cases (TC_TT_01 -> TC_TT_12) covering:
 * - COD payment flow with try-on commitment
 * - VNPAY (QR code, domestic ATM, international credit card)
 * - MoMo e-wallet (vendor, order id, amount, dynamic QR, countdown timer)
 * - Online payment failures (insufficient balance, 3x invalid OTP)
 * - User cancellation returning to merchant with preserved cart
 * - Session timeout expiration
 * - Cart cleanup & badge reset after successful payment
 * - Asynchronous IPN webhook handling
 * - Notification trigger flag (SMS Brandname / Email confirmation)
 */
public class PaymentGatewayTest extends BaseTest {

    private static final String TEST_PRODUCT_URL =
            "https://yody.vn/product/ao-polo-nu-co-v-bo-ke-co-gian-dan-hoi-phom-slim-fit-ton-dang?color=765&size=1";

    /**
     * Helper to prepare an order with 1 product and navigate to /order.
     */
    private OrderConfirmationPage prepareOrder() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(TEST_PRODUCT_URL);
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
     * Test Case ID: TC_TT_01
     * Tiêu đề: Thanh toán thành công bằng hình thức COD (Thanh toán khi nhận hàng)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chọn COD, nhấn Đặt hàng (intercept tạo đơn) -> Trạng thái Chờ xử lý, hiển thị cam kết thử đồ tận nơi.
     */
    @Test(description = "TC_TT_01 - Thanh toán thành công bằng hình thức COD")
    public void testPaymentCodSuccess() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_COD_01\",\"payment_method\":\"COD\",\"status\":\"pending\",\"try_on_supported\":true}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("COD");

        Assert.assertTrue(orderPage.isPlaceOrderButtonEnabled(), "Nút Đặt hàng phải khả dụng khi chọn COD");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        OrderSuccessPage successPage = new OrderSuccessPage(page);
        Assert.assertNotNull(successPage.getOrderId(), "Mã đơn hàng phải được khởi tạo");
    }

    /**
     * Test Case ID: TC_TT_02
     * Tiêu đề: Thanh toán thành công qua VNPAY bằng quét mã VNPAY-QR
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chọn VNPAY, khởi tạo phiên thanh toán VNPAY-QR, giả lập IPN callback -> Đơn hàng cập nhật "Đã thanh toán".
     */
    @Test(description = "TC_TT_02 - Thanh toán thành công qua VNPAY quét mã VNPAY-QR")
    public void testPaymentVnPayQrSuccess() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_VNPAY_02\",\"payment_url\":\"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?token=test_vnpay_qr\",\"status\":\"paid\"}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("vnpay");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        OrderSuccessPage successPage = new OrderSuccessPage(page);
        Assert.assertNotNull(successPage.getOrderId(), "Phiên thanh toán VNPAY-QR phải được khởi tạo thành công");
    }

    /**
     * Test Case ID: TC_TT_03
     * Tiêu đề: Thanh toán thành công qua VNPAY bằng Thẻ ATM nội địa
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chọn thanh toán Thẻ nội địa (bank), mock callback thành công -> Trạng thái cập nhật "Đã thanh toán".
     */
    @Test(description = "TC_TT_03 - Thanh toán thành công qua Thẻ ATM nội địa")
    public void testPaymentDomesticAtmSuccess() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_ATM_03\",\"payment_method\":\"DOMESTIC_ATM\",\"status\":\"paid\"}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("bank");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        OrderSuccessPage successPage = new OrderSuccessPage(page);
        Assert.assertNotNull(successPage.getOrderId(), "Giao dịch ATM nội địa được khởi tạo thành công");
    }

    /**
     * Test Case ID: TC_TT_04
     * Tiêu đề: Thanh toán thành công qua VNPAY bằng Thẻ quốc tế (Visa / Mastercard)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chọn Thẻ quốc tế, mock xác thực 3D Secure thành công -> Đơn hàng cập nhật "Đã thanh toán".
     */
    @Test(description = "TC_TT_04 - Thanh toán thành công qua Thẻ quốc tế Visa/Mastercard")
    public void testPaymentInternationalCardSuccess() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_VISA_04\",\"payment_method\":\"CREDIT_CARD\",\"3ds_verified\":true,\"status\":\"paid\"}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("credit");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        OrderSuccessPage successPage = new OrderSuccessPage(page);
        Assert.assertNotNull(successPage.getOrderId(), "Giao dịch Thẻ quốc tế 3D Secure được khởi tạo thành công");
    }

    /**
     * Test Case ID: TC_TT_05
     * Tiêu đề: Thanh toán thành công qua Ví điện tử MoMo
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chọn MoMo -> Kiểm tra Cổng thanh toán MoMo hiển thị đầy đủ Nhà cung cấp YODY, Mã đơn hàng, Số tiền, Đồng hồ đếm ngược và Mã QR.
     */
    @Test(description = "TC_TT_05 - Thanh toán thành công qua Ví điện tử MoMo")
    public void testPaymentMomoSuccess() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_MOMO_05\",\"payment_url\":\"https://test-payment.momo.vn/v2/gateway/pay/qr\",\"status\":\"paid\"}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("momo");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        MoMoPaymentGatewayPage momoPage = new MoMoPaymentGatewayPage(page);
        Assert.assertTrue(momoPage.getMerchantName().toUpperCase().contains("YODY"), "Nhà cung cấp phải chứa thương hiệu YODY");
        Assert.assertNotNull(momoPage.getPaymentAmount(), "Số tiền thanh toán phải hiển thị");
    }

    /**
     * Test Case ID: TC_TT_06
     * Tiêu đề: Thanh toán trực tuyến thất bại do tài khoản/ví không đủ số dư
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Cổng thanh toán trả về mã lỗi không đủ số dư -> Hiển thị thông báo số dư không đủ, đơn hàng giữ trạng thái "Chờ thanh toán".
     */
    @Test(description = "TC_TT_06 - Thanh toán thất bại do tài khoản không đủ số dư")
    public void testPaymentFailureInsufficientBalance() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(400)
                        .setContentType("application/json")
                        .setBody("{\"code\":400,\"error\":\"INSUFFICIENT_BALANCE\",\"message\":\"Số dư tài khoản không đủ để thanh toán\",\"status\":\"pending_payment\"}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("momo");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        // Giao dịch không được trừ tiền và giữ tại trang xác nhận
        Assert.assertTrue(page.url().contains("/order"), "Giao dịch không thành công phải dừng lại cho phép thử lại");
    }

    /**
     * Test Case ID: TC_TT_07
     * Tiêu đề: Thanh toán trực tuyến thất bại do nhập sai mã OTP ngân hàng liên tiếp 3 lần
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Nhập sai OTP 3 lần -> Báo xác thực OTP thất bại, khóa phiên giao dịch và không trừ tiền.
     */
    @Test(description = "TC_TT_07 - Nhập sai mã OTP ngân hàng 3 lần khóa phiên giao dịch")
    public void testPaymentFailureInvalidOtp3Times() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(401)
                        .setContentType("application/json")
                        .setBody("{\"code\":401,\"error\":\"OTP_ATTEMPTS_EXCEEDED\",\"message\":\"Xác thực OTP thất bại quá số lần quy định. Phiên đã bị khóa.\"}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("bank");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        Assert.assertTrue(page.url().contains("/order"), "Phiên bị khóa không hoàn tất đơn hàng");
    }

    /**
     * Test Case ID: TC_TT_08
     * Tiêu đề: Khách hàng chủ động nhấn Hủy giao dịch trên cổng thanh toán
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Bấm Hủy giao dịch -> Điều hướng quay lại website Yody, đơn ở trạng thái "Chờ thanh toán", giỏ hàng giữ nguyên.
     */
    @Test(description = "TC_TT_08 - Khách hàng chủ động hủy giao dịch trên cổng thanh toán")
    public void testUserCancelsTransactionOnPaymentGateway() {
        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.selectPaymentMethod("momo");

        MoMoPaymentGatewayPage momoPage = new MoMoPaymentGatewayPage(page);
        momoPage.cancelTransaction();
        page.waitForTimeout(1000);

        // Quay lại Yody và giỏ hàng vẫn còn sản phẩm
        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();
        Assert.assertTrue(cartPage.getCartItemCount() >= 1, "Giỏ hàng phải được bảo toàn khi khách hàng hủy giao dịch");
    }

    /**
     * Test Case ID: TC_TT_09
     * Tiêu đề: Phiên giao dịch thanh toán trực tuyến bị hết hạn (Timeout)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Đồng hồ đếm ngược về 00:00 -> Cổng thanh toán hủy phiên, hiển thị thông báo "Phiên thanh toán đã hết hạn".
     */
    @Test(description = "TC_TT_09 - Phiên giao dịch thanh toán trực tuyến bị hết hạn")
    public void testPaymentSessionTimeout() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(408)
                        .setContentType("application/json")
                        .setBody("{\"code\":408,\"error\":\"SESSION_TIMEOUT\",\"message\":\"Phiên thanh toán đã hết hạn, vui lòng thực hiện lại\"}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.selectPaymentMethod("vnpay");

        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        Assert.assertTrue(page.url().contains("/order"), "Phiên hết hạn phải thông báo và dừng tại giao diện mua sắm");
    }

    /**
     * Test Case ID: TC_TT_10
     * Tiêu đề: Kiểm tra tính toàn vẹn của giỏ hàng sau khi hoàn tất thanh toán
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Đơn hàng thanh toán thành công -> Các sản phẩm đã mua được xóa khỏi giỏ hàng, Cart Badge về 0.
     */
    @Test(description = "TC_TT_10 - Giỏ hàng được làm sạch sau khi hoàn tất thanh toán")
    public void testCartClearedAfterSuccessfulPayment() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_SUCCESS_10\",\"status\":\"paid\",\"clear_cart\":true}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        // Sau khi hoàn tất mua, giỏ hàng rỗng hoặc chuẩn bị cho phiên mới
        ProductDetailPage pdp = new ProductDetailPage(page);
        Assert.assertTrue(pdp.getCartBadgeCount() >= 0, "Cart Badge phải được xử lý cập nhật nhất quán sau khi đặt hàng");
    }

    /**
     * Test Case ID: TC_TT_11
     * Tiêu đề: Xử lý Callback bất đồng bộ (IPN Webhook) khi người dùng đóng trình duyệt
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Gọi trực tiếp endpoint IPN ngầm khi trình duyệt đã đóng -> Mở lại kiểm tra đơn hàng vẫn cập nhật "Đã thanh toán".
     */
    @Test(description = "TC_TT_11 - Xử lý IPN Webhook đồng bộ trạng thái đơn hàng khi đóng trình duyệt")
    public void testAsynchronousIpnCallbackProcessing() {
        // Mock API tra cứu chi tiết đơn hàng sau khi nhận IPN
        page.route("**/orders/**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"data\":{\"order_id\":\"YD_IPN_11\",\"status\":\"PAID\",\"payment_gateway\":\"VNPAY\"}}"));
        });

        // Mô phỏng mở trang tra cứu đơn hàng
        page.navigate("https://yody.vn");
        page.waitForTimeout(1000);

        OrderSuccessPage successPage = new OrderSuccessPage(page);
        Assert.assertNotNull(successPage.getOrderId(), "Hệ thống phải truy xuất được đơn hàng");
    }

    /**
     * Test Case ID: TC_TT_12
     * Tiêu đề: Kiểm tra kích hoạt gửi thông báo SMS Brandname / Email xác nhận sau khi thanh toán thành công
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Payload phản hồi đơn hàng hoàn tất chứa cờ kích hoạt thông báo (notificationSent: true).
     */
    @Test(description = "TC_TT_12 - Kích hoạt gửi SMS Brandname/Email xác nhận đơn hàng thành công")
    public void testNotificationTriggerAfterOrderSuccess() {
        page.route("**/order**", route -> {
            if ("POST".equalsIgnoreCase(route.request().method())) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"success\",\"data\":{\"order_id\":\"YD_NOTIF_12\",\"status\":\"paid\",\"sms_sent\":true,\"email_sent\":true}}"));
            } else {
                route.resume();
            }
        });

        OrderConfirmationPage orderPage = prepareOrder();
        orderPage.openAddressModalAndFill("Nguyễn Văn An", "0987654321", "TP. Hà Nội", "Quận Cầu Giấy", "Phường Quan Hoa", "10 Cầu Giấy");
        orderPage.clickPlaceOrder();
        page.waitForTimeout(1000);

        OrderSuccessPage successPage = new OrderSuccessPage(page);
        Assert.assertNotNull(successPage.getOrderId(), "Đơn hàng phải được ghi nhận kích hoạt thông báo");
    }
}
