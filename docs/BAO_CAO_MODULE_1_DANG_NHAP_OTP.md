# BÁO CÁO KẾT QUẢ TRIỂN KHAI BỘ KIỂM THỬ TỰ ĐỘNG (AUTOMATION TEST REPORT)
## MODULE 1: ĐĂNG KÝ, ĐĂNG NHẬP & XÁC THỰC TÀI KHOẢN (PASSWORDLESS OTP) - YODY.VN

- **Dự án:** Automation Testing for E-Commerce Platform Yody.vn
- **Vai trò thực hiện:** Senior Automation QA Engineer
- **Ngôn ngữ & Nền tảng:** Java (JDK 17+) + Microsoft Playwright for Java + TestNG + Maven
- **Kiến trúc thiết kế:** Page Object Model (POM) kết hợp Playwright Tracing & API Mocking
- **Trạng thái thực thi:** **18/18 Test Cases PASSED (100%)**

---

## 1. TỔNG QUAN VÀ MỤC TIÊU DỰ ÁN

Module 1 tập trung kiểm thử toàn diện quy trình xác thực người dùng không mật khẩu (Passwordless Authentication via Email OTP) trên nền tảng thương mại điện tử **Yody.vn**. Hệ thống sử dụng cơ chế popup modal nổi trực tiếp trên trang chủ với 2 màn hình:
1. **Màn hình 1 (Nhập Email):** Tiếp nhận email khách hàng, kiểm tra định dạng và gọi API gửi mã xác thực.
2. **Màn hình 2 (Xác thực OTP):** Gồm 4 ô input riêng biệt (thành phần React `input-otp`), đồng hồ đếm ngược (countdown timer), giới hạn lượt gửi trong ngày, cơ chế gửi lại mã (Resend OTP) và xác thực phiên đăng nhập.

### Mục tiêu kỹ thuật đạt được:
- Độc lập hóa môi trường kiểm thử với **API Mocking (`page.route()`)**, không làm spam hòm thư thật, không tạo tài khoản rác và không phụ thuộc vào hạ tầng backend production khi chạy tự động.
- Áp dụng triệt để cơ chế **Auto-waiting** và **Web-First Assertions (`assertThat`)** của Playwright; tuyệt đối không dùng `Thread.sleep()`.
- Tích hợp **Playwright Trace Viewer** tự động kích hoạt và xuất file vết nén `.zip` khi có bất kỳ ca kiểm thử nào thất bại.

---

## 2. CẤU TRÚC THƯ MỤC VÀ TÀI NGUYÊN DỰ ÁN

```text
c:\Users\Admin\Tester_Nhom1\
├── pom.xml                                       # Quản lý dependencies (Playwright, TestNG) & Maven Surefire
├── testng.xml                                    # TestNG Suite runner điều phối thực thi 2 test classes
├── docs/
│   └── BAO_CAO_MODULE_1_DANG_NHAP_OTP.md         # Báo cáo kỹ thuật chi tiết Module 1
├── traces/                                       # Thư mục lưu vết Playwright Traces (.zip)
└── src/test/java/
    ├── base/
    │   └── BaseTest.java                         # Quản lý vòng đời Browser, Context, Page, Tracing, Auto-dismiss Popup
    ├── pages/
    │   ├── HomePage.java                         # POM Trang chủ: Nút Đăng nhập Header
    │   ├── AuthModal.java                        # POM Màn hình 1: Nhập Email xác thực
    │   └── OtpModal.java                         # POM Màn hình 2: Xác thực mã OTP 4 số
    └── tests/
        ├── AuthEmailTest.java                    # 9 Test Cases kiểm thử Form Email
        └── OtpVerificationTest.java              # 9 Test Cases kiểm thử Xác thực OTP & API Mocking
```

---

## 3. THIẾT KẾ KIẾN TRÚC & PAGE OBJECT MODEL (POM)

### 3.1. `BaseTest.java` (src/test/java/base/BaseTest.java)
- **Vòng đời:**
  - `@BeforeClass`: Khởi tạo đối tượng `Playwright` và `Browser` (Chromium). Mặc định chạy `headless = false` để quan sát trực quan, hỗ trợ cấu hình qua biến môi trường `-Dheadless=true`.
  - `@BeforeMethod`: Khởi tạo phiên `BrowserContext` cô lập hoàn toàn (viewport 1920x1080), kích hoạt `context.tracing().start()`.
  - `@AfterMethod`: Phân tích kết quả test qua `ITestResult`. Nếu test case bị **FAILED**, hệ thống lưu toàn bộ video trace, snapshot, DOM tree và log mạng thành file `traces/{methodName}_{timestamp}.zip`. Nếu **PASSED**, dừng trace và giải phóng tài nguyên.
  - `closePromotionalPopupsIfAny()`: Tự động phát hiện và đóng pop-up quảng cáo/voucher xuất hiện trên trang chủ Yody trước khi vào test.

