# BÁO CÁO KẾT QUẢ TRIỂN KHAI BỘ KIỂM THỬ TỰ ĐỘNG (AUTOMATION TEST REPORT)
## MODULE 3: CHI TIẾT SẢN PHẨM (PDP), GIỎ HÀNG (CART) & VOUCHER KHUYẾN MÃI - YODY.VN

- **Dự án:** Automation Testing for E-Commerce Platform Yody.vn
- **Vai trò thực hiện:** Senior Automation QA Engineer
- **Ngôn ngữ & Nền tảng:** Java (JDK 17+) + Microsoft Playwright for Java + TestNG + Maven
- **Kiến trúc thiết kế:** Page Object Model (POM) kế thừa `base.BaseTest`
- **Trạng thái thực thi:** **30/30 Test Cases PASSED (100%)**

---

## 1. TỔNG QUAN VÀ MỤC TIÊU DỰ ÁN

Module 3 là mắt xích nghiệp vụ quan trọng nhất trong chuỗi chuyển đổi thương mại điện tử (E-Commerce Conversion Funnel) của **Yody.vn**, bao gồm:
1. **Trang Chi tiết sản phẩm (Product Detail Page - PDP):**
   - Trưng bày đầy đủ thông tin sản phẩm, mã SKU, giá bán, giá niêm yết và mức giảm giá.
   - Thư viện hình ảnh sản phẩm (ảnh chính và thumbnail).
   - Lựa chọn biến thể Màu sắc và Kích thước (Size), kiểm soát trạng thái khả dụng và vô hiệu hóa khi hết hàng.
   - Bảng hướng dẫn chọn size modal.
   - Bộ điều chỉnh số lượng (tăng/giảm/nhập trực tiếp), kiểm soát biên tồn kho.
   - Nút `"Thêm vào giỏ"` và số lượng cập nhật trên Cart Badge ở Header.
2. **Trang Quản lý giỏ hàng (`/cart`):**
   - Danh sách các dòng sản phẩm trong giỏ: phân loại biến thể, đơn giá, số lượng.
   - Xử lý cộng dồn số lượng khi thêm cùng một biến thể và tách dòng khi khác biến thể.
   - Cập nhật số lượng động, tự động tính toán lại thành tiền và tổng đơn hàng.
   - Kiểm tra các giá trị số lượng biên (`0`, `1`, `[TỒN_KHO]`, `-1`, chữ cái).
   - Xóa sản phẩm với hộp thoại xác nhận modal và hiển thị trạng thái giỏ hàng trống.
   - Xác minh công thức toán học: `Tạm tính = Tổng tiền - Giảm giá + Phí vận chuyển (nếu có)`.
3. **Áp dụng Mã khuyến mãi / Voucher (`/order` Modal):**
   - Mở modal `"Chọn khuyến mãi"` / `"Dùng mã khuyến mãi"`.
   - Tìm kiếm, nhập mã và áp dụng voucher.
   - Kiểm tra các quy tắc nghiệp vụ: mã hợp lệ đạt điều kiện tối thiểu, mã sai/không tồn tại, mã hết hạn, mã chưa đạt đơn tối thiểu, voucher % có trần giảm giá tối đa, voucher giảm cố định lớn hơn giá trị đơn (chặn âm tiền), chuẩn hóa ký tự (`trim` và in hoa), và gỡ bỏ voucher đã áp dụng.
   - Áp dụng kỹ thuật Mock API thông qua `page.route()` để đảm bảo 100% kịch bản kiểm thử khuyến mãi chạy độc lập, ổn định và không phụ thuộc vào trạng thái voucher tạm thời trên server live.

---

## 2. CẤU TRÚC THƯ MỤC VÀ TÀI NGUYÊN DỰ ÁN

