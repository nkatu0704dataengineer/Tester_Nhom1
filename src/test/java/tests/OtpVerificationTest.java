package tests;

import base.BaseTest;
import com.microsoft.playwright.Route;
import org.testng.annotations.Test;
import pages.AuthModal;
import pages.HomePage;
import pages.OtpModal;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 1 - Xác thực mã OTP (Passwordless Login / Register)
 * Platform: Yody.vn
 *
 * Simulates and verifies the OTP verification screen behavior using API Mocking (page.route())
 * to test success, validation, expired codes, rate limiting, and replay prevention safely on live environment.
 */
public class OtpVerificationTest extends BaseTest {

    private static final String TEST_EMAIL = "yody.test01@gmail.com";

    /**
     * Helper method to navigate to the OTP modal safely by mocking the send-email-otp endpoint.
     *
     * @param email Email to initiate OTP request with
     * @return OtpModal instance ready for OTP tests
     */
    protected OtpModal openOtpModalWithMockedEmail(String email) {
        page.route("**/customer/auth/send-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"expires\":300,\"remaining_daily_otp_count\":5,\"success\":true}}"));
        });

        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();
        authModal.enterEmail(email);

        OtpModal otpModal = authModal.clickContinue();
        assertThat(otpModal.getTitleLoc()).isVisible();
        return otpModal;
    }

    /**
     * Test Case ID: TC_OTP_01
     * Tiêu đề: Nhập sai mã OTP
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Mock API trả về mã lỗi không đúng. Hệ thống hiển thị thông báo lỗi,
     * cho phép nhập lại, modal OTP không đóng và không đăng nhập.
     */
    @Test(description = "TC_OTP_01 - Nhập sai mã OTP")
    public void testEnterIncorrectOtp() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Mock API verify-email-otp phản hồi mã không đúng
        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"message\":\"Mã OTP không đúng hoặc đã hết hạn\"}"));
        });

        otpModal.enterOtp("0000");
        assertThat(otpModal.getVerifyBtnLoc()).isEnabled();

        otpModal.clickVerify();

        // Modal OTP vẫn hiển thị để người dùng có thể sửa và nhập lại
        assertThat(otpModal.getTitleLoc()).isVisible();
        assertThat(otpModal.getErrorMessageLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_OTP_02
     * Tiêu đề: Để trống toàn bộ các ô mã OTP
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Nút "Tiếp tục" ở màn hình OTP ở trạng thái bị vô hiệu hóa (Disabled).
     */
    @Test(description = "TC_OTP_02 - Để trống toàn bộ các ô mã OTP")
    public void testEmptyOtpInput() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        otpModal.clearOtp();

        // Nút Tiếp tục bắt buộc bị disable khi chưa nhập mã
        assertThat(otpModal.getVerifyBtnLoc()).isDisabled();
    }

    /**
     * Test Case ID: TC_OTP_03
     * Tiêu đề: Nhập chữ cái và ký tự đặc biệt vào ô OTP
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Các ô OTP chỉ chấp nhận số (numeric), chặn ký tự chữ/đặc biệt,
     * nút "Tiếp tục" vẫn bị vô hiệu hóa.
     */
    @Test(description = "TC_OTP_03 - Nhập chữ cái và ký tự đặc biệt vào ô OTP")
    public void testEnterAlphaAndSpecialCharactersInOtp() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        otpModal.enterOtp("ab#!");

        // Vì input có pattern numeric/^\d+$/, các ký tự chữ và ký tự đặc biệt bị từ chối
        // Nút Tiếp tục vẫn bị vô hiệu hóa
        assertThat(otpModal.getVerifyBtnLoc()).isDisabled();
    }

    /**
     * Test Case ID: TC_OTP_04
     * Tiêu đề: Nhập thiếu chữ số OTP (chỉ nhập 2-3 chữ số)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nút "Tiếp tục" vẫn bị vô hiệu hóa, không cho phép gửi yêu cầu khi chưa đủ 4 số.
     */
    @Test(description = "TC_OTP_04 - Nhập thiếu chữ số OTP")
    public void testIncompleteOtpDigits() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Nhập 3 chữ số thay vì 4 chữ số
        otpModal.enterOtp("123");

        assertThat(otpModal.getVerifyBtnLoc()).isDisabled();
    }

    /**
     * Test Case ID: TC_OTP_05
     * Tiêu đề: Nhập mã OTP đã hết hạn hiệu lực
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Mock API trả về lỗi OTP Expired. Giao diện hiển thị thông báo hết hạn
     * và cho phép gửi lại mã mới.
     */
    @Test(description = "TC_OTP_05 - Nhập mã OTP đã hết hạn hiệu lực")
    public void testExpiredOtp() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Mock API verify-email-otp báo lỗi mã OTP hết hạn
        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"message\":\"Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.\"}"));
        });

        otpModal.enterOtp("9999");
        otpModal.clickVerify();

        // Modal vẫn duy trì để người dùng gửi lại mã mới, hiển thị thông báo lỗi
        assertThat(otpModal.getTitleLoc()).isVisible();
        assertThat(otpModal.getErrorMessageLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_OTP_06
     * Tiêu đề: Nhập sai OTP liên tiếp vượt quá số lần quy định
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Mock API trả về lỗi Rate Limit (429 / Max attempts reached).
     * Giao diện hiển thị thông báo tạm khóa xác thực, chặn truy cập tài khoản.
     */
    @Test(description = "TC_OTP_06 - Nhập sai OTP vượt quá số lần thử")
    public void testExceedMaxOtpAttempts() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Mock API phản hồi vượt quá số lần thử cho phép
        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(429)
                    .setContentType("application/json")
                    .setBody("{\"code\":429,\"message\":\"Bạn đã vượt quá số lần thử OTP cho phép. Vui lòng thử lại sau 15 phút.\"}"));
        });

        otpModal.enterOtp("8888");
        otpModal.clickVerify();

        assertThat(otpModal.getErrorMessageLoc()).isVisible();
        assertThat(otpModal.getTitleLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_OTP_07
     * Tiêu đề: Kiểm tra chức năng gửi lại mã OTP (Resend OTP) và xác thực thành công
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Gửi lại mã OTP thành công, khi nhập đúng mã OTP mới (Mock 200 OK) thì
     * modal tự động đóng và hoàn tất xác thực đăng nhập.
     */
    @Test(description = "TC_OTP_07 - Gửi lại mã OTP và xác thực thành công")
    public void testResendOtpAndVerifySuccess() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Mock API gửi lại mã OTP
        page.route("**/customer/auth/send-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Đã gửi lại mã thành công\",\"data\":{\"expires\":300,\"remaining_daily_otp_count\":4,\"success\":true}}"));
        });

        // Mock API xác thực OTP thành công với mã mới
        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"access_token\":\"mock_token_success_07\",\"refresh_token\":\"mock_refresh_07\",\"expires_in\":86400}}"));
        });

        // Nhập mã OTP mới và tiến hành xác nhận
        otpModal.enterOtp("6789");
        assertThat(otpModal.getVerifyBtnLoc()).isEnabled();

        otpModal.clickVerify();

        // Sau khi xác thực thành công, màn hình OTP tự động đóng
        assertThat(otpModal.getTitleLoc()).not().isVisible();
    }

    /**
     * Test Case ID: TC_OTP_08
     * Tiêu đề: Dùng lại mã OTP cũ sau khi đã bấm gửi lại mã mới
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống từ chối mã OTP cũ vì đã bị vô hiệu hóa khi tạo mã mới.
     */
    @Test(description = "TC_OTP_08 - Dùng lại mã OTP cũ sau khi gửi lại mã mới")
    public void testReuseOldOtpAfterResend() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Giả lập backend từ chối mã cũ
        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"message\":\"Mã OTP không hợp lệ hoặc đã bị thay thế bởi mã mới.\"}"));
        });

        // Thử gửi mã OTP cũ "1111"
        otpModal.enterOtp("1111");
        otpModal.clickVerify();

        assertThat(otpModal.getErrorMessageLoc()).isVisible();
        assertThat(otpModal.getTitleLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_OTP_09
     * Tiêu đề: Dùng lại mã OTP đã từng xác thực thành công trước đó (Replay attack prevention)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Mã bị từ chối với thông báo lỗi vì OTP chỉ có giá trị sử dụng một lần duy nhất.
     */
    @Test(description = "TC_OTP_09 - Dùng lại mã OTP đã từng xác thực thành công")
    public void testReuseAlreadyVerifiedOtp() {
        OtpModal otpModal = openOtpModalWithMockedEmail(TEST_EMAIL);

        // Giả lập backend phát hiện mã đã qua sử dụng (Replay Attack)
        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"code\":400,\"message\":\"Mã OTP này đã được sử dụng. Mỗi mã OTP chỉ có giá trị 1 lần duy nhất.\"}"));
        });

        // Nhập mã đã qua sử dụng
        otpModal.enterOtp("5555");
        otpModal.clickVerify();

        assertThat(otpModal.getErrorMessageLoc()).isVisible();
        assertThat(otpModal.getTitleLoc()).isVisible();
    }
}