### 3.2. `HomePage.java` (src/test/java/pages/HomePage.java)
- **Định vị phần tử:** Nút "Đăng nhập" ở góc phải thanh điều hướng Header (`headerLoginButton`).
- **Nghiệp vụ:** `openAuthModal()`: Click vào nút Đăng nhập và trả về đối tượng `AuthModal`.

### 3.3. `AuthModal.java` (src/test/java/pages/AuthModal.java)
- **Định vị phần tử:**
  - `modalContainer`: `div.fixed.inset-0.z-[70]` (khung dialog nổi).
  - `title`: `getByText("Đăng nhập / Đăng ký")`.
  - `emailInput`: `locator("input[name='email'], input[type='email']")`.
  - `continueButton`: `getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Tiếp tục").setExact(true))`.
  - `googleLoginButton`: `getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Tiếp tục với Google"))`.
  - `closeButton`: `locator("button[aria-label='Close dialog']")`.
- **Nghiệp vụ:** `enterEmail(String email)`, `clearEmail()`, `clickContinue()`, `closeModal()`, `isContinueButtonEnabled()`.

### 3.4. `OtpModal.java` (src/test/java/pages/OtpModal.java)
- **Đặc thù UI:** React `input-otp` gồm 1 input nền trong suốt (`input[data-slot='input-otp']` với `pattern="^\d+$"`, `maxlength="4"`) và 4 visual slots (`[data-slot='input-otp-slot']`).
- **Định vị phần tử:**
  - `title`: `getByText("Nhập mã OTP")`.
  - `emailNotificationText`: Dòng text `Vui lòng kiểm tra email được gửi đến...`.
  - `otpHiddenInput`: Ô input 4 chữ số OTP.
  - `otpSlots`: 4 ô slot hiển thị số.
  - `countdownTimerText`: Bộ đếm ngược `sau 04:56`.
  - `remainingSendsText`: Số lần gửi OTP còn lại.
  - `verifyButton`: Nút `"Tiếp tục"` để xác nhận mã.
  - `resendButton`: Nút `"Gửi lại mã"`.
  - `backButton`: Nút quay lại màn hình nhập Email (`<-`).
  - `errorMessage`: `.text-negative-text, [data-slot='input-otp-group'] + div, [data-slot='input-otp-slot'].border-border-negative`.
- **Nghiệp vụ:** `enterOtp(String otp)`, `clearOtp()`, `clickVerify()`, `clickResendOtp()`, `clickBack()`, `closeModal()`.

---

## 4. CHI TIẾT 18 KỊCH BẢN KIỂM THỬ VÀ KẾT QUẢ THỰC THI

### 4.1. Suite 1: `AuthEmailTest.java` (Kiểm thử biểu mẫu Email)

| Test Case ID | Mức ưu tiên | Tên kịch bản | Dữ liệu đầu vào | Kết quả kỳ vọng | Trạng thái |
| :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_DN_01** | Cao (P1) | Đăng nhập thành công bằng email hợp lệ | `yody.test01@gmail.com` | Mock API `send-email-otp` trả về 200 OK. Hệ thống chuyển sang màn hình OTP thành công. | **PASSED** |
| **TC_DN_02** | Cao (P1) | Để trống ô Email | `""` | Nút `"Tiếp tục"` bị vô hiệu hóa (`isDisabled()`). | **PASSED** |
| **TC_DN_03** | Trung bình (P2) | Nhập email thiếu ký tự `@` | `yody.test01gmail.com` | Form chặn submit, không chuyển sang màn hình OTP. | **PASSED** |
| **TC_DN_04** | Trung bình (P2) | Nhập email thiếu tên miền | `yody.test01@` | Form chặn submit, không chuyển sang màn hình OTP. | **PASSED** |
| **TC_DN_05** | Trung bình (P2) | Nhập email chứa khoảng trắng và ký tự lỗi | `yody test@@gmail.com` | Form chặn submit, không chuyển sang màn hình OTP. | **PASSED** |
| **TC_DN_06** | Trung bình (P2) | Nhập email có khoảng trắng đầu/cuối chuỗi | `  yody.test01@gmail.com  ` | Hệ thống tự động trim khoảng trắng thừa, nút Tiếp tục sáng và cho phép tiếp tục. | **PASSED** |
| **TC_DN_07** | Trung bình (P2) | Nhập email lẫn lộn chữ hoa/chữ thường | `YODY.Test01@Gmail.com` | Chấp nhận định dạng hợp lệ (Case-insensitive), nút Tiếp tục enabled. | **PASSED** |
| **TC_DN_08** | Thấp (P3) | Nhập email vượt độ dài quy định (>256 ký tự) | Chuỗi 260 ký tự `a` + `@gmail.com` | Form chặn submit, không chuyển sang màn OTP, layout không bị vỡ. | **PASSED** |
| **TC_DN_09** | Cao (P1) | Kiểm tra bảo mật chống SQL Injection | `' OR '1'='1` | Validation chặn xử lý, không lỗi HTTP 500, không bypass xác thực. | **PASSED** |