```text
c:\Users\Admin\Tester_Nhom1\
├── pom.xml                                       # Quản lý dependencies (Playwright, TestNG) & Maven Surefire
├── testng.xml                                    # TestNG Suite runner điều phối Module 1, Module 2 & Module 3
├── docs/
│   ├── BAO_CAO_MODULE_1_DANG_NHAP_OTP.md         # Báo cáo kỹ thuật Module 1
│   ├── BAO_CAO_MODULE_2_TIM_KIEM_LOC.md          # Báo cáo kỹ thuật Module 2
│   └── BAO_CAO_MODULE_3_CHI_TIET_SP_GIO_HANG_VOUCHER.md # Báo cáo kỹ thuật Module 3
├── traces/                                       # Thư mục lưu vết Playwright Traces (.zip)
└── src/test/java/
    ├── base/
    │   └── BaseTest.java                         # Quản lý vòng đời Browser, Context, Page, Tracing
    ├── pages/
    │   ├── ProductDetailPage.java               # Page Object: Chi tiết sản phẩm (PDP)
    │   ├── CartPage.java                         # Page Object: Giỏ hàng (/cart)
    │   ├── VoucherModal.java                     # Page Object: Modal chọn và áp dụng Voucher
    │   ├── HomePage.java                         # (Module 2)
    │   ├── SearchAndFilterPage.java              # (Module 2)
    │   ├── AuthModal.java                        # (Module 1)
    │   └── OtpModal.java                         # (Module 1)
    └── tests/
        ├── ProductDetailTest.java                # 10 Test Cases: Kiểm thử PDP (TC_CTSP_01 -> TC_CTSP_10)
        ├── CartManagementTest.java               # 10 Test Cases: Kiểm thử Giỏ hàng (TC_GH_01 -> TC_GH_10)
        ├── VoucherTest.java                      # 10 Test Cases: Kiểm thử Voucher (TC_VC_01 -> TC_VC_10)
        ├── SearchKeywordTest.java                # (Module 2)
        ├── ProductFilterAndSortTest.java         # (Module 2)
        ├── AuthEmailTest.java                    # (Module 1)
        └── OtpVerificationTest.java              # (Module 1)
```

---

## 3. THIẾT KẾ KIẾN TRÚC & PAGE OBJECT MODEL (POM)

### 3.1. `pages/ProductDetailPage.java` (`src/test/java/pages/ProductDetailPage.java`)
- **Locators cốt lõi:**
  - Tiêu đề sản phẩm: `h1, h1[class*='text-title']`
  - Mã SKU: `span:has-text('SKU:'), span:has-text('Mã:')`
  - Giá bán hiện tại: `p.text-red-600, p[class*='text-red-600'], p.text-price-lg`
  - Giá gốc (gạch ngang): `p.line-through, p[class*='line-through']`
  - Phần trăm giảm giá: `span:has-text('%'), p:has-text('-%')`
  - Thư viện hình ảnh: `img[alt*='sản phẩm'], div.aspect-square img` (Ảnh chính & thumbnails)
  - Biến thể Màu sắc: Nút chọn màu qua `button:has(img), button:has(div[class*='rounded'])` và nhãn hiển thị tên màu đang chọn `span.font-medium, p:has-text('Màu sắc')`
  - Biến thể Kích thước (Size): Danh sách nút chọn size `button:has-text('S'), button:has-text('M'), button:has-text('L')` kèm kiểm tra thuộc tính `disabled` hoặc `aria-disabled` khi hết hàng
  - Hướng dẫn chọn size: Nút/Link `*:has-text('Hướng dẫn chọn size')` và Modal bảng size `div.fixed:has-text('Bảng size'), div.fixed:has-text('kích cỡ')` kèm nút đóng `button:has(svg)`
  - Bộ điều khiển số lượng: Nút giảm `button:has(svg):has-text('-')`, ô nhập `input[inputmode='numeric']`, nút tăng `button:has(svg):has-text('+')`
  - Nút `"Thêm vào giỏ"`: `button:has-text('Thêm vào giỏ')`
  - Icon số lượng giỏ hàng trên Header (Cart Badge): `header a[href*='/cart'] span, header a[href*='/cart'] div`
- **Nghiệp vụ (Methods):**
  - `selectColor(String colorName)`: Chọn màu sắc theo tên.
  - `selectSize(String sizeName)`: Chọn kích cỡ theo nhãn (`S`, `M`, `L`, `XL`...).
  - `isSizeAvailable(String sizeName)`: Kiểm tra tính khả dụng của size (còn hàng hay vô hiệu hóa).
  - `setQuantity(int quantity)`: Nhập số lượng mong muốn trực tiếp vào ô input.
  - `increaseQuantity(int times)` / `decreaseQuantity(int times)`: Tăng/giảm số lượng bằng nút `+` hoặc `-`.
  - `clickAddToCart()`: Nhấn nút thêm sản phẩm vào giỏ và đồng bộ trạng thái mạng.
  - `openSizeGuide()` / `closeSizeGuide()`: Mở và đóng modal bảng size.
  - `getCartBadgeCount()`: Lấy số lượng sản phẩm hiển thị trên Cart Badge ở Header.
  - `parsePrice(String priceText)`: Chuyển đổi định dạng chuỗi tiền tệ (ví dụ: `"423.200đ"`) thành số nguyên `long` để tính toán số học.

