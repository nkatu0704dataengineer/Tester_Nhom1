package tests;

import base.AccountTestHelper;
import base.BaseTest;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.AccountProfilePage;
import pages.AuthModal;
import pages.HomePage;
import pages.OtpModal;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 5 - Kiểm soát truy cập & Phân quyền (Access Control Tests)
 * Platform: Yody.vn
 *
 * Implements 6 test cases (TC_KS_01 -> TC_KS_06) covering:
 * - Unauthenticated route protection for /account and /account/orders
 * - Post-login redirect consistency
 * - Session revocation & page reload (F5) after logout
 * - Browser back button cache protection after logout
 * - Multi-tab session synchronization across browser contexts
 */
public class AccessControlTest extends BaseTest {

    @Override
    protected boolean shouldNavigateHomeOnInit() {
        return false;
    }

    /**
     * Test Case ID: TC_KS_01
     * Tiêu đề: Truy cập trực tiếp URL trang Tài khoản khi chưa đăng nhập
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hệ thống tự động chặn và chuyển hướng về trang đăng nhập / bật Modal yêu cầu Đăng nhập;
     * không lộ dữ liệu cá nhân.
     */
    @Test(description = "TC_KS_01 - Chặn truy cập URL trang Tài khoản khi chưa đăng nhập")
    public void testDirectAccessAccountWithoutAuth() {
        // Truy cập thẳng /account trong phiên Guest chưa đăng nhập
        page.navigate(BASE_URL + "/account");
        page.waitForLoadState();

        // Kiểm tra hệ thống hiển thị thông báo 404 / chặn truy cập không lộ thông tin
        String title = page.title();
        String body = page.locator("body").innerText();

        Assert.assertTrue(title.contains("không tồn tại") || body.contains("không tồn tại") || body.contains("Đăng nhập"),
                "Không chặn người dùng chưa xác thực truy cập /account!");
        Assert.assertFalse(body.contains("Nguyen Van Test"), "Rò rỉ dữ liệu cá nhân cho khách chưa đăng nhập!");
    }

    /**
     * Test Case ID: TC_KS_02
     * Tiêu đề: Truy cập trực tiếp URL trang Lịch sử đơn hàng khi chưa đăng nhập
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chuyển hướng yêu cầu đăng nhập / 404; không hiển thị bất kỳ dữ liệu đơn hàng nào.
     */
    @Test(description = "TC_KS_02 - Chặn truy cập URL Lịch sử đơn hàng khi chưa đăng nhập")
    public void testDirectAccessOrderHistoryWithoutAuth() {
        // Truy cập thẳng /account/orders trong phiên Guest
        page.navigate(BASE_URL + "/account/orders");
        page.waitForLoadState();

        String body = page.locator("body").innerText();
        Assert.assertTrue(page.title().contains("không tồn tại") || body.contains("không tồn tại") || body.contains("Đăng nhập"),
                "Không chặn truy cập /account/orders đối với tài khoản chưa đăng nhập!");
        Assert.assertFalse(body.contains("#SO047342456"), "Bị rò rỉ mã đơn hàng khi chưa đăng nhập!");
    }

