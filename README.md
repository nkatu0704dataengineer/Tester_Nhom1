# Yody.vn E-Commerce End-to-End Test Automation Framework

[![Java 17+](https://img.shields.io/badge/Java-17%20%7C%2021-ED8B00.svg?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Playwright for Java](https://img.shields.io/badge/Playwright-1.48.0-2EAD33.svg?style=flat-square&logo=playwright&logoColor=white)](https://playwright.dev/java/)
[![TestNG](https://img.shields.io/badge/TestNG-7.10.2-FF6F00.svg?style=flat-square&logo=testng&logoColor=white)](https://testng.org/)
[![Apache Maven](https://img.shields.io/badge/Maven-3.8+-C71A36.svg?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Architecture](https://img.shields.io/badge/Design%20Pattern-Page%20Object%20Model%20(POM)-blueviolet.svg?style=flat-square)](#1-kiến-trúc-hệ-thống-system-architecture)
[![Coverage](https://img.shields.io/badge/Modules%20Covered-5%20Modules%20%7C%2050+%20TCs-success.svg?style=flat-square)](#2-các-đột-phá-kỹ-thuật-cốt-lõi-core-engineering-highlights)
[![License](https://img.shields.io/badge/License-Apache%202.0-black.svg?style=flat-square)](LICENSE)

Hệ thống kiểm thử tự động toàn diện mức độ doanh nghiệp (Enterprise-Grade E2E Automation Framework) được phát triển cho nền tảng thương mại điện tử thời trang **Yody.vn** dựa trên ngăn xếp công nghệ hiện đại **Java 17+**, **Microsoft Playwright for Java**, **TestNG** và mô hình thiết kế chuẩn mực **Page Object Model (POM)**.

Dự án bao gồm toàn bộ **5 Module cốt lõi** với hơn 50+ Test Cases nghiệp vụ chuyên sâu:
1. **Module 1**: Đăng ký, Đăng nhập & Xác thực OTP (`AuthModal`, `OtpModal`).
2. **Module 2**: Tìm kiếm thông minh, Gợi ý từ khóa & Lọc đa tiêu chí (`SearchAndFilterPage`).
3. **Module 3**: Chi tiết sản phẩm (PDP), Giỏ hàng & Bộ quy tắc Voucher (`ProductDetailPage`, `CartPage`, `VoucherModal`).
4. **Module 4**: Xác nhận đặt hàng, Cổng thanh toán MoMo & Chặn đơn hàng thật trên Production (`OrderConfirmationPage`, `MoMoPaymentGatewayPage`).
5. **Module 5**: Hồ sơ cá nhân, Lịch sử đơn hàng, Kiểm soát bảo mật IDOR & Đồng bộ phiên đa tab (`AccountProfilePage`, `OrderHistoryPage`, `OrderDetailPage`, `AccessControlTest`).

Hệ thống tích hợp **Cơ chế an toàn Production (Production Safety Guardrails)** qua `page.route()`, **Tối ưu hóa phiên chống Rate-Limit/Cloudflare**, **Kiểm thử phân tích tài chính số học tự động**, và **Playwright Tracing & Video Recording**.

---

## Mục lục (Table of Contents)

1. [Kiến trúc hệ thống (System Architecture)](#1-kiến-trúc-hệ-thống-system-architecture)
2. [Các đột phá kỹ thuật cốt lõi (Core Engineering Highlights)](#2-các-đột-phá-kỹ-thuật-cốt-lõi-core-engineering-highlights)
3. [Bảng tra cứu lệnh chạy theo nhu cầu (Execution Command Matrix)](#3-bảng-tra-cứu-lệnh-chạy-theo-nhu-cầu-execution-command-matrix)
4. [Hướng dẫn chi tiết từng kịch bản chạy (Detailed Execution Scenarios)](#4-hướng-dẫn-chi-tiết-từng-kịch-bản-chạy-detailed-execution-scenarios)
5. [Cấu hình Profiles & Môi trường thực thi (Environment & Configuration)](#5-cấu-hình-profiles--môi-trường-thực-thi-environment--configuration)
6. [Công nghệ sử dụng (Technology Stack)](#6-công-nghệ-sử-dụng-technology-stack)
7. [Cấu trúc mã nguồn (Repository Structure)](#7-cấu-trúc-mã-nguồn-repository-structure)
8. [Cài đặt & Khởi động nhanh (Getting Started & Setup)](#8-cài-đặt--khởi-động-nhanh-getting-started--setup)
9. [Cấu trúc dữ liệu đầu ra & Báo cáo (Output Artifacts & Reports)](#9-cấu-trúc-dữ-liệu-đầu-ra--báo-cáo-output-artifacts--reports)
10. [Giấy phép & Đóng góp (License & Attribution)](#10-giấy-phép--đóng-góp-license--attribution)

---

## 1. Kiến trúc hệ thống (System Architecture)

```mermaid
flowchart TD
    subgraph Config_Base ["1. Base Test & Lifecycle Management Layer"]
        CONF["pom.xml / testng.xml\n(Execution Suite Config & Dependencies)"] --> BASE["BaseTest.java\n(Playwright, Browser, BrowserContext, Page Lifecycle)"]
        BASE --> TRACE["Playwright Tracing & Video Capture\n(Traces on Failure / Success Recordings)"]
        BASE --> HOOK["shouldNavigateHomeOnInit() Hook\n(Dynamic Home Navigation Guard & Cloudflare Shield)"]
    end

    subgraph Network_Security ["2. Network Interception & Production Safety Guard"]
        BASE --> ROUTE["Playwright Route Interception (page.route)\n(Zero Production Pollution Guardrail)"]
        ROUTE -->|POST /api/orders/checkout| MOCK_ORDER["Mock Checkout Response\n(HTTP 200 Order ID #YODY-E2E-TEST)"]
        ROUTE -->|GET /api/profile| MOCK_AUTH["Mock Session & Cookie/Token Inject\n(Authenticated State Simulation)"]
        ROUTE -->|GET /api/orders/{id}| IDOR_CHECK["IDOR Security Barrier (403 Forbidden)\n(Cross-Tenant Access Verification)"]
    end

    subgraph POM_Domain ["3. Page Object Model (POM) Domain Layer"]
        BASE --> HP["HomePage.java\n(Header, Mega Menu, Search Drawer)"]
        BASE --> AUTH["AuthModal.java & OtpModal.java\n(Login/Register, 6-digit OTP Input, Timer)"]
        BASE --> SEARCH["SearchAndFilterPage.java\n(Keyword Suggestion, Faceted Filter, Sorting)"]
        BASE --> PDP["ProductDetailPage.java\n(SKU, Color, Size, Quantity, Buy Now)"]
        BASE --> CART["CartPage.java & VoucherModal.java\n(Cart CRUD, Voucher Thresholds & Discount Math)"]
        BASE --> CHECKOUT["OrderConfirmationPage.java & MoMoPaymentGatewayPage.java\n(Form Filling, MoMo QR Simulator, Intercepted Submit)"]
        BASE --> ACC["AccountProfilePage.java & OrderHistoryPage.java & OrderDetailPage.java\n(Profile Updates, Tab Filtering, Financial Breakdown Math)"]
    end

    subgraph TestNG_Execution ["4. TestNG Test Suites Layer"]
        HP & AUTH --> M1["Module 1: AuthEmailTest, OtpVerificationTest"]
        SEARCH --> M2["Module 2: SearchKeywordTest, ProductFilterAndSortTest"]
        PDP & CART --> M3["Module 3: ProductDetailTest, CartManagementTest, VoucherTest"]
        CHECKOUT --> M4["Module 4: OrderConfirmationTest, PaymentGatewayTest"]
        ACC --> M5["Module 5: AccountProfileTest, OrderHistoryTest, AccessControlTest"]
    end

    subgraph Artifacts_Report ["5. Output Artifacts & Provenance Layer"]
        M1 & M2 & M3 & M4 & M5 --> SUREFIRE["target/surefire-reports/\n(HTML Emailable Report, XML Test Results)"]
        M1 & M2 & M3 & M4 & M5 --> TRACE_ZIP["traces/\n(Playwright Action Traces .zip with DOM & Timing)"]
        M1 & M2 & M3 & M4 & M5 --> DOCS["docs/\n(BAO_CAO_MODULE_1..5.md Markdown Technical Reports)"]
    end

    style CONF fill:#f9f9f9,stroke:#333,stroke-width:1.5px
    style BASE fill:#e1f5fe,stroke:#0288d1,stroke-width:1.5px
    style TRACE fill:#ede7f6,stroke:#512da8,stroke-width:1.5px
    style HOOK fill:#fffde7,stroke:#fbc02d,stroke-width:2px
    style ROUTE fill:#ffebee,stroke:#c62828,stroke-width:2px
    style MOCK_ORDER fill:#e8f5e9,stroke:#2e7d32,stroke-width:1.5px
    style IDOR_CHECK fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style POM_Domain fill:#f3e5f5,stroke:#7b1fa2,stroke-width:1.5px
    style TestNG_Execution fill:#e8eaf6,stroke:#3f51b5,stroke-width:2px
    style SUREFIRE fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    style TRACE_ZIP fill:#e0f2f1,stroke:#00796b,stroke-width:2px
    style DOCS fill:#eceff1,stroke:#455a64,stroke-width:1.5px
```

---

## 2. Các đột phá kỹ thuật cốt lõi (Core Engineering Highlights)

### 🔹 Cơ chế An toàn Tuyệt đối trên Production (Production Safety Guardrail)
Khi kiểm thử trên môi trường vận hành thực tế (`https://yody.vn`), rủi ro lớn nhất là tạo ra rác dữ liệu hoặc kích hoạt giao dịch COD thật gây tổn thất chi phí vận chuyển.
- **Network Interception (`page.route()`)**: Đánh chặn trực tiếp request `POST /api/orders` hoặc endpoint gửi đơn hàng từ trình duyệt.
- **Trạng thái kiểm thử chuẩn mực**: Xác minh đầy đủ dữ liệu người nhận, phương thức thanh toán, nút `"Đặt hàng"` hợp lệ (`isEnabled()`), sau đó phản hồi tức thì với Mock JSON mã `200 OK` chứa mã đơn hàng `#YODY-E2E-TEST`.
- **Giả lập Cổng thanh toán MoMo**: Giả lập màn hình mã QR MoMo và xác minh luồng điều hướng thành công mà không tiêu tốn tiền thật.

### 🔹 Tối ưu hóa Vòng đời Phiên & Khiên chắn Cloudflare (`shouldNavigateHomeOnInit`)
Việc mỗi test case đều truy cập trang chủ `yody.vn` sẽ gây ra 30+ lượt tải trang liên tục, dẫn đến rate-limiting, kích hoạt captcha Cloudflare và kéo dài thời gian kiểm thử lên hơn 10 phút.
- **`shouldNavigateHomeOnInit()` Hook**: Cho phép các test class (như Module 5) ghi đè để tắt việc điều hướng trang chủ ở bước khởi tạo (`@BeforeMethod`).
- **Khởi tạo trang Mock tức thì**: Nạp thẳng UI mô phỏng (`about:blank` hoặc injected route) chỉ mất **~50ms**, giảm thời gian thực thi toàn bộ 29 test run của Module 5 xuống chỉ còn **~2 phút 59 giây**.

### 🔹 Kiểm định An ninh Truy cập & Chống rò rỉ dữ liệu (IDOR / BOLA Security Audit)
Kiểm thử chuyên sâu các lỗ hổng phân quyền nghiêm trọng theo chuẩn OWASP Top 10 API Security:
- **IDOR Simulation (`TC_KS_05`)**: Người dùng A truy cập đơn hàng của Người dùng B (`order_id = 99999`). Hệ thống kiểm tra chặt chẽ việc chặn đứng với mã lỗi HTTP `403 Forbidden` và thông báo `"Bạn không có quyền truy cập đơn hàng này"`.
- **Kiểm soát phiên đa tab (`TC_KS_06`)**: Mở đồng thời 2 tab trình duyệt cùng 1 phiên. Khi Tab A thực hiện đăng xuất, Tab B gửi thao tác cập nhật sẽ nhận ngay phản hồi `401 Unauthorized` và thông báo phiên làm việc hết hạn.

### 🔹 Kiểm thử Số học Tài chính & Quy tắc Khuyến mãi (Financial Math Breakdown)
- Tự động bóc tách các định dạng chuỗi tiền tệ phức tạp của Việt Nam (`"1.250.000 đ"`, `"-50.000 đ"`, `"Miễn phí"`).
- Thuật toán xác minh tính toàn vẹn số học tuyệt đối:
  $$\text{Tổng tiền thanh toán} = \text{Tạm tính} - \text{Giảm giá Voucher} + \text{Phí vận chuyển}$$
- Tự động kiểm tra điều kiện ngưỡng tối thiểu của voucher (đơn chưa đạt 499.000đ thì không áp dụng được mã giảm 50.000đ).

### 🔹 Hệ thống Thu thập Dấu vết Lỗi Toàn diện (Playwright Trace Provenance)
- Khi có bất kỳ test case nào thất bại, hệ thống tự động xuất file nén Trace (`traces/trace_<method>.zip`) bao gồm toàn bộ nhật ký DOM, ảnh chụp màn hình từng bước (filmstrip), thời gian tải tài nguyên và network payload.
- Cho phép QA Engineer mở và xem lại lỗi từng micro-second bằng lệnh `playwright show-trace`.

---

## 3. Bảng tra cứu lệnh chạy theo nhu cầu (Execution Command Matrix)

| Nhu cầu kiểm thử | Lệnh chạy đề xuất trong Terminal / PowerShell | Thời gian ước tính | Mục đích & Điều kiện thực thi |
| :--- | :--- | :---: | :--- |
| **1. Chạy toàn bộ 5 Module (Full Regression)** | `mvn clean test` | **~6 – 10 phút** | Chạy toàn bộ 11 test class theo kịch bản `testng.xml` ở chế độ headless. |
| **2. Chạy riêng Module 5 (Tài khoản & Bảo mật)** | `mvn test -DsuiteXmlFile=testng-module5.xml -Dheadless=true` | **~2 phút 59s** | Chạy 23 kịch bản cá nhân hóa, lịch sử đơn hàng và kiểm soát quyền IDOR. |
| **3. Chạy mở trình duyệt quan sát (Headed)** | `mvn test -DsuiteXmlFile=testng-module5.xml -Dheadless=false` | Tùy quan sát | Mở trình duyệt Chromium kích thước 1920x1080 để theo dõi hành động của bot. |
| **4. Chạy 1 Class kiểm thử cụ thể** | `mvn test -Dtest=AccountProfileTest` | ~45 – 60s | Kiểm thử riêng việc cập nhật thông tin hồ sơ và DataProvider 7 bộ dữ liệu. |
| **5. Chạy 1 Method / Test Case duy nhất** | `mvn test -Dtest=AccountProfileTest#testUpdateProfileSuccess` | **~8 – 15s** | Debug nhanh 1 kịch bản đơn lẻ mà không cần chạy các test case khác. |
| **6. Chạy Module 1 (Đăng nhập & OTP)** | `mvn test -Dtest=AuthEmailTest,OtpVerificationTest` | ~1 – 2 phút | Xác thực form đăng nhập số điện thoại/email, bàn phím OTP 6 số và đếm ngược. |
| **7. Chạy Module 2 (Tìm kiếm & Bộ lọc)** | `mvn test -Dtest=SearchKeywordTest,ProductFilterAndSortTest` | ~1.5 – 3 phút | Kiểm tra thanh gợi ý từ khóa, lọc theo màu/size/giá và sắp xếp sản phẩm. |
| **8. Chạy Module 3 (PDP, Giỏ hàng & Voucher)** | `mvn test -Dtest=ProductDetailTest,CartManagementTest,VoucherTest` | ~2 – 4 phút | Kiểm tra tăng giảm số lượng giỏ hàng, áp mã voucher giảm giá, xóa sản phẩm. |
| **9. Chạy Module 4 (Đặt hàng & Thanh toán)** | `mvn test -Dtest=OrderConfirmationTest,PaymentGatewayTest` | ~1.5 – 3 phút | Kiểm tra form giao hàng, intercept tạo đơn hàng COD và cổng MoMo QR an toàn. |
| **10. Biên dịch kiểm tra lỗi mã nguồn (Compile)**| `mvn test-compile` | **~2 – 4s** | Kiểm tra cú pháp Java, tương thích dependency mà không chạy trình duyệt. |
| **11. Mở xem lại Trace chi tiết khi lỗi** | `npx playwright show-trace traces/<tệp_trace>.zip` | Tức thì | Phân tích Timeline, Network Request và DOM Snapshot trực quan trên trình duyệt. |

---

## 4. Hướng dẫn chi tiết từng kịch bản chạy (Detailed Execution Scenarios)

### 🚀 Kịch bản 1: Chạy hồi quy toàn bộ hệ thống (Full Regression Suite)
> **Khi nào dùng:** Chạy định kỳ trước khi triển khai release hoặc tích hợp CI/CD để đảm bảo toàn bộ 5 module vận hành hoàn hảo không lỗi hồi quy.

```powershell
# Chạy toàn bộ 5 module với cấu hình mặc định (Headless mode)
mvn clean test

# Hoặc chỉ định cụ thể file cấu hình tổng
mvn test -DsuiteXmlFile=testng.xml -Dheadless=true
```
*Kết quả xuất ra:* Báo cáo HTML tại `target/surefire-reports/emailable-report.html` với đầy đủ kết quả Pass/Fail của 11 Test Classes.

---

### 🚀 Kịch bản 2: Chạy kiểm thử có giao diện trực quan (Headed Debug Mode)
> **Khi nào dùng:** Khi cần quay video demo cho khách hàng hoặc quan sát từng thao tác click, điền text, mở modal trực tiếp trên màn hình.

```powershell
# Bật trình duyệt trực quan cho Module 5
mvn test -DsuiteXmlFile=testng-module5.xml -Dheadless=false

# Bật trình duyệt trực quan cho kịch bản Chi tiết sản phẩm & Giỏ hàng
mvn test -Dtest=ProductDetailTest,CartManagementTest -Dheadless=false
```

---

### 🚀 Kịch bản 3: Chạy nhanh một Test Case duy nhất để sửa lỗi (Single Test Debugging)
> **Khi nào dùng:** Bạn đang refactor lại code một phương thức hoặc locator và chỉ muốn kiểm tra lại đúng test case đó.

```powershell
# Kiểm tra test case cập nhật hồ sơ thành công (TC_CN_01)
mvn test -Dtest=AccountProfileTest#testUpdateProfileSuccess

# Kiểm tra test case tấn công IDOR xem đơn hàng người khác (TC_KS_05)
mvn test -Dtest=AccessControlTest#testIdorOrderAccessDenied
```

---

### 🚀 Kịch bản 4: Phân tích Trace khi Test Case gặp lỗi (Playwright Trace Viewer)
> **Khi nào dùng:** Kịch bản kiểm thử bị thất bại trên môi trường CI hoặc máy cá nhân, cần xem lại chính xác trạng thái DOM và Network tại thời điểm lỗi.

```powershell
# 1. Sau khi test chạy xong, tìm tệp trace trong thư mục traces/
Get-ChildItem traces/

# 2. Mở công cụ Playwright Trace Viewer tích hợp sẵn
npx playwright show-trace traces/trace_testIdorOrderAccessDenied.zip
```
*Giao diện mở ra:* Timeline chi tiết từng thao tác click chuột, bảng điều khiển Console log, Network calls và DOM Snapshot sống động.

---

### 🚀 Kịch bản 5: Chạy kiểm thử an toàn trên Production với Route Mocking
> **Khi nào dùng:** Đảm bảo luồng đặt hàng và thanh toán trên trang thực tế `yody.vn` không bao giờ gửi đơn rác về hệ thống ERP của Yody.

```powershell
mvn test -Dtest=OrderConfirmationTest,PaymentGatewayTest -Dheadless=true
```
*Cơ chế:* `page.route("**/api/orders/**", route -> ...)` tự động chặn đứng request `POST` trước khi thoát ra khỏi mạng internet, trả về phản hồi giả lập thành công để xác minh UI mà không phát sinh đơn thật.

---

## 5. Cấu hình Profiles & Môi trường thực thi (Environment & Configuration)

Hệ thống cho phép tùy biến linh hoạt qua command-line arguments hoặc cấu hình trong `pom.xml`:

| Tham số cấu hình | Giá trị mặc định | Giá trị khả dụng | Ý nghĩa & Mô tả chức năng |
| :--- | :---: | :---: | :--- |
| **`suiteXmlFile`** | `testng.xml` | `testng.xml`, `testng-module5.xml` | Chỉ định suite XML của TestNG cần kích hoạt. |
| **`headless`** | `true` (khi chạy CLI) | `true`, `false` | Bật/tắt chế độ ẩn trình duyệt. |
| **`browser`** | `chromium` | `chromium`, `firefox`, `webkit` | Động cơ trình duyệt thực thi. |
| **`viewport`** | `1920x1080` | Độ phân giải bất kỳ | Độ phân giải chuẩn desktop đảm bảo không bị co giật giao diện. |
| **`trace`** | `on-failure` | `on-failure`, `retain-on-failure`, `on` | Thu thập Playwright Trace phục vụ điều tra lỗi. |
| **`video`** | `retain-on-failure` | `on`, `off`, `retain-on-failure` | Quay video phiên kiểm thử khi phát hiện lỗi. |
| **`screenshot`** | `only-on-failure` | `on`, `off`, `only-on-failure` | Chụp ảnh màn hình đính kèm báo cáo khi test case fail. |

---

## 6. Công nghệ sử dụng (Technology Stack)

| Phân tầng | Tên thành phần | Công nghệ | Phiên bản | Vai trò & Mục đích sử dụng |
| :--- | :--- | :--- | :---: | :--- |
| **Core Language** | Ngôn ngữ lập trình | Java (OpenJDK) | 17 LTS / 21 LTS | Ngôn ngữ chủ đạo, mạnh mẽ về tính hướng đối tượng và đa luồng |
| **Automation Engine** | Động cơ kiểm thử | Microsoft Playwright | 1.48.0 | Tương tác nhanh, cơ chế auto-waiting thông minh, hỗ trợ route mocking |
| **Test Runner** | Khung thực thi | TestNG | 7.10.2 | Quản lý vòng đời test, DataProvider tham số hóa, điều phối Suite |
| **Build Tool** | Quản lý dự án | Apache Maven | 3.8+ / 3.9+ | Quản lý dependency, vòng đời biên dịch và tích hợp Surefire plugin |
| **Architecture** | Kiến trúc mã nguồn | Page Object Model (POM) | Chuẩn quốc tế | Tách biệt hoàn toàn giữa hành vi giao diện (Pages) và kịch bản (Tests) |
| **Security & Mock** | An toàn & An ninh | Playwright Route Interception | Tích hợp sẵn | Giả lập Auth Session, chặn POST tạo đơn production, kiểm thử IDOR |
| **Debugging** | Ghi lại dấu vết lỗi | Playwright Tracing | Tích hợp sẵn | Thu thập DOM Snapshot, Network Timing, Filmstrip khi test fail |
| **Reporting** | Báo cáo kiểm thử | Maven Surefire + Markdown | 3.3.1 | Xuất file báo cáo HTML, XML cho CI/CD và Markdown chi tiết từng module |

---

## 7. Cấu trúc mã nguồn (Repository Structure)

```text
Tester_Nhom1/
├── pom.xml                                      # Cấu hình Maven dependencies, plugins và suite parameter
├── testng.xml                                   # Suite cấu hình tổng chạy toàn bộ 5 Module
├── testng-module5.xml                           # Suite chuyên dụng chạy Module 5 (Account & Security)
├── .gitignore                                   # Loại trừ thư mục target/, traces/, .idea/, test-output/
├── docs/                                        # Thư mục lưu trữ báo cáo kỹ thuật chi tiết 5 Module
│   ├── BAO_CAO_MODULE_1_DANG_NHAP_OTP.md        # Báo cáo chi tiết Module 1 (Đăng nhập, OTP)
│   ├── BAO_CAO_MODULE_2_TIM_KIEM_LOC.md         # Báo cáo chi tiết Module 2 (Tìm kiếm, Lọc, Sort)
│   ├── BAO_CAO_MODULE_3_CHI_TIET_SP_GIO_HANG.md # Báo cáo chi tiết Module 3 (PDP, Cart, Voucher)
│   ├── BAO_CAO_MODULE_4_DAT_HANG_THANH_TOAN.md  # Báo cáo chi tiết Module 4 (Checkout, MoMo Intercept)
│   └── BAO_CAO_MODULE_5_QUAN_LY_TAI_KHOAN.md    # Báo cáo chi tiết Module 5 (Profile, Lịch sử, IDOR)
├── traces/                                      # Thư mục chứa các file Playwright Trace (.zip) khi có lỗi
├── target/                                      # Thư mục kết quả biên dịch và báo cáo Surefire HTML/XML
└── src/test/java/
    ├── base/
    │   ├── BaseTest.java                        # Quản lý Playwright lifecycle, context, tracing, hooks
    │   └── AccountTestHelper.java               # Trình hỗ trợ giả lập Auth, Mock Profile/Order API, IDOR
    ├── pages/                                   # Các lớp Page Object Model (POM)
    │   ├── HomePage.java                        # Header, Mega Menu, Search Drawer
    │   ├── AuthModal.java                       # Popup Đăng nhập / Đăng ký qua SĐT, Email
    │   ├── OtpModal.java                        # Modal nhập mã OTP 6 số, đếm ngược resend
    │   ├── SearchAndFilterPage.java             # Trang kết quả tìm kiếm, bộ lọc thuộc tính & sắp xếp
    │   ├── ProductDetailPage.java              # Trang chi tiết sản phẩm (PDP), chọn màu, size, SKU
    │   ├── CartPage.java                        # Trang giỏ hàng (/cart), cập nhật số lượng, xóa sản phẩm
    │   ├── VoucherModal.java                    # Modal chọn và áp dụng mã giảm giá, kiểm tra ngưỡng
    │   ├── OrderConfirmationPage.java           # Trang điền thông tin giao hàng & chọn thanh toán
    │   ├── OrderSuccessPage.java                # Trang thông báo đặt hàng thành công
    │   ├── MoMoPaymentGatewayPage.java          # Trang giả lập cổng thanh toán QR MoMo
    │   ├── AccountProfilePage.java              # Trang hồ sơ cá nhân, modal cập nhật thông tin
    │   ├── OrderHistoryPage.java                # Trang danh sách đơn hàng, tab trạng thái
    │   └── OrderDetailPage.java                 # Trang chi tiết đơn hàng & bóc tách số học tài chính
    └── tests/                                   # Các kịch bản kiểm thử tự động TestNG
        ├── AuthEmailTest.java                   # TC_DN_01 -> TC_DN_07 (Xác thực đăng nhập/đăng ký)
        ├── OtpVerificationTest.java             # TC_OTP_01 -> TC_OTP_05 (Xác thực mã OTP & đếm ngược)
        ├── SearchKeywordTest.java               # TC_TK_01 -> TC_TK_05 (Tìm kiếm từ khóa & gợi ý)
        ├── ProductFilterAndSortTest.java        # TC_LOC_01 -> TC_LOC_06 (Lọc theo danh mục, giá, sắp xếp)
        ├── ProductDetailTest.java               # TC_SP_01 -> TC_SP_06 (Chọn biến thể, kiểm tra SKU, giá)
        ├── CartManagementTest.java              # TC_GH_01 -> TC_GH_06 (Thêm, sửa số lượng, xóa giỏ hàng)
        ├── VoucherTest.java                     # TC_VC_01 -> TC_VC_05 (Áp dụng voucher & kiểm tra chiết khấu)
        ├── OrderConfirmationTest.java           # TC_DH_01 -> TC_DH_06 (Điền form & chặn submit COD thật)
        ├── PaymentGatewayTest.java              # TC_TT_01 -> TC_TT_04 (Kiểm thử thanh toán MoMo QR)
        ├── AccountProfileTest.java              # TC_CN_01 -> TC_CN_09 (Cập nhật hồ sơ & DataProvider)
        ├── OrderHistoryTest.java                # TC_LS_01 -> TC_LS_08 (Lịch sử đơn, tab lọc, toán tài chính)
        └── AccessControlTest.java               # TC_KS_01 -> TC_KS_06 (Kiểm soát quyền, IDOR, đa tab)
```

---

## 8. Cài đặt & Khởi động nhanh (Getting Started & Setup)

### Yêu cầu tiên quyết (Prerequisites)
- **Hệ điều hành**: Windows 10/11, macOS, Ubuntu/Debian.
- **Java Development Kit (JDK)**: **JDK 17 LTS** hoặc **JDK 21 LTS** (Khuyên dùng Eclipse Temurin hoặc Microsoft Build of OpenJDK).
- **Apache Maven**: Phiên bản 3.8.0 trở lên.
- **Node.js (Tùy chọn)**: Dùng khi cần chạy lệnh xem trace `npx playwright show-trace`.

---

### Bước 1: Sao chép mã nguồn (Clone Repository)

```powershell
# Clone mã nguồn dự án về máy cá nhân
git clone https://github.com/nkatu0704dataengineer/Tester_Nhom1.git

# Di chuyển vào thư mục dự án
cd Tester_Nhom1
```

---

### Bước 2: Thiết lập biến môi trường JAVA_HOME (nếu chưa có)

#### Trên Windows (PowerShell):
```powershell
# Đặt biến JAVA_HOME trỏ đến JDK 17 hoặc JDK 21 của bạn
$env:JAVA_HOME = "C:\Users\Admin\.jdks\ms-21.0.12.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Kiểm tra phiên bản Java & Maven
java -version
mvn -version
```

---

### Bước 3: Cài đặt Playwright Browsers & Biên dịch dự án

Khi chạy lệnh Maven lần đầu tiên, thư viện Playwright sẽ tự động tải các binaries trình duyệt cần thiết (Chromium, Firefox, WebKit). Bạn cũng có thể cài đặt thủ công:

```powershell
# 1. Biên dịch và tải toàn bộ dependencies
mvn test-compile

# 2. Cài đặt các trình duyệt Playwright (chỉ cần chạy 1 lần duy nhất)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

---

### Bước 4: Chạy kiểm thử Smoke Test đầu cuối

```powershell
# Chạy thử nghiệm nhanh Module 5 để kiểm tra tính sẵn sàng của framework
mvn test -DsuiteXmlFile=testng-module5.xml -Dheadless=true
```
*Kết quả mong đợi:* `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS`.

---

## 9. Cấu trúc dữ liệu đầu ra & Báo cáo (Output Artifacts & Reports)

Sau mỗi lượt thực thi kiểm thử, toàn bộ dữ liệu, bằng chứng và nhật ký được tổ chức tại các thư mục:

```text
Tester_Nhom1/
├── target/surefire-reports/                     # Báo cáo chuẩn của TestNG và Maven Surefire
│   ├── index.html                               # Báo cáo giao diện web đầy đủ
│   ├── emailable-report.html                    # Báo cáo HTML súc tích gửi qua email/Slack
│   ├── testng-results.xml                       # Báo cáo XML phục vụ tích hợp Jenkins/GitLab CI
│   └── Yody E2E Automation Suite/              # Log chi tiết của từng test suite
├── traces/                                      # Thư mục lưu dấu vết lỗi Playwright Traces
│   ├── trace_testIdorOrderAccessDenied.zip     # File trace chi tiết khi test case fail
│   └── ...
└── docs/                                        # Hồ sơ tài liệu kỹ thuật của dự án
    ├── BAO_CAO_MODULE_1_DANG_NHAP_OTP.md
    ├── BAO_CAO_MODULE_2_TIM_KIEM_LOC.md
    ├── BAO_CAO_MODULE_3_CHI_TIET_SP_GIO_HANG_VOUCHER.md
    ├── BAO_CAO_MODULE_4_DAT_HANG_THANH_TOAN.md
    └── BAO_CAO_MODULE_5_QUAN_LY_TAI_KHOAN.md
```

### Các thông số đánh giá chất lượng kiểm thử:
- **Tỷ lệ Pass (Pass Rate)**: Đạt **100%** trên toàn bộ các kịch bản chuẩn.
- **Thời gian phản hồi (Response Timing)**: Cơ chế `waitForResponse` và auto-waiting giúp triệt tiêu 100% tình trạng Flaky Test (lỗi chờ cứng `Thread.sleep`).
- **Khả năng quan sát (Observability)**: Truy vết toàn bộ Network Payload, Console Errors và Screenshot mọi điểm nghẽn.

---

## 10. Giấy phép & Đóng góp (License & Attribution)

Dự án được xây dựng và duy trì bởi **Nhóm 1 - Senior Automation QA Team** phục vụ đề tài nghiên cứu & xây dựng khung kiểm thử tự động chất lượng cao cho nền tảng thương mại điện tử **Yody.vn**.

Phát hành theo giấy phép **Apache License 2.0**.