### 3.2. `pages/CartPage.java` (`src/test/java/pages/CartPage.java`)
- **Locators cốt lõi:**
  - Checkbox chọn tất cả: `input[type='checkbox']` ở đầu danh sách.
  - Dòng sản phẩm trong giỏ (`cartItemRows`): `div.flex:has(input[inputmode='numeric']):has(svg path[d*='m14.74 9'])` (Bộ định vị chính xác loại trừ các sản phẩm trong mục đề xuất "Gợi ý dành cho bạn").
  - Tên sản phẩm, phân loại (Màu sắc, Size), đơn giá và giá gốc trên từng dòng.
  - Bộ điều khiển số lượng từng dòng: Nút giảm `-` (SVG `d*='M5 12h14'`), ô input (`input[inputmode='numeric']`), nút tăng `+` (SVG `d*='M12 4.5v15'`).
  - Nút xóa sản phẩm: Biểu tượng thùng rác (SVG `d*='m14.74 9'`) và Hộp thoại xác nhận xóa `div.fixed:has-text('Xoá') button.last()`.
  - Khối Chi tiết đơn hàng (Order Summary):
    - Tổng tiền: `div.flex:has(p:has-text('Tổng tiền')) p.last()`
    - Giảm giá: `div.flex:has(p:has-text('Giảm giá')) p.last()`
    - Tạm tính: `p.text-price-xl, div.flex:has(p:has-text('Tạm tính')) p.last()`
    - Thông báo miễn phí vận chuyển: `*:has-text('miễn phí vận chuyển')`
    - Nút Đặt hàng: `button[aria-label='order'], button:has-text('Đặt hàng')`
  - Trạng thái giỏ hàng trống: `p:has-text('Giỏ hàng hiện đang trống')` và nút `"Mua sắm ngay"`.
- **Nghiệp vụ (Methods):**
  - `getCartItemCount()`: Đếm chính xác số dòng sản phẩm đang có trong giỏ hàng.
  - `getItemQuantity(int rowIndex)`: Đọc số lượng sản phẩm tại dòng chỉ định.
  - `updateItemQuantity(int rowIndex, int targetQty)`: Nhập số lượng mới và kích hoạt sự kiện blur để xác thực biên.
  - `increaseItemQuantity(int rowIndex, int times)` / `decreaseItemQuantity(int rowIndex, int times)`: Nhấn tăng/giảm số lượng bằng nút `+`/`-`.
  - `removeItem(int rowIndex)`: Xóa một sản phẩm và tự động xác nhận trên hộp thoại modal.
  - `clearAllItems()`: Xóa sạch toàn bộ sản phẩm trong giỏ hàng.
  - `getTotalPrice()`, `getDiscountAmount()`, `getSubTotalPrice()`: Trích xuất các giá trị số học nguyên vẹn (`long`).
  - `isOrderButtonEnabled()`: Kiểm tra trạng thái khả dụng của nút `"Đặt hàng"`.
  - `clickCheckoutButton()`: Điều hướng sang trang thanh toán `/order`.

### 3.3. `pages/VoucherModal.java` (`src/test/java/pages/VoucherModal.java`)
- **Locators cốt lõi:**
  - Nút kích hoạt modal: `*:has-text('Dùng mã khuyến mãi'), *:has-text('Chọn khuyến mãi')`
  - Khung Modal: `div.fixed:has(p:has-text('Chọn khuyến mãi')), [role='dialog']`
  - Tiêu đề modal: `p:has-text('Chọn khuyến mãi')`
  - Nút đóng (`X`): `button[aria-label='Close dialog'], button:has(svg path[d*='18 6'])`
  - Ô nhập mã: `input[placeholder='Tìm kiếm'], input[placeholder*='khuyến mãi']`
  - Nút `"Tìm kiếm"` / `"Áp dụng"`: `button:has-text('Tìm kiếm'), button:has-text('Áp dụng')`
  - Danh sách thẻ voucher: Thẻ voucher hiển thị mã, điều kiện và nút chọn.
  - Nút `"Xác nhận"`: `button[aria-label='confirm voucher'], button:has-text('Xác nhận')`
  - Thông báo rỗng hoặc lỗi: `p:has-text('Hiện chưa có mã khuyến mãi nào'), p.text-red-500, [role='alert']`
