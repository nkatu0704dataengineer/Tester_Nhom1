package tests;

import base.BaseTest;
import com.microsoft.playwright.Route;
import org.testng.annotations.Test;
import pages.AuthModal;
import pages.HomePage;
import pages.OtpModal;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 1 - Đăng ký, Đăng nhập & Xác thực Email
 * Platform: Yody.vn
 *
 * Verifies the email input form validation and transition logic to the OTP verification screen.
 */
public class AuthEmailTest extends BaseTest {

    /**
     * Test Case ID: TC_DN_01
     * Tiêu đề: Đăng nhập thành công bằng email hợp lệ
     * Mức độ ưu tiên: Cao (P1)
     *
     * Mô tả: Nhập email hợp lệ chuẩn. Sử dụng page.route() để mock API send-email-otp trả về 200 OK
     * nhằm đảm bảo an toàn môi trường live, chống spam và xác minh chuyển sang màn hình OTP ổn định.
     */
    @Test(description = "TC_DN_01 - Đăng nhập thành công bằng email hợp lệ")
    public void testLoginWithValidEmail() {
        // Mock send-email-otp response with expected JSON schema
        page.route("**/customer/auth/send-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"expires\":300,\"remaining_daily_otp_count\":5,\"success\":true}}"));
        });

        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        assertThat(authModal.getTitleLoc()).isVisible();

        authModal.enterEmail("yody.test01@gmail.com");
        assertThat(authModal.getContinueBtnLoc()).isEnabled();

        OtpModal otpModal = authModal.clickContinue();

        // Xác minh màn hình "Nhập mã OTP" hiển thị thành công
        assertThat(otpModal.getTitleLoc()).isVisible();
        assertThat(otpModal.getEmailNotificationLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_DN_02
     * Tiêu đề: Để trống ô Email
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Nút "Tiếp tục" ở trạng thái bị vô hiệu hóa (Disabled).
     */
    @Test(description = "TC_DN_02 - Để trống ô Email")
    public void testEmptyEmail() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("");

        // Nút Tiếp tục bắt buộc phải bị vô hiệu hóa
        assertThat(authModal.getContinueBtnLoc()).isDisabled();
    }

    /**
     * Test Case ID: TC_DN_03
     * Tiêu đề: Nhập email thiếu ký tự '@'
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nút "Tiếp tục" bị vô hiệu hóa hoặc hệ thống báo lỗi định dạng email,
     * không cho phép chuyển sang màn OTP.
     */
    @Test(description = "TC_DN_03 - Nhập email thiếu ký tự @")
    public void testEmailMissingAtSymbol() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("yody.test01gmail.com");

        if (authModal.isContinueButtonEnabled()) {
            OtpModal otpModal = authModal.clickContinue();
            // Kỳ vọng: Không cho phép chuyển sang màn OTP
            assertThat(otpModal.getTitleLoc()).not().isVisible();
            assertThat(authModal.getTitleLoc()).isVisible();
        } else {
            assertThat(authModal.getContinueBtnLoc()).isDisabled();
        }
    }

    /**
     * Test Case ID: TC_DN_04
     * Tiêu đề: Nhập email thiếu tên miền
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nút "Tiếp tục" bị vô hiệu hóa hoặc báo lỗi định dạng email.
     */
    @Test(description = "TC_DN_04 - Nhập email thiếu tên miền")
    public void testEmailMissingDomain() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("yody.test01@");

        if (authModal.isContinueButtonEnabled()) {
            OtpModal otpModal = authModal.clickContinue();
            // Kỳ vọng: Không cho phép chuyển sang màn OTP
            assertThat(otpModal.getTitleLoc()).not().isVisible();
            assertThat(authModal.getTitleLoc()).isVisible();
        } else {
            assertThat(authModal.getContinueBtnLoc()).isDisabled();
        }
    }

    /**
     * Test Case ID: TC_DN_05
     * Tiêu đề: Nhập email chứa khoảng trắng giữa chừng và ký tự không hợp lệ
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nút "Tiếp tục" bị vô hiệu hóa hoặc báo lỗi định dạng email không hợp lệ.
     */
    @Test(description = "TC_DN_05 - Nhập email chứa khoảng trắng và ký tự không hợp lệ")
    public void testEmailWithSpacesAndInvalidChars() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("yody test@@gmail.com");

        if (authModal.isContinueButtonEnabled()) {
            OtpModal otpModal = authModal.clickContinue();
            // Kỳ vọng: Báo lỗi/chặn không chuyển sang màn OTP
            assertThat(otpModal.getTitleLoc()).not().isVisible();
            assertThat(authModal.getTitleLoc()).isVisible();
        } else {
            assertThat(authModal.getContinueBtnLoc()).isDisabled();
        }
    }

    /**
     * Test Case ID: TC_DN_06
     * Tiêu đề: Nhập email có khoảng trắng ở đầu và cuối chuỗi
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống tự động loại bỏ khoảng trắng (trim) và cho phép bấm nút "Tiếp tục".
     */
    @Test(description = "TC_DN_06 - Nhập email có khoảng trắng ở đầu và cuối chuỗi")
    public void testEmailWithLeadingAndTrailingSpaces() {
        page.route("**/customer/auth/send-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"expires\":300,\"remaining_daily_otp_count\":5,\"success\":true}}"));
        });

        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("  yody.test01@gmail.com  ");

        // Khi gõ email hợp lệ (kể cả có khoảng trắng biên), hệ thống xử lý trim và bật nút Tiếp tục
        assertThat(authModal.getContinueBtnLoc()).isEnabled();

        OtpModal otpModal = authModal.clickContinue();
        assertThat(otpModal.getTitleLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_DN_07
     * Tiêu đề: Nhập email lẫn lộn chữ hoa và chữ thường
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống chấp nhận định dạng hợp lệ (Case-insensitive) và nút "Tiếp tục" ở trạng thái Enabled.
     */
    @Test(description = "TC_DN_07 - Nhập email lẫn lộn chữ hoa và chữ thường")
    public void testEmailWithMixedCase() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("YODY.Test01@Gmail.com");

        assertThat(authModal.getContinueBtnLoc()).isEnabled();
    }

    /**
     * Test Case ID: TC_DN_08
     * Tiêu đề: Nhập email có độ dài vượt giới hạn (> 256 ký tự)
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Báo email không hợp lệ hoặc vô hiệu hóa nút "Tiếp tục", giao diện không bị vỡ.
     */
    @Test(description = "TC_DN_08 - Nhập email có độ dài vượt giới hạn")
    public void testEmailExceedingLengthLimit() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        String longEmail = "a".repeat(260) + "@gmail.com";
        authModal.enterEmail(longEmail);

        if (authModal.isContinueButtonEnabled()) {
            OtpModal otpModal = authModal.clickContinue();
            // Form chặn không chuyển sang màn OTP, không vỡ layout
            assertThat(otpModal.getTitleLoc()).not().isVisible();
            assertThat(authModal.getTitleLoc()).isVisible();
        } else {
            assertThat(authModal.getContinueBtnLoc()).isDisabled();
        }
        assertThat(authModal.getModalContainerLoc()).isVisible();
    }

    /**
     * Test Case ID: TC_DN_09
     * Tiêu đề: Kiểm tra bảo mật chống SQL Injection vào ô Email
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chuỗi SQL Injection bị chặn bởi validation, nút "Tiếp tục" bị vô hiệu hóa hoặc không cho qua bước OTP,
     * hệ thống không bị lỗi 500 hay bypass đăng nhập.
     */
    @Test(description = "TC_DN_09 - Kiểm tra bảo mật SQL Injection trên ô Email")
    public void testSqlInjectionInEmailInput() {
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();

        authModal.enterEmail("' OR '1'='1");

        if (authModal.isContinueButtonEnabled()) {
            OtpModal otpModal = authModal.clickContinue();
            // Input bị chặn, không cho phép bypass sang bước OTP
            assertThat(otpModal.getTitleLoc()).not().isVisible();
            assertThat(authModal.getTitleLoc()).isVisible();
        } else {
            assertThat(authModal.getContinueBtnLoc()).isDisabled();
        }
        // Không phát sinh lỗi crash giao diện
        assertThat(authModal.getTitleLoc()).isVisible();
    }
}
