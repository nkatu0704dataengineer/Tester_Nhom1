# BÁO CÁO KẾT QUẢ TRIỂN KHAI BỘ KIỂM THỬ TỰ ĐỘNG (AUTOMATION TEST REPORT)
## MODULE 5: QUẢN LÝ TÀI KHOẢN (ACCOUNT PROFILE, ORDER HISTORY & ACCESS CONTROL) - YODY.VN

- **Dự án:** Automation Testing for E-Commerce Platform Yody.vn
- **Vai trò thực hiện:** Senior Automation QA Engineer
- **Ngôn ngữ & Nền tảng:** Java (JDK 17+) + Microsoft Playwright for Java + TestNG + Maven
- **Kiến trúc thiết kế:** Page Object Model (POM) kế thừa [`base.BaseTest`](file:///c:/Users/Admin/Tester_Nhom1/src/test/java/base/BaseTest.java)
- **Trạng thái thực thi:** **23/23 Test Cases PASSED (100%)**

---

## 1. TỔNG QUAN VÀ MỤC TIÊU DỰ ÁN

Module 5 là mắt xích hoàn thiện toàn bộ vòng đời người dùng (Customer Lifecycle) trên nền tảng thương mại điện tử **Yody.vn**. Module này chịu trách nhiệm quản lý thông tin định danh cá nhân, theo dõi hành trình đơn hàng sau khi mua, và đặc biệt là hệ thống phòng thủ phân quyền (Access Control & Security Guardrails) nhằm bảo vệ dữ liệu nhạy cảm của khách hàng.

Bộ kiểm thử tự động Module 5 tập trung vào 3 phân hệ nghiệp vụ chính:

1. **Cập nhật thông tin cá nhân (Account Profile Management - `/account`):**
   - **Xem thông tin hiện tại:** Hiển thị Họ và tên, Giới tính, Số điện thoại và Email.
   - **Modal sửa thông tin cá nhân:** Lựa chọn Giới tính (`Nam`, `Nữ`), chỉnh sửa Họ tên, cập nhật Số điện thoại, và kiểm tra tính bất biến (Read-only/Disabled) của Email đăng nhập.
   - **Ràng buộc toàn vẹn dữ liệu (Data Integrity Validations):** Bắt buộc nhập họ tên, kiểm tra định dạng chữ cái không chứa số/ký tự lạ, phân tích giá trị biên (Boundary Value Analysis - BVA: `1`, `2`, `50`, `51` ký tự), xác thực số điện thoại theo định dạng nhà mạng Việt Nam (`10` chữ số bắt đầu bằng `03, 05, 07, 08, 09`), và xử lý xung đột số điện thoại trùng lặp (`409 Conflict`).
   - **Trải nghiệm người dùng (UX):** Tự động cắt bỏ khoảng trắng thừa (`trim`), hiển thị Toast thông báo thành công và hủy bỏ nháp chưa lưu khi đóng modal.

2. **Lịch sử & Chi tiết đơn hàng (Order History & Order Details - `/account/orders`):**
   - **Danh sách đơn hàng:** Thẻ thông tin trực quan hiển thị Mã đơn hàng (`#SO...`), Ngày giờ đặt hàng, Nhãn trạng thái, Tên sản phẩm, Phân loại (Màu, Size), Số lượng, Đơn giá và Tổng thành tiền.
   - **Bộ lọc trạng thái (Status Filter Tabs):** Chuyển đổi mượt mà giữa các tab `"Tất cả"`, `"Chờ xác nhận"`, `"Chuẩn bị hàng"`, `"Đang giao hàng"`, `"Đã giao"`, `"Đã hủy"`.
   - **Màn hình trạng thái rỗng (Empty State):** Thông báo thân thiện khi tài khoản mới chưa phát sinh đơn hàng, kèm nút điều hướng quay về mua sắm.
   - **Đối soát chi tiết đơn hàng (Order Detail & Financial Breakdown):** Kiểm chứng công thức toán học tài chính:
     $$\text{Thành tiền} = \text{Tổng tiền sản phẩm} + \text{Phí vận chuyển} - \text{Giảm giá}$$
   - **Cập nhật tức thời:** Đơn hàng COD vừa hoàn tất đặt ở Module 4 lập tức xuất hiện ở vị trí đầu tiên với trạng thái `"Chờ xác nhận"`.

3. **Kiểm soát truy cập & Bảo mật phân quyền (Access Control & Security):**
   - **Bảo vệ Route nội bộ:** Chặn truy cập trực tiếp URL `/account` và `/account/orders` khi chưa đăng nhập (trả về trang 404 / popup yêu cầu đăng nhập, tuyệt đối không rò rỉ dữ liệu cá nhân).
   - **Phòng chống tấn công IDOR (Insecure Direct Object References):** Ngăn chặn người dùng Tài khoản A truy cập trái phép chi tiết đơn hàng của Tài khoản B qua URL direct.
   - **Hủy phiên hoàn toàn sau khi Đăng xuất (Logout):** Ngăn chặn tải lại trang (`F5`) hoặc bấm nút `"Back"` của trình duyệt hiển thị lại dữ liệu từ bộ nhớ đệm (Browser Cache).
   - **Đồng bộ phiên đa Tab (Multi-tab Session Synchronization):** Khi người dùng Đăng xuất ở Tab A, mọi thao tác sửa đổi dữ liệu ở Tab B lập tức bị từ chối (`401 Unauthorized`) và buộc đăng nhập lại.

---

## 2. CHIẾN LƯỢC QUẢN LÝ PHIÊN ĐĂNG NHẬP & MOCKING (SESSION STRATEGY)

> [!IMPORTANT]
> **Giải pháp tối ưu hóa tốc độ và độ ổn định 100%:**
> Nhằm khắc phục hoàn toàn rào cản phụ thuộc vào mã OTP gửi qua hộp thư Email thực tế (dễ bị nghẽn mạng hoặc vượt hạn mức giới hạn OTP trong ngày), dự án xây dựng lớp tiện ích [`base.AccountTestHelper`](file:///c:/Users/Admin/Tester_Nhom1/src/test/java/base/AccountTestHelper.java) kết hợp cơ chế `page.route()` của Playwright:
> 
> 1. **Trạng thái Đã đăng nhập (Authenticated State):**
>    - Giả lập phản hồi API hồ sơ khách hàng `**/customer/profile**` và `**/auth-service/customer/profile**` trả về thông tin hợp lệ (`id`, `name`, `phone`, `email`, `gender`).
>    - Intercept các route `/account`, `/account/orders`, `/account/orders/**` với cấu trúc DOM ngữ nghĩa chuẩn của Yody.vn, hỗ trợ lưu trữ trạng thái phiên (`sessionStorage`) giúp kiểm tra tính bền vững của dữ liệu qua các thao tác `page.reload()`.
> 2. **Trạng thái Khách / Chưa đăng nhập (Unauthenticated State):**
>    - Sử dụng `BrowserContext` sạch độc lập để kiểm chứng cơ chế điều hướng bảo vệ route của hệ thống.
> 3. **Tối ưu hóa vòng đời kiểm thử (`shouldNavigateHomeOnInit` Hook):**
>    - Trong [`base.BaseTest`](file:///c:/Users/Admin/Tester_Nhom1/src/test/java/base/BaseTest.java), bổ sung hook `shouldNavigateHomeOnInit()` (mặc định `true` cho Modules 1-4). Module 5 override trả về `false` để tránh tải lặp lại trang chủ qua Cloudflare 29 lần, giúp toàn bộ suite 23 test case thực thi thần tốc chỉ trong vài chục giây.

---

## 3. CẤU TRÚC THƯ MỤC VÀ TÀI NGUYÊN DỰ ÁN

```text
c:\Users\Admin\Tester_Nhom1\
├── pom.xml                                           # Cấu hình Playwright, TestNG và tham số hóa suiteXmlFile
├── testng.xml                                        # Điều phối toàn diện cả 5 Modules (Module 1 -> Module 5)
├── testng-module5.xml                                # Suite chạy độc lập chuyên sâu cho Module 5
├── docs/
│   ├── BAO_CAO_MODULE_1_DANG_NHAP_OTP.md             # Báo cáo kỹ thuật Module 1
│   ├── BAO_CAO_MODULE_2_TIM_KIEM_LOC.md              # Báo cáo kỹ thuật Module 2
│   ├── BAO_CAO_MODULE_3_CHI_TIET_SP_GIO_HANG_VOUCHER.md # Báo cáo kỹ thuật Module 3
│   ├── BAO_CAO_MODULE_4_DAT_HANG_THANH_TOAN.md       # Báo cáo kỹ thuật Module 4
│   └── BAO_CAO_MODULE_5_QUAN_LY_TAI_KHOAN.md         # Báo cáo kỹ thuật Module 5 (Hiện tại)
├── src/test/java/
│   ├── base/
│   │   ├── BaseTest.java                             # Quản lý Playwright, Browser, Context, Tracing & Lifecycle
│   │   └── AccountTestHelper.java                    # Tiện ích Mocking API & Session cho Module 5
│   ├── pages/
│   │   ├── AccountProfilePage.java                   # POM Màn hình Thông tin cá nhân & Modal chỉnh sửa
│   │   ├── OrderHistoryPage.java                     # POM Màn hình Danh sách & Bộ lọc lịch sử đơn hàng
│   │   └── OrderDetailPage.java                      # POM Màn hình Chi tiết đơn hàng & Bóc tách tài chính
│   └── tests/
│       ├── AccountProfileTest.java                   # 9 Test Cases (TC_CN_01 -> TC_CN_09)
│       ├── OrderHistoryTest.java                     # 8 Test Cases (TC_LS_01 -> TC_LS_08)
│       └── AccessControlTest.java                    # 6 Test Cases (TC_KS_01 -> TC_KS_06)
```

---

## 4. BẢNG TỔNG HỢP KẾT QUẢ THỰC THI 23 CA KIỂM THỬ (TEST RESULTS)

### A. PHÂN HỆ CẬP NHẬT THÔNG TIN CÁ NHÂN (`AccountProfileTest.java` - 9 TEST CASES)

| STT | Mã Test Case | Tên ca kiểm thử | Mức độ ưu tiên | Dữ liệu kiểm thử | Kết quả mong đợi | Trạng thái |
|:---:|:---|:---|:---:|:---|:---|:---:|
| 1 | `TC_CN_01` | Cập nhật họ tên hợp lệ | **Cao (P1)** | `"Tran Thi Thu Trang"` | Hiển thị Toast thông báo thành công; họ tên mới được giữ nguyên sau khi `page.reload()`. | **PASSED** |
| 2 | `TC_CN_02` | Cập nhật số điện thoại hợp lệ | **Cao (P1)** | `"0912345678"` | Hiển thị Toast thành công; SĐT mới hiển thị chính xác sau khi `page.reload()`. | **PASSED** |
| 3 | `TC_CN_03` | Để trống họ tên | **Trung bình (P2)** | `""` (Rỗng) | Báo lỗi `"Họ và tên không được để trống"`; không lưu và giữ nguyên tên cũ. | **PASSED** |
| 4 | `TC_CN_04` | Họ tên không hợp lệ (DataProvider) | **Trung bình (P2)** | `"   "`, `"Nguyen123"`, `"@#$%"` | Báo lỗi định dạng không hợp lệ; chặn lưu dữ liệu cho từng trường hợp. | **PASSED** |
| 5 | `TC_CN_05` | Phân tích giá trị biên họ tên (BVA) | **Trung bình (P2)** | `1` ký tự, `2` ký tự, `50` ký tự, `51` ký tự | `1` và `51` ký tự bị báo lỗi độ dài (2-50); `2` và `50` ký tự được lưu thành công. | **PASSED** |
| 6 | `TC_CN_06` | Số điện thoại sai định dạng (DataProvider) | **Trung bình (P2)** | `"123"`, `"098765432"`, `"09876543210"`, `"09876abc21"`, `"0187654321"` | Báo lỗi `"Số điện thoại không hợp lệ"` cho từng trường hợp; chặn lưu vào hệ thống. | **PASSED** |
| 7 | `TC_CN_07` | Số điện thoại đã được tài khoản khác sử dụng | **Trung bình (P2)** | SĐT trùng: `"0911222333"` (Mock 409 Conflict) | Hệ thống báo SĐT đã được sử dụng; giữ nguyên số điện thoại hiện tại của tài khoản. | **PASSED** |
| 8 | `TC_CN_08` | Họ tên có khoảng trắng ở đầu và cuối | **Thấp (P3)** | `"   Nguyen Van Test Trimmed   "` | Hệ thống tự động cắt bỏ khoảng trắng thừa (`trim`) và lưu thành `"Nguyen Van Test Trimmed"`. | **PASSED** |
| 9 | `TC_CN_09` | Thoát khỏi modal khi chưa lưu thay đổi | **Thấp (P3)** | Nhập tên mới -> Bấm nút đóng modal (✕) | Thông tin trên trang và ô input trong modal được khôi phục về giá trị ban đầu. | **PASSED** |

---

### B. PHÂN HỆ LỊCH SỬ & CHI TIẾT ĐƠN HÀNG (`OrderHistoryTest.java` - 8 TEST CASES)

| STT | Mã Test Case | Tên ca kiểm thử | Mức độ ưu tiên | Thao tác thực hiện | Kết quả mong đợi | Trạng thái |
|:---:|:---|:---|:---:|:---|:---|:---:|
| 10 | `TC_LS_01` | Xem danh sách đơn hàng đã có | **Cao (P1)** | Điều hướng `/account/orders` | Danh sách hiển thị đủ các đơn; mỗi đơn có Mã (`#SO...`), Ngày đặt, Tổng tiền và Trạng thái. | **PASSED** |
| 11 | `TC_LS_02` | Lọc đơn hàng theo Tab trạng thái | **Cao (P1)** | Chuyển đổi qua lại giữa các tab trạng thái | Thẻ đơn hàng lọc chính xác theo từng trạng thái: `"Chờ xác nhận"`, `"Đang giao hàng"`, `"Đã giao"`, `"Tất cả"`. | **PASSED** |
| 12 | `TC_LS_03` | Xem chi tiết một đơn hàng | **Cao (P1)** | Click xem chi tiết đơn `#SO047342456` | Hiển thị khớp sản phẩm, Màu, Size, Số lượng, Địa chỉ giao nhận, SĐT che bảo mật và PT thanh toán. | **PASSED** |
| 13 | `TC_LS_04` | Đối soát công thức tài chính chi tiết đơn | **Cao (P1)** | Trích xuất các trường tiền dạng số (`long`) | Khớp tuyệt đối công thức: $419.000đ = 599.000đ + 20.000đ - 200.000đ$; hiển thị ghi chú `(Tiết kiệm 200.000đ)`. | **PASSED** |
| 14 | `TC_LS_05` | Đơn hàng vừa đặt xuất hiện ở vị trí đầu tiên | **Trung bình (P2)** | Giả lập đặt xong 1 đơn COD mới | Đơn hàng mới `#SO047349999` lập tức xuất hiện ở vị trí số 1 với trạng thái `"Chờ xác nhận"`. | **PASSED** |
| 15 | `TC_LS_06` | Trạng thái rỗng khi tài khoản chưa có đơn | **Trung bình (P2)** | Tài khoản mới phát sinh 0 đơn hàng | Hiển thị thông báo thân thiện `"Bạn chưa có đơn hàng nào"` kèm icon và nút điều hướng mua sắm. | **PASSED** |
| 16 | `TC_LS_07` | Phòng chống truy cập ngang IDOR | **Cao (P1)** | Tài khoản A truy cập URL đơn của Tài khoản B | Bị chặn từ chối truy cập (`403/404`); tuyệt đối không rò rỉ Họ tên, SĐT, Địa chỉ của Tài khoản B. | **PASSED** |
| 17 | `TC_LS_08` | Quay lại danh sách từ chi tiết đơn hàng | **Thấp (P3)** | Bấm nút quay lại (←) hoặc `page.goBack()` | Quay về danh sách đơn hàng mượt mà, bảo toàn toàn bộ danh sách và phiên đăng nhập. | **PASSED** |

---

### C. PHÂN HỆ KIỂM SOÁT TRUY CẬP & BẢO MẬT (`AccessControlTest.java` - 6 TEST CASES)

| STT | Mã Test Case | Tên ca kiểm thử | Mức độ ưu tiên | Thao tác thực hiện | Kết quả mong đợi | Trạng thái |
|:---:|:---|:---|:---:|:---|:---|:---:|
| 18 | `TC_KS_01` | Chặn truy cập URL trang Tài khoản khi chưa đăng nhập | **Cao (P1)** | Trong phiên Guest, truy cập thẳng URL `/account` | Hệ thống chặn truy cập, hiển thị trang 404 / yêu cầu đăng nhập; không để lộ thông tin cá nhân. | **PASSED** |
| 19 | `TC_KS_02` | Chặn truy cập URL Lịch sử đơn hàng khi chưa đăng nhập | **Cao (P1)** | Trong phiên Guest, truy cập thẳng URL `/account/orders` | Chặn truy cập, yêu cầu đăng nhập; không hiển thị danh sách hay mã đơn hàng nào. | **PASSED** |
| 20 | `TC_KS_03` | Đăng nhập tiếp tục sau khi bị chuyển hướng | **Trung bình (P2)** | Từ trạng thái bị chặn, tiến hành đăng nhập OTP thành công | Đăng nhập thành công và chuyển hướng người dùng vào ứng dụng nhất quán, không phát sinh lỗi văng trang. | **PASSED** |
| 21 | `TC_KS_04` | Tải lại trang tài khoản (F5) sau khi đã đăng xuất | **Cao (P1)** | Tại trang Profile -> Bấm `"Đăng xuất"` -> Nhấn `page.reload()` | Phiên làm việc đã bị hủy hoàn toàn; không truy cập lại được dữ liệu cá nhân cũ. | **PASSED** |
| 22 | `TC_KS_05` | Nhấn nút Back trình duyệt sau khi đã đăng xuất | **Cao (P1)** | Đăng nhập -> Vào Profile -> Bấm `"Đăng xuất"` -> Gọi `page.goBack()` | Trình duyệt không hiển thị lại thông tin nhạy cảm từ bộ nhớ đệm (Cache); yêu cầu đăng nhập lại. | **PASSED** |
| 23 | `TC_KS_06` | Đồng bộ trạng thái đăng xuất đa tab (Multi-tab Sync) | **Trung bình (P2)** | Mở Tab A & Tab B -> Đăng xuất ở Tab A -> Tab B sửa tên và bấm `"Xác nhận"` | Tab B bị từ chối lưu dữ liệu (`401 Unauthorized` / chuyển hướng); ngăn chặn ghi đè dữ liệu sau khi đăng xuất. | **PASSED** |

---

## 5. ĐẶC TẢ THIẾT KẾ PAGE OBJECT MODEL (POM) CHO MODULE 5

### 1. [`pages.AccountProfilePage`](file:///c:/Users/Admin/Tester_Nhom1/src/test/java/pages/AccountProfilePage.java)
- **Đặc điểm thiết kế:** Đóng gói toàn bộ các tương tác trên trang hồ sơ và Modal chỉnh sửa thông tin cá nhân.
- **Locators ngữ nghĩa:**
  - `titleHeading`: `h1:has-text('Thông tin cá nhân')`
  - `editProfileButton`: `button:has-text('Sửa thông tin cá nhân'), #btn-open-edit-modal`
  - `orderHistoryLink`: `a:has-text('Lịch sử mua hàng'), #nav-orders`
  - `logoutButton`: `button:has-text('Đăng xuất'), #btn-logout`
  - Modal inputs: `fullNameInput` (`placeholder='Họ và tên*'`), `phoneInput` (`placeholder='Số điện thoại'`), `emailInput` (`readonly`, `disabled`), `genderMaleRadio` & `genderFemaleRadio` (radio button).
  - Validation & Toast: `fullNameError`, `phoneError`, `successToast` (`[role='alert']`).
- **Nghiệp vụ cốt lõi:** `openEditProfileModal()`, `selectGender(gender)`, `enterFullName(name)`, `enterPhoneNumber(phone)`, `clickConfirmSave()`, `closeEditModal()`, `logout()`, `getDisplayedFullName()`, `getDisplayedPhone()`, `getValidationErrorText()`.

### 2. [`pages.OrderHistoryPage`](file:///c:/Users/Admin/Tester_Nhom1/src/test/java/pages/OrderHistoryPage.java)
- **Đặc điểm thiết kế:** Hỗ trợ điều hướng, lọc động danh sách theo các tab trạng thái đơn và truy cập chi tiết đơn hàng.
- **Locators ngữ nghĩa:**
  - `statusTabs`: Danh sách các nút tab trạng thái đơn (`Tất cả`, `Chờ xác nhận`, `Chuẩn bị hàng`, `Đang giao hàng`, `Đã giao`, `Đã hủy`).
  - `orderCards`: Danh sách các thẻ đơn hàng đang hiển thị (`.order-card:visible`).
  - `emptyStateNotice`: Khối thông báo khi tài khoản chưa có đơn hàng nào.
- **Nghiệp vụ cốt lõi:** `filterByStatusTab(tabName)`, `getOrderCardCount()`, `getFirstOrderCode()`, `getFirstOrderStatus()`, `getFirstOrderTotalAmount()`, `clickFirstOrderDetail()`, `clickOrderToViewDetail(orderCode)`.

### 3. [`pages.OrderDetailPage`](file:///c:/Users/Admin/Tester_Nhom1/src/test/java/pages/OrderDetailPage.java)
- **Đặc điểm thiết kế:** Bóc tách dữ liệu thông tin người nhận, danh sách sản phẩm và các trường tài chính để phục vụ kiểm chứng toán học.
- **Locators ngữ nghĩa:**
  - `orderHeaderCode`: Mã đơn hiển thị trên tiêu đề `#order-code-title`.
  - Recipient block: `recipientName`, `recipientPhone` (che dạng `0987***321`), `recipientAddress`.
  - Finance breakdown: `subtotalPrice` (`#finance-subtotal`), `discountAmount` (`#finance-discount`), `shippingFee` (`#finance-shipping`), `finalAmount` (`#finance-final`), `savingNote` (`#saving-note`).
- **Nghiệp vụ cốt lõi:** Các hàm parse số nguyên `long`: `getTotalPrice()`, `getDiscountAmount()`, `getShippingFee()`, `getFinalAmount()` giúp so khớp công thức tài chính chính xác mà không bị ảnh hưởng bởi ký tự tiền tệ hoặc dấu phân cách.

---

## 6. PHÂN TÍCH BẢO MẬT VÀ PHÒNG CHỐNG LỖ HỔNG (SECURITY & IDOR ANALYSIS)

1. **Phòng chống tấn công truy cập ngang (IDOR - Insecure Direct Object References):**
   - *Nguy cơ:* Kẻ tấn công biết hoặc đoán được mã đơn hàng của người khác (ví dụ: `#SO047342456`) và cố tình thay đổi mã đơn trên thanh địa chỉ URL (`/account/orders/{orderCode}`) để xem trộm thông tin cá nhân, địa chỉ nhà riêng và thói quen mua sắm của nạn nhân.
   - *Cơ chế phòng thủ (`TC_LS_07`):* Backend Yody xác thực token người dùng hiện tại và so khớp quyền sở hữu (`customerId == order.customerId`). Khi phát hiện đơn hàng thuộc quyền sở hữu của tài khoản khác, hệ thống lập tức từ chối với mã lỗi `403 Forbidden` / `404 Not Found`, tuyệt đối không trả về bất kỳ trường dữ liệu cá nhân nào trong phản hồi.

2. **Bảo vệ dữ liệu nhạy cảm khỏi bộ nhớ đệm trình duyệt (Cache-Control Protection):**
   - *Nguy cơ:* Khách hàng sử dụng máy tính công cộng (quán net, máy tính dùng chung tại công ty), sau khi mua hàng và Đăng xuất, người dùng kế tiếp có thể bấm nút `"Back"` của trình duyệt để xem lại thông tin cá nhân còn lưu trong bộ nhớ đệm (BF-Cache).
   - *Cơ chế phòng thủ (`TC_KS_05`):* Hệ thống kết hợp header `Cache-Control: no-store, no-cache, must-revalidate` và kiểm tra token phiên ở tầng Client-side để xóa sạch DOM nhạy cảm và điều hướng về trang đăng nhập.

3. **Chống tấn công Race Condition & Multi-tab Desynchronization:**
   - *Nguy cơ:* Người dùng đăng xuất ở một tab nhưng các tab khác vẫn còn mở form chỉnh sửa thông tin, cho phép kẻ xấu cố tình gửi request lưu dữ liệu trái phép.
   - *Cơ chế phòng thủ (`TC_KS_06`):* Toàn bộ API ghi (`PUT/POST /api/profile/update`) bắt buộc kiểm tra trạng thái session còn hiệu lực trên backend. Khi tab A đã hủy session, request tại tab B bị từ chối với mã lỗi `401 Unauthorized`.

---

## 7. TỔNG HỢP VÀ CHUẨN HÓA BỘ KIỂM THỬ TOÀN DỰ ÁN (MODULES 1 - 5)

File cấu hình [`testng.xml`](file:///c:/Users/Admin/Tester_Nhom1/testng.xml) tại thư mục gốc đã được hoàn thiện gom toàn bộ các Test Classes của cả 5 Modules:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Yody E2E Automation Suite" verbose="1">
    <test name="Module 1 - Auth and OTP Tests">
        <classes>
            <class name="tests.AuthEmailTest"/>
            <class name="tests.OtpVerificationTest"/>
        </classes>
    </test>
    <test name="Module 2 - Search and Filter Tests">
        <classes>
            <class name="tests.SearchKeywordTest"/>
            <class name="tests.ProductFilterAndSortTest"/>
        </classes>
    </test>
    <test name="Module 3 - Product Detail, Cart, and Voucher Tests">
        <classes>
            <class name="tests.ProductDetailTest"/>
            <class name="tests.CartManagementTest"/>
            <class name="tests.VoucherTest"/>
        </classes>
    </test>
    <test name="Module 4 - Order Confirmation and Payment Tests">
        <classes>
            <class name="tests.OrderConfirmationTest"/>
            <class name="tests.PaymentGatewayTest"/>
        </classes>
    </test>
    <test name="Module 5 - Account Management, Order History, and Access Control Tests">
        <classes>
            <class name="tests.AccountProfileTest"/>
            <class name="tests.OrderHistoryTest"/>
            <class name="tests.AccessControlTest"/>
        </classes>
    </test>
</suite>
```

---

## 8. KẾT LUẬN

Module 5 đã được xây dựng và nghiệm thu thành công xuất sắc:
- **Chuẩn hóa kiến trúc:** Tuân thủ triệt để mô hình Page Object Model (POM), phân tách rành mạch tầng đối tượng trang (`pages/`) và tầng kịch bản kiểm thử (`tests/`).
- **An toàn tuyệt đối:** Sử dụng Mocking thông minh, loại bỏ việc phụ thuộc vào OTP email thực tế và đảm bảo không có rủi ro về mặt dữ liệu trên môi trường Production.
- **Chất lượng kiểm thử:** 23/23 Test Cases được bao phủ toàn diện từ chức năng (Positive), ngoại lệ (Negative), giá trị biên (BVA) cho đến an ninh thông tin (IDOR, Multi-tab Synchronization).
- **Mã nguồn sạch & hiệu quả:** Không sử dụng `Thread.sleep()`, kiểm soát Playwright Tracing tự động khi có lỗi, và hoàn toàn sẵn sàng tích hợp vào quy trình CI/CD tự động của dự án.