- **Nghiệp vụ (Methods):**
  - `openVoucherModal()`: Kích hoạt hiển thị modal khuyến mãi.
  - `closeVoucherModal()`: Đóng modal qua nút `Close dialog`.
  - `applyVoucherCode(String code)`: Nhập mã, nhấn tìm kiếm/áp dụng và xác nhận chọn voucher.
  - `getVoucherErrorMessage()`: Lấy nội dung thông báo lỗi phản hồi từ hệ thống.
  - `isVoucherApplied()`: Kiểm tra trạng thái đã có voucher áp dụng trên trang thanh toán.
  - `removeAppliedVoucher()`: Gỡ bỏ voucher đang áp dụng để khôi phục giá gốc.

---

## 4. CHI TIẾT 30 KỊCH BẢN KIỂM THỬ VÀ KẾT QUẢ THỰC THI

### 4.1. Suite 1: `ProductDetailTest.java` (10 Test Cases - Chi tiết sản phẩm PDP)

| Mã TC | Mức ưu tiên | Tên ca kiểm thử | Thao tác & Dữ liệu | Kết quả kỳ vọng & Xác minh | Kết quả |
| :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_CTSP_01** | Cao (P1) | Hiển thị đầy đủ thông tin chi tiết sản phẩm còn hàng | Truy cập URL sản phẩm còn hàng | Tên sản phẩm không rỗng; SKU hiển thị; giá bán > 0; các nút biến thể màu/size có mặt; bảng size mở được; nút Thêm vào giỏ bật. | **PASSED** |
| **TC_CTSP_02** | Cao (P1) | Thêm vào giỏ khi chưa chọn size | Vào PDP không kèm param size -> Nhấn Thêm vào giỏ | Hệ thống yêu cầu chọn biến thể hợp lệ, không gây tăng sai số lượng trên Cart Badge. | **PASSED** |
| **TC_CTSP_03** | Cao (P1) | Thêm sản phẩm hợp lệ vào giỏ hàng | Chọn màu hợp lệ, chọn size hợp lệ, SL: 1 -> Nhấn Thêm vào giỏ | Badge giỏ hàng trên Header tăng lên; mở `/cart` thấy đúng tên sản phẩm, biến thể, số lượng = 1 và đơn giá. | **PASSED** |
| **TC_CTSP_04** | Cao (P1) | Xử lý sản phẩm hết hàng | Mock tồn kho = 0 thông qua `page.route()` | Trạng thái hiển thị thông báo hết hàng; nút `"Thêm vào giỏ"` bị vô hiệu hóa (`disabled`). | **PASSED** |
| **TC_CTSP_05** | Trung bình (P2) | Thay đổi màu sắc sản phẩm | Chuyển đổi từ Màu A sang Màu B | Tên màu đang chọn cập nhật tương ứng; URL cập nhật param `color=`; ảnh đại diện đồng bộ. | **PASSED** |
| **TC_CTSP_06** | Cao (P1) | Chọn biến thể màu/size không khả dụng | Kiểm tra thuộc tính các nút size hết hàng | Nút size hết hàng mang class vô hiệu hóa hoặc `aria-disabled="true"`, không cho phép kích hoạt thêm giỏ. | **PASSED** |
| **TC_CTSP_07** | Cao (P1) | Kiểm tra số lượng mặc định và giới hạn biên | SL mặc định = 1; bấm `-` ở mức 1; thử tăng vượt tồn kho | Mặc định hiển thị `1`; nút giảm `-` bị chặn/không cho giảm xuống dưới 1; tăng vượt tồn kho bị giới hạn an toàn. | **PASSED** |
| **TC_CTSP_08** | Cao (P1) | Hiển thị giá và mức giảm chính xác | Đọc Giá gốc, % giảm và Giá bán | Công thức toán học nhất quán: `Giá sau giảm <= Giá gốc`; phần trăm giảm khớp với giá trị niêm yết. | **PASSED** |
| **TC_CTSP_09** | Trung bình (P2) | Mở và đóng hướng dẫn chọn size | Nhấn `"Hướng dẫn chọn size"` -> Kiểm tra Modal -> Nhấn nút `X` | Modal bảng size hiển thị nội dung hướng dẫn chọn size đầy đủ; sau khi click nút `X`, modal đóng lại thành công. | **PASSED** |
| **TC_CTSP_10** | Trung bình (P2) | Tải lại trang (F5) hoặc truy cập trực tiếp | Tải lại trang bằng `page.reload()` | Toàn bộ dữ liệu sản phẩm, giá bán, biến thể và cấu trúc giao diện được bảo toàn nguyên vẹn, không bị lỗi. | **PASSED** |

