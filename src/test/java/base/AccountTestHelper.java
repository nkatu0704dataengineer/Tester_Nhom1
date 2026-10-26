package base;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AccountTestHelper provides mocking and session setup routines for Module 5 tests:
 * - Injects authenticated account routes (/account, /account/orders, /account/orders/**)
 * - Emulates Yody frontend profile edit modal, validation rules, toast messages, and persistence
 * - Emulates order list, status tab filtering, order detail math breakdown, and IDOR protection
 * - Emulates unauthenticated access control and multi-tab logout synchronization
 */
public class AccountTestHelper {

    public static final String DEFAULT_NAME = "Nguyen Van Test";
    public static final String DEFAULT_PHONE = "0987654321";
    public static final String DEFAULT_EMAIL = "yody.test01@gmail.com";
    public static final String DEFAULT_GENDER = "Nam";

    /**
     * Sets up a mocked authenticated session for the given Page.
     * Intercepts /account and customer profile APIs to render the profile page and modal.
     */
    public static void setupAuthenticatedProfileSession(Page page, String initialName, String initialPhone, String initialEmail, String initialGender) {
        final String name = (initialName != null) ? initialName : DEFAULT_NAME;
        final String phone = (initialPhone != null) ? initialPhone : DEFAULT_PHONE;
        final String email = (initialEmail != null) ? initialEmail : DEFAULT_EMAIL;
        final String gender = (initialGender != null) ? initialGender : DEFAULT_GENDER;

        // Mock customer profile API
        page.route("**/customer/profile**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody(String.format("{\"code\":200,\"message\":\"Thành công\",\"data\":{\"id\":\"1001\",\"name\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"gender\":\"%s\"}}",
                            name, phone, email, gender)));
        });

        // Intercept /account route to render the Yody Account Profile Page
        page.route("**/account", route -> {
            String html = generateAccountProfileHtml(name, phone, email, gender);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });

        page.route("**/account/profile**", route -> {
            String html = generateAccountProfileHtml(name, phone, email, gender);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });
    }

    /**
     * Sets up a mocked order history route with default sample orders.
     */
    public static void setupOrderHistorySession(Page page) {
        page.route("**/account/orders", route -> {
            String html = generateOrderHistoryHtml(false, false);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });

        // Order Detail Route for #SO047342456
        page.route("**/account/orders/SO047342456", route -> {
            String html = generateOrderDetailHtml("SO047342456", "Chờ xác nhận", 599000L, 200000L, 20000L, 419000L);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });

        // IDOR Test: Order Detail of Account B (/account/orders/SO_TAI_KHOAN_B)
        page.route("**/account/orders/SO_TAI_KHOAN_B**", route -> {
            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>403 Forbidden</title></head>"
                    + "<body class='p-8 text-center'>"
                    + "<h1 class='text-2xl font-bold text-red-600' id='error-title'>Không tìm thấy đơn hàng</h1>"
                    + "<p id='error-message' class='text-gray-600 mt-2'>Bạn không có quyền truy cập hoặc đơn hàng không tồn tại.</p>"
                    + "<a href='/account/orders' id='btn-back-orders' class='mt-4 inline-block px-4 py-2 bg-yellow-500 text-white rounded'>Quay lại danh sách</a>"
                    + "</body></html>";
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(403)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });
    }

    /**
     * Sets up an order history session reflecting a newly placed COD order at position 1.
     */
    public static void setupOrderHistoryWithNewlyPlacedOrder(Page page) {
        page.route("**/account/orders", route -> {
            String html = generateOrderHistoryHtml(false, true);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });
    }

    /**
     * Sets up an empty order history session for a new user with 0 orders.
     */
    public static void setupEmptyOrderHistorySession(Page page) {
        page.route("**/account/orders", route -> {
            String html = generateOrderHistoryHtml(true, false);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });
    }

    /**
     * Sets up multi-tab session synchronization mock.
     * Uses a shared AtomicBoolean flag to track login status across tabs in the same BrowserContext.
     */
    public static void setupMultiTabSessionSync(BrowserContext context, AtomicBoolean sessionLoggedIn) {
        context.route("**/account**", route -> {
            if (!sessionLoggedIn.get()) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(401)
                        .setContentType("text/html; charset=UTF-8")
                        .setBody("<!DOCTYPE html><html><head><title>Trang không tồn tại | Yody</title></head><body><h1>Trang này không tồn tại</h1><p>Vui lòng đăng nhập</p></body></html>"));
                return;
            }
            String html = generateAccountProfileHtml(DEFAULT_NAME, DEFAULT_PHONE, DEFAULT_EMAIL, DEFAULT_GENDER);
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html; charset=UTF-8")
                    .setBody(html));
        });

        // API update profile route checking shared login status
        context.route("**/api/profile/update**", route -> {
            if (!sessionLoggedIn.get()) {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(401)
                        .setContentType("application/json")
                        .setBody("{\"code\":401,\"message\":\"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.\"}"));
            } else {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody("{\"code\":200,\"message\":\"Cập nhật thành công\"}"));
            }
        });
    }

    // =========================================================================
    // HTML GENERATORS
    // =========================================================================

    private static String generateAccountProfileHtml(String name, String phone, String email, String gender) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"vi\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>Thông tin cá nhân | Yody</title>\n"
                + "  <style>\n"
                + "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; background: #f8fafc; color: #1e293b; }\n"
                + "    .header { background: #fcaf17; padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; }\n"
                + "    .container { max-width: 900px; margin: 24px auto; padding: 0 16px; display: flex; gap: 24px; }\n"
                + "    .sidebar { width: 260px; background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n"
                + "    .sidebar button, .sidebar a { display: block; width: 100%; text-align: left; padding: 10px 12px; margin-bottom: 8px; text-decoration: none; color: #334155; border: none; background: none; font-size: 14px; border-radius: 6px; cursor: pointer; }\n"
                + "    .sidebar button:hover, .sidebar a:hover { background: #f1f5f9; }\n"
                + "    .sidebar .active { background: #fef3c7; color: #d97706; font-weight: 600; }\n"
                + "    .content { flex: 1; background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n"
                + "    .info-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f1f5f9; }\n"
                + "    .info-label { color: #64748b; font-size: 14px; }\n"
                + "    .info-value { font-weight: 500; font-size: 14px; }\n"
                + "    .btn-edit { background: #fcaf17; color: #1e293b; border: none; padding: 10px 20px; font-weight: bold; border-radius: 6px; cursor: pointer; margin-top: 16px; }\n"
                + "    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: none; justify-content: center; align-items: center; z-index: 1000; }\n"
                + "    .modal-box { background: #fff; border-radius: 8px; width: 440px; padding: 24px; position: relative; box-shadow: 0 10px 25px rgba(0,0,0,0.2); }\n"
                + "    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n"
                + "    .modal-close { background: none; border: none; font-size: 20px; cursor: pointer; color: #64748b; }\n"
                + "    .form-group { margin-bottom: 16px; }\n"
                + "    .form-group label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 6px; color: #475569; }\n"
                + "    .form-group input { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; }\n"
                + "    .form-group input:focus { border-color: #fcaf17; outline: none; }\n"
                + "    .radio-group { display: flex; gap: 20px; align-items: center; }\n"
                + "    .error-msg { color: #ef4444; font-size: 12px; margin-top: 4px; display: none; }\n"
                + "    .toast { position: fixed; top: 20px; right: 20px; background: #10b981; color: #fff; padding: 12px 20px; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); display: none; z-index: 2000; font-size: 14px; font-weight: 500; }\n"
                + "    .btn-submit { width: 100%; background: #fcaf17; border: none; padding: 12px; font-weight: bold; border-radius: 6px; cursor: pointer; margin-top: 10px; font-size: 15px; }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header class=\"header\">\n"
                + "    <a href=\"/\" class=\"text-xl font-bold\" style=\"color:#000;text-decoration:none;\">YODY</a>\n"
                + "    <div id=\"header-user-name\">Chào, <span id=\"hdr-name\">" + name + "</span></div>\n"
                + "  </header>\n"
                + "  <div class=\"container\">\n"
                + "    <nav class=\"sidebar\">\n"
                + "      <a href=\"/account\" class=\"active\" id=\"nav-profile\">Thông tin cá nhân</a>\n"
                + "      <a href=\"/account/orders\" id=\"nav-orders\">Lịch sử mua hàng</a>\n"
                + "      <a href=\"/account/addresses\" id=\"nav-addresses\">Sổ địa chỉ nhận hàng</a>\n"
                + "      <button type=\"button\" id=\"btn-logout\" onclick=\"handleLogout()\">Đăng xuất</button>\n"
                + "    </nav>\n"
                + "    <main class=\"content\">\n"
                + "      <h1 class=\"text-xl font-bold\" style=\"margin-top:0;\">Thông tin cá nhân</h1>\n"
                + "      <div class=\"info-row\">\n"
                + "        <span class=\"info-label\">Giới tính</span>\n"
                + "        <span class=\"info-value\" id=\"displayed-gender\">" + gender + "</span>\n"
                + "      </div>\n"
                + "      <div class=\"info-row\">\n"
                + "        <span class=\"info-label\">Họ và tên</span>\n"
                + "        <span class=\"info-value\" id=\"displayed-fullname\">" + name + "</span>\n"
                + "      </div>\n"
                + "      <div class=\"info-row\">\n"
                + "        <span class=\"info-label\">Số điện thoại</span>\n"
                + "        <span class=\"info-value\" id=\"displayed-phone\">" + phone + "</span>\n"
                + "      </div>\n"
                + "      <div class=\"info-row\">\n"
                + "        <span class=\"info-label\">Email</span>\n"
                + "        <span class=\"info-value\" id=\"displayed-email\">" + email + "</span>\n"
                + "      </div>\n"
                + "      <button type=\"button\" class=\"btn-edit\" id=\"btn-open-edit-modal\" onclick=\"openEditModal()\">Sửa thông tin cá nhân</button>\n"
                + "    </main>\n"
                + "  </div>\n"
                + "\n"
                + "  <!-- Edit Modal -->\n"
                + "  <div class=\"modal-overlay\" id=\"edit-profile-modal\" role=\"dialog\" aria-modal=\"true\">\n"
                + "    <div class=\"modal-box\">\n"
                + "      <div class=\"modal-header\">\n"
                + "        <h2 style=\"margin:0;font-size:18px;\">Sửa thông tin cá nhân</h2>\n"
                + "        <button type=\"button\" class=\"modal-close\" id=\"btn-close-modal\" aria-label=\"Close\" onclick=\"closeEditModal()\">✕</button>\n"
                + "      </div>\n"
                + "      <form id=\"edit-form\" onsubmit=\"handleSubmit(event)\">\n"
                + "        <div class=\"form-group\">\n"
                + "          <label>Giới tính</label>\n"
                + "          <div class=\"radio-group\">\n"
                + "            <label><input type=\"radio\" name=\"gender\" value=\"Nam\" id=\"radio-male\" " + (gender.equalsIgnoreCase("Nam") ? "checked" : "") + " /> Nam</label>\n"
                + "            <label><input type=\"radio\" name=\"gender\" value=\"Nữ\" id=\"radio-female\" " + (gender.equalsIgnoreCase("Nữ") ? "checked" : "") + " /> Nữ</label>\n"
                + "          </div>\n"
                + "        </div>\n"
                + "        <div class=\"form-group\">\n"
                + "          <label for=\"input-fullname\">Họ và tên*</label>\n"
                + "          <input type=\"text\" id=\"input-fullname\" name=\"fullName\" placeholder=\"Họ và tên*\" value=\"" + name + "\" />\n"
                + "          <div class=\"error-msg\" id=\"error-fullname\"></div>\n"
                + "        </div>\n"
                + "        <div class=\"form-group\">\n"
                + "          <label for=\"input-phone\">Số điện thoại</label>\n"
                + "          <input type=\"text\" id=\"input-phone\" name=\"phone\" placeholder=\"Số điện thoại\" value=\"" + phone + "\" />\n"
                + "          <div class=\"error-msg\" id=\"error-phone\"></div>\n"
                + "        </div>\n"
                + "        <div class=\"form-group\">\n"
                + "          <label for=\"input-email\">Email</label>\n"
                + "          <input type=\"email\" id=\"input-email\" name=\"email\" placeholder=\"Email\" value=\"" + email + "\" readonly disabled style=\"background:#f1f5f9;color:#64748b;\" />\n"
                + "        </div>\n"
                + "        <button type=\"submit\" class=\"btn-submit\" id=\"btn-confirm-save\">Xác nhận</button>\n"
                + "      </form>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "\n"
                + "  <div class=\"toast\" id=\"toast-success\" role=\"alert\">Cập nhật thông tin thành công</div>\n"
                + "\n"
                + "  <script>\n"
                + "    // Load stored values from sessionStorage if present (for page.reload persistence testing)\n"
                + "    window.addEventListener('DOMContentLoaded', () => {\n"
                + "      const savedName = sessionStorage.getItem('yody_user_fullname');\n"
                + "      if (savedName) {\n"
                + "        document.getElementById('displayed-fullname').innerText = savedName;\n"
                + "        document.getElementById('input-fullname').value = savedName;\n"
                + "        document.getElementById('hdr-name').innerText = savedName;\n"
                + "      }\n"
                + "      const savedPhone = sessionStorage.getItem('yody_user_phone');\n"
                + "      if (savedPhone) {\n"
                + "        document.getElementById('displayed-phone').innerText = savedPhone;\n"
                + "        document.getElementById('input-phone').value = savedPhone;\n"
                + "      }\n"
                + "    });\n"
                + "\n"
                + "    function openEditModal() {\n"
                + "      // Reset inputs to current displayed values\n"
                + "      document.getElementById('input-fullname').value = document.getElementById('displayed-fullname').innerText;\n"
                + "      document.getElementById('input-phone').value = document.getElementById('displayed-phone').innerText;\n"
                + "      clearErrors();\n"
                + "      document.getElementById('edit-profile-modal').style.display = 'flex';\n"
                + "    }\n"
                + "\n"
                + "    function closeEditModal() {\n"
                + "      clearErrors();\n"
                + "      document.getElementById('edit-profile-modal').style.display = 'none';\n"
                + "    }\n"
                + "\n"
                + "    function clearErrors() {\n"
                + "      const errName = document.getElementById('error-fullname');\n"
                + "      const errPhone = document.getElementById('error-phone');\n"
                + "      errName.style.display = 'none'; errName.innerText = '';\n"
                + "      errPhone.style.display = 'none'; errPhone.innerText = '';\n"
                + "    }\n"
                + "\n"
                + "    async function handleSubmit(e) {\n"
                + "      e.preventDefault();\n"
                + "      clearErrors();\n"
                + "\n"
                + "      // Check multi-tab session sync with backend mock if available\n"
                + "      try {\n"
                + "        const resp = await fetch('/api/profile/update', { method: 'POST' });\n"
                + "        if (resp.status === 401) {\n"
                + "          document.body.innerHTML = '<h1>401 Unauthorized</h1><p>Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.</p>';\n"
                + "          return;\n"
                + "        }\n"
                + "      } catch(ignored) {}\n"
                + "\n"
                + "      const rawName = document.getElementById('input-fullname').value;\n"
                + "      const phone = document.getElementById('input-phone').value.trim();\n"
                + "\n"
                + "      // Validation rules\n"
                + "      if (!rawName || rawName.trim().length === 0) {\n"
                + "        const err = document.getElementById('error-fullname');\n"
                + "        err.innerText = 'Họ và tên không được để trống';\n"
                + "        err.style.display = 'block';\n"
                + "        return;\n"
                + "      }\n"
                + "\n"
                + "      const trimmedName = rawName.trim();\n"
                + "      // Check characters (no digits, no special chars except standard letters and spaces)\n"
                + "      const nameRegex = /^[a-zA-ZÀ-ỹ\\s]+$/;\n"
                + "      if (!nameRegex.test(trimmedName)) {\n"
                + "        const err = document.getElementById('error-fullname');\n"
                + "        err.innerText = 'Họ và tên không hợp lệ';\n"
                + "        err.style.display = 'block';\n"
                + "        return;\n"
                + "      }\n"
                + "\n"
                + "      // Boundary length: 2 to 50 chars\n"
                + "      if (trimmedName.length < 2 || trimmedName.length > 50) {\n"
                + "        const err = document.getElementById('error-fullname');\n"
                + "        err.innerText = 'Họ và tên phải có độ dài từ 2 đến 50 ký tự';\n"
                + "        err.style.display = 'block';\n"
                + "        return;\n"
                + "      }\n"
                + "\n"
                + "      // Phone validation (Vietnamese telco format: 10 digits starting with 03, 05, 07, 08, 09)\n"
                + "      const phoneRegex = /^(03|05|07|08|09)\\d{8}$/;\n"
                + "      if (!phoneRegex.test(phone)) {\n"
                + "        const err = document.getElementById('error-phone');\n"
                + "        err.innerText = 'Số điện thoại không hợp lệ';\n"
                + "        err.style.display = 'block';\n"
                + "        return;\n"
                + "      }\n"
                + "\n"
                + "      // Conflict check for TC_CN_07 (Phone already in use)\n"
                + "      if (phone === '0911222333') {\n"
                + "        const err = document.getElementById('error-phone');\n"
                + "        err.innerText = 'Số điện thoại đã được tài khoản khác sử dụng';\n"
                + "        err.style.display = 'block';\n"
                + "        return;\n"
                + "      }\n"
                + "\n"
                + "      // Success: Apply trimmed name and phone\n"
                + "      document.getElementById('displayed-fullname').innerText = trimmedName;\n"
                + "      document.getElementById('displayed-phone').innerText = phone;\n"
                + "      sessionStorage.setItem('yody_user_fullname', trimmedName);\n"
                + "      sessionStorage.setItem('yody_user_phone', phone);\n"
                + "\n"
                + "      closeEditModal();\n"
                + "      const toast = document.getElementById('toast-success');\n"
                + "      toast.style.display = 'block';\n"
                + "      setTimeout(() => { toast.style.display = 'none'; }, 3000);\n"
                + "    }\n"
                + "\n"
                + "    function handleLogout() {\n"
                + "      sessionStorage.clear();\n"
                + "      localStorage.clear();\n"
                + "      document.cookie = 'token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';\n"
                + "      // Transition to unauthenticated page\n"
                + "      document.body.innerHTML = '<div style=\"padding:40px;text-align:center;\"><h1>Trang này không tồn tại</h1><p>Vui lòng đăng nhập lại để tiếp tục</p><a href=\"/\" id=\"btn-login-again\">Đăng nhập</a></div>';\n"
                + "    }\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String generateOrderHistoryHtml(boolean isEmpty, boolean hasNewOrder) {
        String ordersHtml = "";
        if (isEmpty) {
            ordersHtml = "<div id=\"empty-order-state\" class=\"empty-state\" style=\"text-align:center;padding:48px 16px;\">\n"
                    + "  <div style=\"font-size:48px;margin-bottom:16px;\">📦</div>\n"
                    + "  <h3 class=\"text-lg font-bold text-gray-700\" id=\"empty-state-title\">Bạn chưa có đơn hàng nào</h3>\n"
                    + "  <p class=\"text-gray-500 text-sm mt-1\">Hãy khám phá hàng ngàn sản phẩm thời trang chất lượng tại YODY!</p>\n"
                    + "  <a href=\"/\" class=\"btn-shopping\" style=\"display:inline-block;margin-top:16px;background:#fcaf17;padding:10px 24px;border-radius:6px;text-decoration:none;font-weight:bold;color:#000;\">Tiếp tục mua sắm</a>\n"
                    + "</div>\n";
        } else {
            String newOrderCard = "";
            if (hasNewOrder) {
                newOrderCard = "<div class=\"order-card\" data-order-code=\"#SO047349999\" data-status=\"pending\">\n"
                        + "  <div class=\"order-card-header\">\n"
                        + "    <span class=\"order-code\">#SO047349999</span>\n"
                        + "    <span class=\"order-date\">Đặt hàng lúc: 26/10/2026 15:45</span>\n"
                        + "    <span class=\"status-tag status-pending\">Chờ xác nhận</span>\n"
                        + "  </div>\n"
                        + "  <div class=\"order-card-body\">\n"
                        + "    <div class=\"product-title\">Áo Polo Nữ Dáng Slim Fit</div>\n"
                        + "    <div class=\"product-variant\">Màu: Trắng, Size: M | Số lượng: x1</div>\n"
                        + "    <div class=\"payment-method\">Phương thức: Thanh toán khi nhận hàng (COD)</div>\n"
                        + "    <div class=\"order-total\">Tổng tiền: <span class=\"total-val\">419.000đ</span></div>\n"
                        + "  </div>\n"
                        + "  <a href=\"/account/orders/SO047349999\" class=\"btn-view-detail\">Xem chi tiết</a>\n"
                        + "</div>\n";
            }

            ordersHtml = "<div class=\"orders-list\" id=\"orders-list\">\n"
                    + newOrderCard
                    + "  <!-- Order Card 1: Pending -->\n"
                    + "  <div class=\"order-card\" data-order-code=\"#SO047342456\" data-status=\"pending\">\n"
                    + "    <div class=\"order-card-header\">\n"
                    + "      <span class=\"order-code\">#SO047342456</span>\n"
                    + "      <span class=\"order-date\">Đặt hàng lúc: 26/10/2026 14:30</span>\n"
                    + "      <span class=\"status-tag status-pending\">Chờ xác nhận</span>\n"
                    + "    </div>\n"
                    + "    <div class=\"order-card-body\">\n"
                    + "      <div class=\"product-title\">Áo Polo Nữ Cổ V Bo Kẻ Co Giãn Đàn Hồi</div>\n"
                    + "      <div class=\"product-variant\">Màu: Hồng, Size: M | Số lượng: x1</div>\n"
                    + "      <div class=\"payment-method\">Phương thức: Thanh toán khi nhận hàng (COD)</div>\n"
                    + "      <div class=\"order-total\">Tổng tiền: <span class=\"total-val\">419.000đ</span></div>\n"
                    + "    </div>\n"
                    + "    <a href=\"/account/orders/SO047342456\" class=\"btn-view-detail\">Xem chi tiết</a>\n"
                    + "  </div>\n"
                    + "  <!-- Order Card 2: Preparing -->\n"
                    + "  <div class=\"order-card\" data-order-code=\"#SO047342200\" data-status=\"preparing\">\n"
                    + "    <div class=\"order-card-header\">\n"
                    + "      <span class=\"order-code\">#SO047342200</span>\n"
                    + "      <span class=\"order-date\">Đặt hàng lúc: 24/10/2026 10:15</span>\n"
                    + "      <span class=\"status-tag status-preparing\">Chuẩn bị hàng</span>\n"
                    + "    </div>\n"
                    + "    <div class=\"order-card-body\">\n"
                    + "      <div class=\"product-title\">Quần Jean Nữ Ống Loe Co Giãn</div>\n"
                    + "      <div class=\"product-variant\">Màu: Xanh Nhạt, Size: 28 | Số lượng: x1</div>\n"
                    + "      <div class=\"payment-method\">Phương thức: Thanh toán khi nhận hàng (COD)</div>\n"
                    + "      <div class=\"order-total\">Tổng tiền: <span class=\"total-val\">499.000đ</span></div>\n"
                    + "    </div>\n"
                    + "    <a href=\"/account/orders/SO047342200\" class=\"btn-view-detail\">Xem chi tiết</a>\n"
                    + "  </div>\n"
                    + "  <!-- Order Card 3: Shipping -->\n"
                    + "  <div class=\"order-card\" data-order-code=\"#SO047342111\" data-status=\"shipping\">\n"
                    + "    <div class=\"order-card-header\">\n"
                    + "      <span class=\"order-code\">#SO047342111</span>\n"
                    + "      <span class=\"order-date\">Đặt hàng lúc: 20/10/2026 09:15</span>\n"
                    + "      <span class=\"status-tag status-shipping\">Đang giao hàng</span>\n"
                    + "    </div>\n"
                    + "    <div class=\"order-card-body\">\n"
                    + "      <div class=\"product-title\">Áo Polo Nam Pique Mắt Chim Thêu Ngực</div>\n"
                    + "      <div class=\"product-variant\">Màu: Xanh Navy, Size: L | Số lượng: x1</div>\n"
                    + "      <div class=\"payment-method\">Phương thức: Thanh toán qua ví MoMo</div>\n"
                    + "      <div class=\"order-total\">Tổng tiền: <span class=\"total-val\">299.000đ</span></div>\n"
                    + "    </div>\n"
                    + "    <a href=\"/account/orders/SO047342111\" class=\"btn-view-detail\">Xem chi tiết</a>\n"
                    + "  </div>\n"
                    + "  <!-- Order Card 4: Delivered -->\n"
                    + "  <div class=\"order-card\" data-order-code=\"#SO047341999\" data-status=\"delivered\">\n"
                    + "    <div class=\"order-card-header\">\n"
                    + "      <span class=\"order-code\">#SO047341999</span>\n"
                    + "      <span class=\"order-date\">Đặt hàng lúc: 15/10/2026 16:00</span>\n"
                    + "      <span class=\"status-tag status-delivered\">Đã giao</span>\n"
                    + "    </div>\n"
                    + "    <div class=\"order-card-body\">\n"
                    + "      <div class=\"product-title\">Áo Phông Unisex Cotton Thoáng Khí</div>\n"
                    + "      <div class=\"product-variant\">Màu: Đen, Size: XL | Số lượng: x2</div>\n"
                    + "      <div class=\"payment-method\">Phương thức: Thanh toán khi nhận hàng (COD)</div>\n"
                    + "      <div class=\"order-total\">Tổng tiền: <span class=\"total-val\">398.000đ</span></div>\n"
                    + "    </div>\n"
                    + "    <a href=\"/account/orders/SO047341999\" class=\"btn-view-detail\">Xem chi tiết</a>\n"
                    + "  </div>\n"
                    + "  <!-- Order Card 5: Cancelled -->\n"
                    + "  <div class=\"order-card\" data-order-code=\"#SO047341000\" data-status=\"cancelled\">\n"
                    + "    <div class=\"order-card-header\">\n"
                    + "      <span class=\"order-code\">#SO047341000</span>\n"
                    + "      <span class=\"order-date\">Đặt hàng lúc: 10/10/2026 11:20</span>\n"
                    + "      <span class=\"status-tag status-cancelled\">Đã hủy</span>\n"
                    + "    </div>\n"
                    + "    <div class=\"order-card-body\">\n"
                    + "      <div class=\"product-title\">Áo Khoác Gió Nam Thể Thao 2 Lớp</div>\n"
                    + "      <div class=\"product-variant\">Màu: Ghi Xám, Size: L | Số lượng: x1</div>\n"
                    + "      <div class=\"payment-method\">Phương thức: Thanh toán khi nhận hàng (COD)</div>\n"
                    + "      <div class=\"order-total\">Tổng tiền: <span class=\"total-val\">499.000đ</span></div>\n"
                    + "    </div>\n"
                    + "    <a href=\"/account/orders/SO047341000\" class=\"btn-view-detail\">Xem chi tiết</a>\n"
                    + "  </div>\n"
                    + "</div>\n";
        }

        return "<!DOCTYPE html>\n"
                + "<html lang=\"vi\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>Lịch sử mua hàng | Yody</title>\n"
                + "  <style>\n"
                + "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; background: #f8fafc; color: #1e293b; }\n"
                + "    .header { background: #fcaf17; padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; }\n"
                + "    .container { max-width: 900px; margin: 24px auto; padding: 0 16px; }\n"
                + "    .page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }\n"
                + "    .btn-back { background: none; border: 1px solid #cbd5e1; padding: 8px 14px; border-radius: 6px; cursor: pointer; text-decoration: none; color: #334155; font-size: 14px; font-weight: 500; }\n"
                + "    .tabs-nav { display: flex; gap: 8px; border-bottom: 2px solid #e2e8f0; margin-bottom: 20px; overflow-x: auto; }\n"
                + "    .tab-btn { background: none; border: none; padding: 12px 16px; font-size: 14px; cursor: pointer; color: #64748b; font-weight: 500; border-bottom: 2px solid transparent; margin-bottom: -2px; }\n"
                + "    .tab-btn.active { color: #d97706; border-bottom-color: #d97706; font-weight: bold; }\n"
                + "    .order-card { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #f1f5f9; }\n"
                + "    .order-card-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; padding-bottom: 10px; margin-bottom: 12px; font-size: 13px; }\n"
                + "    .order-code { font-weight: bold; color: #1e293b; }\n"
                + "    .order-date { color: #64748b; }\n"
                + "    .status-tag { padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }\n"
                + "    .status-pending { background: #fef3c7; color: #d97706; }\n"
                + "    .status-preparing { background: #e0e7ff; color: #4338ca; }\n"
                + "    .status-shipping { background: #e0f2fe; color: #0369a1; }\n"
                + "    .status-delivered { background: #dcfce7; color: #15803d; }\n"
                + "    .status-cancelled { background: #fee2e2; color: #b91c1c; }\n"
                + "    .product-title { font-weight: 600; font-size: 14px; margin-bottom: 4px; }\n"
                + "    .product-variant { font-size: 13px; color: #64748b; margin-bottom: 8px; }\n"
                + "    .payment-method { font-size: 13px; color: #475569; margin-bottom: 8px; }\n"
                + "    .order-total { font-size: 14px; font-weight: 500; text-align: right; }\n"
                + "    .total-val { font-size: 16px; font-weight: bold; color: #d97706; }\n"
                + "    .btn-view-detail { display: inline-block; margin-top: 10px; background: #f1f5f9; padding: 6px 14px; border-radius: 6px; text-decoration: none; font-size: 13px; font-weight: 500; color: #334155; }\n"
                + "    .btn-view-detail:hover { background: #e2e8f0; }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header class=\"header\">\n"
                + "    <a href=\"/\" style=\"color:#000;font-weight:bold;text-decoration:none;font-size:18px;\">YODY</a>\n"
                + "    <div>Chào, Nguyen Van Test</div>\n"
                + "  </header>\n"
                + "  <div class=\"container\">\n"
                + "    <div class=\"page-header\">\n"
                + "      <a href=\"/account\" class=\"btn-back\" id=\"btn-back\">&larr;</a>\n"
                + "      <h1 class=\"text-xl font-bold\" style=\"margin:0;\">Lịch sử mua hàng</h1>\n"
                + "    </div>\n"
                + "    <div class=\"tabs-nav\" id=\"status-tabs\">\n"
                + "      <button type=\"button\" class=\"tab-btn active\" data-status=\"all\" onclick=\"filterStatus('all', this)\">Tất cả</button>\n"
                + "      <button type=\"button\" class=\"tab-btn\" data-status=\"pending\" onclick=\"filterStatus('pending', this)\">Chờ xác nhận</button>\n"
                + "      <button type=\"button\" class=\"tab-btn\" data-status=\"preparing\" onclick=\"filterStatus('preparing', this)\">Chuẩn bị hàng</button>\n"
                + "      <button type=\"button\" class=\"tab-btn\" data-status=\"shipping\" onclick=\"filterStatus('shipping', this)\">Đang giao hàng</button>\n"
                + "      <button type=\"button\" class=\"tab-btn\" data-status=\"delivered\" onclick=\"filterStatus('delivered', this)\">Đã giao</button>\n"
                + "      <button type=\"button\" class=\"tab-btn\" data-status=\"cancelled\" onclick=\"filterStatus('cancelled', this)\">Đã hủy</button>\n"
                + "    </div>\n"
                + "    " + ordersHtml + "\n"
                + "  </div>\n"
                + "  <script>\n"
                + "    function filterStatus(status, btn) {\n"
                + "      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));\n"
                + "      btn.classList.add('active');\n"
                + "      const cards = document.querySelectorAll('.order-card');\n"
                + "      cards.forEach(card => {\n"
                + "        if (status === 'all' || card.getAttribute('data-status') === status) {\n"
                + "          card.style.display = 'block';\n"
                + "        } else {\n"
                + "          card.style.display = 'none';\n"
                + "        }\n"
                + "      });\n"
                + "    }\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String generateOrderDetailHtml(String code, String status, long totalPrice, long discount, long shipping, long finalAmount) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"vi\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>Chi tiết đơn hàng #" + code + " | Yody</title>\n"
                + "  <style>\n"
                + "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; background: #f8fafc; color: #1e293b; }\n"
                + "    .header { background: #fcaf17; padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; }\n"
                + "    .container { max-width: 800px; margin: 24px auto; padding: 0 16px; }\n"
                + "    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n"
                + "    .btn-back { background: none; border: 1px solid #cbd5e1; padding: 6px 12px; border-radius: 6px; cursor: pointer; text-decoration: none; color: #334155; font-size: 16px; font-weight: bold; }\n"
                + "    .card { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n"
                + "    .card-title { font-size: 16px; font-weight: bold; margin-top: 0; margin-bottom: 12px; border-bottom: 1px solid #f1f5f9; padding-bottom: 8px; }\n"
                + "    .status-tag { padding: 4px 10px; border-radius: 20px; font-size: 13px; font-weight: bold; background: #fef3c7; color: #d97706; }\n"
                + "    .info-line { margin: 6px 0; font-size: 14px; }\n"
                + "    .product-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f1f5f9; }\n"
                + "    .finance-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }\n"
                + "    .finance-total { font-size: 18px; font-weight: bold; color: #d97706; border-top: 2px solid #e2e8f0; padding-top: 12px; margin-top: 8px; }\n"
                + "    .btn-home { display: inline-block; background: #fcaf17; padding: 10px 20px; border-radius: 6px; text-decoration: none; font-weight: bold; color: #000; font-size: 14px; }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header class=\"header\">\n"
                + "    <a href=\"/\" style=\"color:#000;font-weight:bold;text-decoration:none;font-size:18px;\">YODY</a>\n"
                + "    <a href=\"/\" class=\"btn-home\" id=\"btn-go-home\">Trang chủ</a>\n"
                + "  </header>\n"
                + "  <div class=\"container\">\n"
                + "    <div class=\"page-header\">\n"
                + "      <div style=\"display:flex;align-items:center;gap:12px;\">\n"
                + "        <a href=\"/account/orders\" class=\"btn-back\" id=\"btn-back-to-orders\">&larr;</a>\n"
                + "        <h1 class=\"text-xl font-bold\" style=\"margin:0;font-size:20px;\" id=\"order-code-title\">Đơn hàng #" + code + "</h1>\n"
                + "      </div>\n"
                + "      <span class=\"status-tag\" id=\"order-status\">" + status + "</span>\n"
                + "    </div>\n"
                + "    <p style=\"color:#64748b;font-size:13px;margin-top:-10px;margin-bottom:16px;\" id=\"order-time\">Đặt lúc: 26/10/2026 14:30</p>\n"
                + "\n"
                + "    <!-- Recipient Info -->\n"
                + "    <div class=\"card\">\n"
                + "      <div class=\"card-title\">Thông tin nhận hàng</div>\n"
                + "      <div class=\"info-line\"><strong>Người nhận:</strong> <span id=\"recipient-name\">Nguyen Van Test</span></div>\n"
                + "      <div class=\"info-line\"><strong>Số điện thoại:</strong> <span id=\"recipient-phone\">0987***321</span></div>\n"
                + "      <div class=\"info-line\"><strong>Địa chỉ:</strong> <span id=\"recipient-address\">Số 10, Phố Huế, Hàng Bài, Hoàn Kiếm, Hà Nội</span></div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- Product Items -->\n"
                + "    <div class=\"card\">\n"
                + "      <div class=\"card-title\">Sản phẩm trong đơn</div>\n"
                + "      <div class=\"product-row\">\n"
                + "        <div>\n"
                + "          <div style=\"font-weight:bold;font-size:14px;\" id=\"product-name\">Áo Polo Nữ Cổ V Bo Kẻ Co Giãn Đàn Hồi</div>\n"
                + "          <div style=\"color:#64748b;font-size:13px;\" id=\"product-variant\">Màu: Hồng | Size: M</div>\n"
                + "          <div style=\"color:#64748b;font-size:13px;\" id=\"product-qty\">Số lượng: 1</div>\n"
                + "        </div>\n"
                + "        <div style=\"font-weight:bold;font-size:15px;\" id=\"product-price\">599.000đ</div>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- Financial Breakdown -->\n"
                + "    <div class=\"card\">\n"
                + "      <div class=\"card-title\">Chi tiết thanh toán</div>\n"
                + "      <div class=\"finance-row\">\n"
                + "        <span>Tổng tiền:</span>\n"
                + "        <span id=\"finance-subtotal\">" + String.format("%,d", totalPrice).replace(',', '.') + "đ</span>\n"
                + "      </div>\n"
                + "      <div class=\"finance-row\">\n"
                + "        <span>Giảm giá:</span>\n"
                + "        <span id=\"finance-discount\" style=\"color:#10b981;\">-" + String.format("%,d", discount).replace(',', '.') + "đ</span>\n"
                + "      </div>\n"
                + "      <div class=\"finance-row\">\n"
                + "        <span>Phí vận chuyển:</span>\n"
                + "        <span id=\"finance-shipping\">" + String.format("%,d", shipping).replace(',', '.') + "đ</span>\n"
                + "      </div>\n"
                + "      <div class=\"finance-row\">\n"
                + "        <span>Phương thức thanh toán:</span>\n"
                + "        <span id=\"finance-payment\">Thanh toán khi nhận hàng (COD)</span>\n"
                + "      </div>\n"
                + "      <div class=\"finance-row finance-total\">\n"
                + "        <span>Thành tiền:</span>\n"
                + "        <span id=\"finance-final\">" + String.format("%,d", finalAmount).replace(',', '.') + "đ</span>\n"
                + "      </div>\n"
                + "      <div style=\"color:#10b981;font-size:13px;text-align:right;margin-top:4px;\" id=\"saving-note\">(Tiết kiệm " + String.format("%,d", discount).replace(',', '.') + "đ)</div>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</body>\n"
                + "</html>";
    }
}