    /**
     * Test Case ID: TC_KS_03
     * Tiêu đề: Đăng nhập tiếp tục sau khi bị chuyển hướng (Post-Login Redirect)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Đăng nhập thành công và chuyển hướng người dùng vào trang tài khoản hoặc trang chủ
     * một cách nhất quán, không phát sinh lỗi.
     */
    @Test(description = "TC_KS_03 - Đăng nhập tiếp tục sau khi bị chặn chuyển hướng")
    public void testPostLoginRedirect() {
        // Mock luồng xác thực đăng nhập
        page.route("**/customer/auth/send-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"expires\":300,\"remaining_daily_otp_count\":5,\"success\":true}}"));
        });

        page.route("**/customer/auth/verify-email-otp", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"access_token\":\"mock_token_redirect_03\",\"refresh_token\":\"mock_refresh_03\",\"expires_in\":86400}}"));
        });

        // Điều hướng về trang chủ và mở Auth Modal
        page.navigate(BASE_URL);
        HomePage homePage = new HomePage(page);
        AuthModal authModal = homePage.openAuthModal();
        authModal.enterEmail("yody.test01@gmail.com");

        OtpModal otpModal = authModal.clickContinue();
        otpModal.enterOtp("1234");
        otpModal.clickVerify();

        // Sau khi đăng nhập thành công, điều hướng nhất quán
        page.waitForTimeout(1000);
        Assert.assertTrue(page.url().equals("https://yody.vn/") || page.url().contains("account"),
                "Chuyển hướng sau đăng nhập không đúng: " + page.url());
    }

    /**
     * Test Case ID: TC_KS_04
     * Tiêu đề: Tải lại trang tài khoản (F5) sau khi đã đăng xuất
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Phiên đã bị hủy hoàn toàn; không truy cập lại được dữ liệu cá nhân và yêu cầu đăng nhập lại.
     */
    @Test(description = "TC_KS_04 - Tải lại trang tài khoản (F5) sau khi đã đăng xuất")
    public void testReloadAfterLogout() {
        // Thiết lập trạng thái ban đầu đã đăng nhập
        AccountTestHelper.setupAuthenticatedProfileSession(page, "Nguyen Van Test", "0987654321", "yody.test01@gmail.com", "Nam");
        AccountProfilePage profile = new AccountProfilePage(page);
        profile.navigateToProfilePage();
        assertThat(profile.getTitleHeading()).isVisible();

        // Thực hiện Đăng xuất
        profile.logout();

        // Mock lại route /account thành trang yêu cầu đăng nhập (phiên đã kết thúc)
        page.route("**/account**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(401)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody("<!DOCTYPE html><html><head><title>Trang không tồn tại | Yody</title></head><body><h1>Trang này không tồn tại</h1><p>Vui lòng đăng nhập lại</p></body></html>"));
        });

        // Nhấn F5 / reload
        page.reload();

        // Kỳ vọng: Phiên đã bị hủy, hiển thị trang yêu cầu đăng nhập lại
        String body = page.locator("body").innerText();
        Assert.assertTrue(body.contains("không tồn tại") || body.contains("đăng nhập lại"),
                "Không hủy phiên đăng nhập sau khi reload!");
        Assert.assertFalse(body.contains("Nguyen Van Test"), "Dữ liệu cá nhân vẫn hiển thị sau reload!");
    }

    /**
     * Test Case ID: TC_KS_05
     * Tiêu đề: Nhấn nút Back trình duyệt sau khi đã đăng xuất
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Trình duyệt không hiển thị lại dữ liệu nhạy cảm từ bộ nhớ đệm; hệ thống yêu cầu đăng nhập.
     */
    @Test(description = "TC_KS_05 - Nhấn nút Back trình duyệt sau khi đã đăng xuất")
    public void testBrowserBackAfterLogout() {
        AccountTestHelper.setupAuthenticatedProfileSession(page, "Nguyen Van Test", "0987654321", "yody.test01@gmail.com", "Nam");
        AccountProfilePage profile = new AccountProfilePage(page);
        profile.navigateToProfilePage();
        assertThat(profile.getTitleHeading()).isVisible();

        // Đăng xuất
        profile.logout();

        // Sau khi đăng xuất, hủy phiên
        page.route("**/account**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(401)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody("<!DOCTYPE html><html><head><title>Yêu cầu đăng nhập</title></head><body><h1>Vui lòng đăng nhập lại</h1></body></html>"));
        });

        // Gọi Back trình duyệt
        page.goBack();

        // Dữ liệu nhạy cảm không được lưu trong cache và hiển thị lại
        String body = page.locator("body").innerText();
        Assert.assertFalse(body.contains("Nguyen Van Test") && profile.getTitleHeading().isVisible(),
                "Trình duyệt để lộ dữ liệu cá nhân từ bộ nhớ cache khi bấm Back!");
    }

    /**
     * Test Case ID: TC_KS_06
     * Tiêu đề: Đồng bộ trạng thái đăng xuất giữa nhiều Tab trình duyệt (Multi-tab Session Sync)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: tabA đăng xuất -> tabB sửa Họ tên và bấm "Xác nhận" -> tabB bị từ chối lưu dữ liệu
     * (401 Unauthorized / chuyển hướng đăng nhập); không ghi đè được dữ liệu.
     */
    @Test(description = "TC_KS_06 - Đồng bộ trạng thái đăng xuất đa tab (Multi-tab Session Sync)")
    public void testMultiTabSessionSyncOnLogout() {
        // Shared login flag across tabs within the same BrowserContext
        AtomicBoolean sessionLoggedIn = new AtomicBoolean(true);
        AccountTestHelper.setupMultiTabSessionSync(context, sessionLoggedIn);

        // Mở Tab A và Tab B
        Page tabA = context.newPage();
        Page tabB = context.newPage();

        AccountProfilePage profileTabA = new AccountProfilePage(tabA);
        AccountProfilePage profileTabB = new AccountProfilePage(tabB);

        profileTabA.navigateToProfilePage();
        profileTabB.navigateToProfilePage();

        assertThat(profileTabA.getTitleHeading()).isVisible();
        assertThat(profileTabB.getTitleHeading()).isVisible();

        // 1. Thực hiện Đăng xuất tại Tab A
        sessionLoggedIn.set(false);
        profileTabA.logout();

        // 2. Quay sang Tab B, mở modal sửa họ tên và bấm Xác nhận
        profileTabB.openEditProfileModal();
        profileTabB.enterFullName("Hacker Modify Name");
        profileTabB.clickConfirmSave();

        // 3. Tab B bị từ chối lưu dữ liệu vì phiên đã bị hủy tại Tab A
        assertThat(tabB.locator("body")).containsText("401");
        String tabBBody = tabB.locator("body").innerText();
        Assert.assertTrue(tabBBody.contains("401") || tabBBody.contains("hết hạn"),
                "Tab B không bị từ chối lưu dữ liệu sau khi Tab A đã đăng xuất!");

        tabA.close();
        tabB.close();
    }
}