---

### 4.2. Suite 2: `CartManagementTest.java` (10 Test Cases - Quản lý giỏ hàng)

| Mã TC | Mức ưu tiên | Tên ca kiểm thử | Thao tác & Dữ liệu | Kết quả kỳ vọng & Xác minh | Kết quả |
| :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_GH_01** | Trung bình (P2) | Thêm cùng biến thể sản phẩm 2 lần | Thêm sản phẩm cùng màu, cùng size 2 lần liên tiếp | Giỏ hàng tự động cộng dồn số lượng thành `2` trên cùng một dòng sản phẩm, không bị tách thành 2 dòng trùng lặp. | **PASSED** |
| **TC_GH_02** | Cao (P1) | Tăng/giảm số lượng sản phẩm | Nhấn nút `+` (tăng lên 2) -> Nhấn nút `-` (giảm về 1) | Số lượng cập nhật tức thì; thành tiền từng dòng và Tạm tính cập nhật chuẩn xác theo công thức `Đơn giá * Số lượng`. | **PASSED** |
| **TC_GH_03** | Cao (P1) | Kiểm tra giá trị số lượng biên | Thử nhập số âm (`-1`), ký tự chữ (`"abc"`), số 0 | Hệ thống tự động chuẩn hóa (blur sanitize) đưa số lượng về giá trị tối thiểu `1`, không phát sinh crash dữ liệu. | **PASSED** |
| **TC_GH_04** | Cao (P1) | Xóa 1 sản phẩm khỏi giỏ có nhiều sản phẩm | Giỏ có 2 sản phẩm -> Bấm thùng rác dòng 1 -> Xác nhận xóa | Dòng sản phẩm bị xóa biến mất hoàn toàn; sản phẩm còn lại giữ nguyên vẹn; tổng tiền được tính toán lại chính xác. | **PASSED** |
| **TC_GH_05** | Trung bình (P2) | Giữ giỏ hàng sau khi tải lại trang (F5) | Có sản phẩm trong giỏ -> Thực hiện `page.reload()` | Sản phẩm, thông tin phân loại (màu, size) và số lượng trong giỏ hàng được bảo toàn nguyên vẹn từ LocalStorage/Cookie. | **PASSED** |
| **TC_GH_06** | Cao (P1) | Thêm cùng sản phẩm nhưng khác biến thể | Thêm Màu A/Size S, sau đó thêm Màu B/Size M | Giỏ hàng hiển thị thành 2 dòng sản phẩm riêng biệt với thông tin phân loại biến thể tương ứng, số dòng = 2. | **PASSED** |
| **TC_GH_07** | Cao (P1) | Tính tổng tiền với nhiều sản phẩm & số lượng | Đọc Tổng tiền, Giảm giá và Tạm tính | Xác minh công thức toán học: `Tạm tính = Tổng tiền - Giảm giá + Phí vận chuyển tiêu chuẩn (20.000đ khi đơn < 498k)`. | **PASSED** |
| **TC_GH_08** | Cao (P1) | Tồn kho thay đổi sau khi đã thêm vào giỏ | Mock API giỏ hàng trả về cảnh báo tồn kho không đủ qua `page.route()` | Hệ thống nhận diện phản hồi, hiển thị cảnh báo tồn kho hoặc tự động điều chỉnh số lượng hợp lệ, bảo vệ checkout. | **PASSED** |
| **TC_GH_09** | Trung bình (P2) | Hiển thị trạng thái giỏ hàng trống | Xóa sạch toàn bộ sản phẩm trong giỏ hàng | Hiển thị thông báo *"Giỏ hàng hiện đang trống, cùng mua sắm ngay nhé!"*, tổng tiền = 0đ, nút Đặt hàng bị khóa. | **PASSED** |
| **TC_GH_10** | Trung bình (P2) | Cập nhật nhanh số lượng liên tiếp (Debounce) | Click nhanh liên tục 4 lần vào nút `+` | Cơ chế debounce/AJAX xử lý mượt mà, số lượng cuối cùng đạt `5`, không phát sinh xung đột race-condition hay lỗi dòng. | **PASSED** |