---

### 4.2. Suite 2: `OtpVerificationTest.java` (Kiểm thử Xác thực mã OTP)

| Test Case ID | Mức ưu tiên | Tên kịch bản | Dữ liệu đầu vào & Mock API | Kết quả kỳ vọng | Trạng thái |
| :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_OTP_01** | Cao (P1) | Nhập sai mã OTP | `"0000"`, Mock 400 (`Mã không hợp lệ`) | Hiển thị thông báo lỗi inline dưới 4 ô OTP, giữ nguyên modal để nhập lại. | **PASSED** |
| **TC_OTP_02** | Cao (P1) | Để trống toàn bộ các ô mã OTP | Chưa nhập ký tự | Nút `"Tiếp tục"` bị vô hiệu hóa (`isDisabled()`). | **PASSED** |
| **TC_OTP_03** | Trung bình (P2) | Nhập chữ cái và ký tự đặc biệt | `"ab#!"` | Thuộc tính numeric chặn ký tự, ô OTP không nhận chữ, nút Tiếp tục disabled. | **PASSED** |
| **TC_OTP_04** | Trung bình (P2) | Nhập thiếu chữ số OTP | `"123"` (chỉ có 3 số) | Nút `"Tiếp tục"` vẫn bị vô hiệu hóa khi chưa đủ 4 số. | **PASSED** |
| **TC_OTP_05** | Cao (P1) | Nhập mã OTP đã hết hạn hiệu lực | `"9999"`, Mock 400 Expired | Hiển thị lỗi OTP đã hết hạn, duy trì modal để gửi lại mã mới. | **PASSED** |
| **TC_OTP_06** | Cao (P1) | Nhập sai OTP vượt quá số lần quy định | `"8888"`, Mock 429 Rate Limit | Hiển thị cảnh báo tạm khóa xác thực, chặn đăng nhập. | **PASSED** |
| **TC_OTP_07** | Cao (P1) | Gửi lại mã OTP và xác thực thành công | `"6789"`, Mock 200 Success | Cập nhật bộ đếm, xác thực thành công tự động đóng modal OTP. | **PASSED** |
| **TC_OTP_08** | Trung bình (P2) | Dùng lại mã OTP cũ sau khi đã gửi lại mã | `"1111"`, Mock 400 Replaced OTP | Hệ thống từ chối mã cũ và hiển thị thông báo lỗi. | **PASSED** |
| **TC_OTP_09** | Cao (P1) | Dùng lại mã OTP đã xác thực thành công | `"5555"`, Mock 400 Replay Attack | Hệ thống từ chối vì OTP chỉ có giá trị sử dụng một lần duy nhất. | **PASSED** |

---

## 5. TỔNG KẾT KẾT QUẢ THỰC THI TỪ MAVEN SUREFIRE

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running TestSuite
SLF4J: Defaulting to no-operation (NOP) logger implementation
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 109.6 s -- in TestSuite
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:51 min
[INFO] Finished at: 2026-10-26T16:29:10+07:00
[INFO] ------------------------------------------------------------------------
```

---

## 6. HƯỚNG DẪN THỰC THI & DEBUG DỰ ÁN

1. **Chạy toàn bộ Test Suite có giao diện (Headless = false):**
   ```powershell
   mvn test
   ```
2. **Chạy toàn bộ Test Suite ngầm trên CI/CD (Headless = true):**
   ```powershell
   mvn test -Dheadless=true
   ```
3. **Chạy riêng lẻ một Test Class:**
   ```powershell
   # Chỉ chạy Email Tests
   mvn test -Dtest=AuthEmailTest -Dheadless=true

   # Chỉ chạy OTP Verification Tests
   mvn test -Dtest=OtpVerificationTest -Dheadless=true
   ```
4. **Mở Playwright Trace Viewer để điều tra ca lỗi (nếu có):**
   ```powershell
   mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace traces/<tên_file_trace>.zip"
   ```