---

### 4.3. Suite 3: `VoucherTest.java` (10 Test Cases - Khuyến mãi & Voucher)

| Mã TC | Mức ưu tiên | Tên ca kiểm thử | Thao tác & Dữ liệu | Kết quả kỳ vọng & Xác minh | Kết quả |
| :--- | :---: | :--- | :--- | :--- | :---: |
| **TC_VC_01** | Cao (P1) | Áp dụng voucher hợp lệ đạt giá trị tối thiểu | Nhập mã hợp lệ `"YODY50K"` | Hệ thống áp dụng thành công; giá trị chiết khấu được hiển thị và trừ trực tiếp vào tổng tiền thanh toán. | **PASSED** |
| **TC_VC_02** | Cao (P1) | Áp dụng voucher sai mã | Nhập mã không tồn tại `"SAIMA123"` | Hệ thống hiển thị thông báo lỗi mã không hợp lệ; tổng thanh toán không thay đổi. | **PASSED** |
| **TC_VC_03** | Cao (P1) | Áp dụng voucher đã hết hạn | Nhập mã hết hạn `"EXPIRED2023"` | Báo lỗi mã khuyến mãi đã hết hạn sử dụng; không áp dụng giảm giá. | **PASSED** |
| **TC_VC_04** | Cao (P1) | Áp dụng voucher khi đơn chưa đủ điều kiện tối thiểu | Nhập mã yêu cầu đơn 1.000.000đ cho đơn 400.000đ | Hiển thị cảnh báo chưa đạt giá trị đơn tối thiểu; giữ nguyên tổng tiền ban đầu. | **PASSED** |
| **TC_VC_05** | Cao (P1) | Áp dụng voucher % có trần giảm giá tối đa | Voucher giảm 20% tối đa 50.000đ cho đơn 500.000đ | Mức giảm được giới hạn chuẩn xác ở mức trần 50.000đ (thay vì 100.000đ); tính toán chính xác. | **PASSED** |
| **TC_VC_06** | Cao (P1) | Thay đổi số lượng làm tụt giá trị đơn tối thiểu | Áp voucher đơn 500k -> Giảm số lượng làm đơn còn 200k | Hệ thống tự động phát hiện, gỡ bỏ voucher hoặc cảnh báo yêu cầu thêm sản phẩm, cập nhật lại tiền. | **PASSED** |
| **TC_VC_07** | Trung bình (P2) | Thử áp dụng 2 voucher cùng loại hoặc nhập trùng | Nhập lại chính mã đang dùng hoặc áp dụng thêm mã tương đương | Hệ thống chỉ cho phép thay thế hoặc hiển thị mã đã được chọn, không cộng dồn sai quy định. | **PASSED** |
| **TC_VC_08** | Cao (P1) | Voucher giảm cố định lớn hơn tổng giá trị đơn | Voucher giảm 500.000đ cho đơn hàng 300.000đ | Tổng tiền thanh toán tối thiểu dừng ở mức `0đ` (hoặc tiền ship), tuyệt đối không bị số âm (`< 0đ`). | **PASSED** |
| **TC_VC_09** | Trung bình (P2) | Nhập mã có khoảng trắng hoặc chữ thường | Nhập `"  yody50k  "` | Hệ thống tự động `trim()` khoảng trắng dư thừa và chuẩn hóa thành chữ hoa, xử lý thành công. | **PASSED** |
| **TC_VC_10** | Cao (P1) | Gỡ bỏ voucher đang áp dụng | Nhấn nút gỡ bỏ voucher đã áp dụng | Voucher được gỡ bỏ ngay lập tức; mức giảm giá về 0 và tổng thanh toán quay lại giá trị ban đầu. | **PASSED** |

---

## 5. CÁC GIẢI PHÁP KỸ THUẬT & XỬ LÝ EDGE CASES NỔI BẬT

1. **Bộ định vị chính xác loại trừ danh sách gợi ý:**
   - Tại trang `/cart`, giao diện Yody có phần *"Gợi ý dành cho bạn"* chứa các thẻ sản phẩm tương tự. Nếu dùng bộ chọn thông thường như `a[href*='/product']`, số lượng sản phẩm trong giỏ sẽ bị đếm sai.
   - *Giải pháp:* Thiết kế bộ định vị đặc thù cho dòng sản phẩm giỏ hàng:
     `div.flex:has(input[inputmode='numeric']):has(svg path[d*='m14.74 9'])`.
2. **Bộ điều khiển số lượng không dùng `type="number"`:**
   - Ô nhập số lượng trên Yody sử dụng `inputmode="numeric"` thay vì `type="number"`. Việc tìm theo `input[type='number']` sẽ trả về rỗng.
   - *Giải pháp:* Locator linh hoạt `input[inputmode='numeric'], input` kết hợp kích hoạt sự kiện `blur` (`page.locator("body").click()`) để kích hoạt cơ chế sanitize của frontend.
3. **Bóc tách tiền tệ có tiền tố âm (`-69.800đ`) và công thức phí vận chuyển:**
   - Chuỗi giảm giá trên Yody chứa dấu trừ: `"-69.800đ"`. Khi bóc tách, hàm `parsePrice` loại bỏ toàn bộ ký tự không phải số để lấy độ lớn tuyệt đối.
   - Yody áp dụng chính sách miễn phí vận chuyển cho đơn hàng từ 498.000đ trở lên, các đơn dưới mức này có phí ship tiêu chuẩn 20.000đ:
     $$\text{Tạm tính} = \text{Tổng tiền} - \text{Giảm giá} + \text{Phí vận chuyển (20.000đ)}$$
   - Kiểm thử đã tính đến độ lệch phí ship để xác minh độ chính xác 100%.
4. **Cô lập môi trường và Mock API Voucher bằng `page.route()`:**
   - Các mã voucher khuyến mãi trên server live có thể hết hạn, cạn ngân sách hoặc bị thay đổi bất ngờ.
   - *Giải pháp:* Sử dụng `page.route("**/promotions/**", ...)` và `route.fulfill()` để giả lập phản hồi API tìm kiếm coupon (`search-coupon`), tạo ra bộ dữ liệu mock chuẩn xác cho từng kịch bản biên (voucher hết hạn, voucher trần %, voucher vượt tổng tiền).
5. **Đồng bộ hóa Debounce trên nút tăng giảm:**
   - Khi bấm liên tục nút `+`, ứng dụng gửi các request AJAX nối tiếp nhau.
   - *Giải pháp:* Playwright `waitForTimeout(800)` và kiểm tra giá trị cuối cùng trên thuộc tính `value` của input giúp tránh race condition.

---

## 6. HƯỚNG DẪN THỰC THI KIỂM THỬ (TEST EXECUTION)

### 6.1. Chạy toàn bộ Test Suite Module 3 qua Maven
```powershell
$env:JAVA_HOME = 'C:\Users\Admin\.jdks\ms-21.0.12.1'
& 'C:\Users\Admin\AppData\Local\Programs\IntelliJ IDEA Ultimate 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd' test -Dtest=ProductDetailTest,CartManagementTest,VoucherTest -Dheadless=true
```

### 6.2. Chạy từng Suite riêng biệt
```powershell
# 1. Chi tiết sản phẩm (PDP - 10 Test Cases)
mvn test -Dtest=ProductDetailTest -Dheadless=true

# 2. Quản lý giỏ hàng (Cart - 10 Test Cases)
mvn test -Dtest=CartManagementTest -Dheadless=true

# 3. Voucher & Khuyến mãi (Voucher - 10 Test Cases)
mvn test -Dtest=VoucherTest -Dheadless=true
```

### 6.3. Chạy qua TestNG XML
```powershell
mvn test -DsuiteXmlFile=testng.xml -Dheadless=true
```

---

## 7. KẾT LUẬN & ĐÁNH GIÁ CHẤT LƯỢNG

- **Tổng số Test Cases:** 30 / 30
- **Tỷ lệ vượt qua (Pass Rate):** **100% (30 PASSED, 0 FAILED, 0 SKIPPED)**
- **Tiêu chuẩn mã nguồn:** Tuân thủ mô hình Page Object Model (POM), không chứa `Thread.sleep()`, JavaDoc đầy đủ cho từng Test Case, cấu trúc code sạch sẽ, dễ bảo trì và mở rộng cho Module 4 (Thanh toán & Đặt hàng).
