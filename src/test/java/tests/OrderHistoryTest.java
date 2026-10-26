package tests;

import base.AccountTestHelper;
import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.OrderDetailPage;
import pages.OrderHistoryPage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 5 - Lịch sử & Chi tiết đơn hàng (Order History Tests)
 * Platform: Yody.vn
 *
 * Implements 8 test cases (TC_LS_01 -> TC_LS_08) covering:
 * - Order history card rendering (Code, Date, Total, Status)
 * - Status tab filtering ("Tất cả", "Chờ xác nhận", "Chuẩn bị hàng", "Đang giao hàng", "Đã giao", "Đã hủy")
 * - Order detail inspection (Recipient, products, payment)
 * - Mathematical pricing verification (Thành tiền = Tổng tiền + Phí - Giảm giá)
 * - Immediate appearance of newly placed orders at position 1
 * - Empty state representation for zero-order accounts
 * - Insecure Direct Object References (IDOR) prevention across user accounts
 * - Smooth back navigation without session loss
 */
public class OrderHistoryTest extends BaseTest {

    private OrderHistoryPage orderHistoryPage;

    @Override
    protected boolean shouldNavigateHomeOnInit() {
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public void setupOrderHistoryTest() {
        AccountTestHelper.setupOrderHistorySession(page);
        orderHistoryPage = new OrderHistoryPage(page);
        orderHistoryPage.navigateToOrderHistory();
    }

    /**
     * Test Case ID: TC_LS_01
     * Tiêu đề: Xem danh sách đơn hàng của tài khoản đã có đơn
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hiển thị danh sách đúng các đơn hàng của tài khoản; mỗi đơn hiển thị đầy đủ
     * Mã đơn (#SO...), Ngày đặt, Tổng tiền và Trạng thái.
     */
    @Test(description = "TC_LS_01 - Xem danh sách đơn hàng của tài khoản")
    public void testViewOrderHistoryList() {
        assertThat(orderHistoryPage.getTitleHeading()).isVisible();

        // Kiểm tra số lượng đơn hàng có trong danh sách
        int count = orderHistoryPage.getOrderCardCount();
        Assert.assertTrue(count > 0, "Không có đơn hàng nào được hiển thị trong lịch sử mua hàng");

        // Kiểm tra thông tin đơn hàng đầu tiên
        String orderCode = orderHistoryPage.getFirstOrderCode();
        String orderStatus = orderHistoryPage.getFirstOrderStatus();
        String totalAmount = orderHistoryPage.getFirstOrderTotalAmount();

        Assert.assertTrue(orderCode.startsWith("#SO"), "Mã đơn hàng không đúng định dạng: " + orderCode);
        Assert.assertFalse(orderStatus.isEmpty(), "Trạng thái đơn hàng bị trống");
        Assert.assertTrue(totalAmount.contains("đ"), "Tổng tiền đơn hàng không có đơn vị VNĐ: " + totalAmount);
    }

    /**
     * Test Case ID: TC_LS_02
     * Tiêu đề: Hiển thị và lọc trạng thái đơn hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chuyển đổi qua lại giữa các Tab trạng thái; mỗi đơn hiển thị đúng nhãn trạng thái theo danh mục hệ thống.
     */
    @Test(description = "TC_LS_02 - Hiển thị và lọc trạng thái đơn hàng theo Tab")
    public void testFilterOrdersByStatusTabs() {
        // 1. Lọc tab "Chờ xác nhận"
        orderHistoryPage.filterByStatusTab("Chờ xác nhận");
        Assert.assertTrue(orderHistoryPage.getOrderCardCount() >= 1, "Tab Chờ xác nhận không có đơn hàng nào");
        Assert.assertEquals(orderHistoryPage.getFirstOrderStatus(), "Chờ xác nhận");

        // 2. Lọc tab "Đang giao hàng"
        orderHistoryPage.filterByStatusTab("Đang giao hàng");
        Assert.assertTrue(orderHistoryPage.getOrderCardCount() >= 1, "Tab Đang giao hàng không có đơn hàng nào");
        Assert.assertEquals(orderHistoryPage.getFirstOrderStatus(), "Đang giao hàng");

        // 3. Lọc tab "Đã giao"
        orderHistoryPage.filterByStatusTab("Đã giao");
        Assert.assertTrue(orderHistoryPage.getOrderCardCount() >= 1, "Tab Đã giao không có đơn hàng nào");
        Assert.assertEquals(orderHistoryPage.getFirstOrderStatus(), "Đã giao");

        // 4. Quay lại tab "Tất cả"
        orderHistoryPage.filterByStatusTab("Tất cả");
        Assert.assertTrue(orderHistoryPage.getOrderCardCount() >= 5, "Tab Tất cả không hiển thị toàn bộ đơn hàng");
    }

    /**
     * Test Case ID: TC_LS_03
     * Tiêu đề: Xem chi tiết một đơn hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Trang chi tiết hiển thị khớp sản phẩm, màu sắc, kích thước, số lượng,
     * thông tin địa chỉ nhận hàng, phương thức thanh toán và tổng tiền.
     */
    @Test(description = "TC_LS_03 - Xem chi tiết một đơn hàng")
    public void testViewOrderDetail() {
        OrderDetailPage detailPage = orderHistoryPage.clickFirstOrderDetail();

        // Kiểm tra tiêu đề mã đơn
        assertThat(detailPage.getOrderHeaderCodeLoc()).isVisible();
        Assert.assertTrue(detailPage.getOrderHeaderCode().contains("SO047342456"), "Mã đơn hàng chi tiết không khớp");

        // Kiểm tra thông tin người nhận
        Assert.assertEquals(detailPage.getRecipientName(), "Nguyen Van Test");
        Assert.assertTrue(detailPage.getRecipientPhone().contains("***"), "Số điện thoại chưa được che bảo mật");
        Assert.assertTrue(detailPage.getRecipientAddress().contains("Hà Nội"), "Địa chỉ giao nhận chưa chính xác");

        // Kiểm tra sản phẩm trong đơn
        Assert.assertTrue(detailPage.getFirstProductName().contains("Áo Polo Nữ"), "Tên sản phẩm không khớp");
        Assert.assertTrue(detailPage.getFirstProductVariant().contains("Hồng"), "Màu sắc không khớp");
        Assert.assertTrue(detailPage.getFirstProductVariant().contains("M"), "Kích thước không khớp");
        Assert.assertEquals(detailPage.getFirstProductQuantity(), "Số lượng: 1");

        // Kiểm tra phương thức thanh toán
        assertThat(detailPage.getPaymentMethodLoc()).hasText("Thanh toán khi nhận hàng (COD)");
    }

    /**
     * Test Case ID: TC_LS_04
     * Tiêu đề: Tổng tiền trong chi tiết đơn khớp với công thức khi đặt hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Kiểm chứng toán học Thành tiền = Tổng tiền + Phí vận chuyển - Giảm giá.
     * Ví dụ thực tế: 599.000đ + 20.000đ - 200.000đ = 419.000đ.
     */
    @Test(description = "TC_LS_04 - Kiểm chứng công thức tính toán tài chính chi tiết đơn hàng")
    public void testFinancialMathBreakdown() {
        OrderDetailPage detailPage = orderHistoryPage.clickFirstOrderDetail();

        long totalPrice = detailPage.getTotalPrice();
        long discount = detailPage.getDiscountAmount();
        long shipping = detailPage.getShippingFee();
        long finalAmount = detailPage.getFinalAmount();

        // Kiểm tra các giá trị độc lập
        Assert.assertEquals(totalPrice, 599000L, "Tổng tiền sản phẩm chưa chính xác");
        Assert.assertEquals(discount, 200000L, "Số tiền giảm giá chưa chính xác");
        Assert.assertEquals(shipping, 20000L, "Phí vận chuyển chưa chính xác");
        Assert.assertEquals(finalAmount, 419000L, "Thành tiền thanh toán chưa chính xác");

        // Đối chiếu công thức toán học: Thành tiền = Tổng tiền + Phí vận chuyển - Giảm giá
        long expectedFinalAmount = totalPrice + shipping - discount;
        Assert.assertEquals(finalAmount, expectedFinalAmount,
                String.format("Sai lệch công thức tài chính! Thực tế: %d, Mong đợi: %d", finalAmount, expectedFinalAmount));

        // Kiểm tra dòng ghi chú tiết kiệm
        assertThat(detailPage.getSavingNoteLoc()).hasText("(Tiết kiệm 200.000đ)");
    }

    /**
     * Test Case ID: TC_LS_05
     * Tiêu đề: Đơn hàng vừa đặt xuất hiện ngay trong danh sách
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Đơn hàng mới xuất hiện ở vị trí đầu tiên (mới nhất) trong danh sách với trạng thái "Chờ xác nhận".
     */
    @Test(description = "TC_LS_05 - Đơn hàng vừa đặt xuất hiện ở vị trí đầu tiên")
    public void testNewlyPlacedOrderAppearsFirst() {
        // Giả lập trạng thái sau khi vừa đặt thành công 1 đơn COD mới (#SO047349999)
        AccountTestHelper.setupOrderHistoryWithNewlyPlacedOrder(page);
        orderHistoryPage.navigateToOrderHistory();

        // Kiểm tra đơn hàng đầu tiên chính là đơn vừa tạo
        String firstCode = orderHistoryPage.getFirstOrderCode();
        String firstStatus = orderHistoryPage.getFirstOrderStatus();

        Assert.assertEquals(firstCode, "#SO047349999", "Đơn hàng mới tạo không đứng ở vị trí đầu tiên");
        Assert.assertEquals(firstStatus, "Chờ xác nhận", "Đơn hàng mới tạo không ở trạng thái Chờ xác nhận");
    }

    /**
     * Test Case ID: TC_LS_06
     * Tiêu đề: Tài khoản mới chưa có đơn hàng nào
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hiển thị thông báo trạng thái rỗng (chưa có đơn hàng nào); giao diện không bị lỗi trang trắng hay crash.
     */
    @Test(description = "TC_LS_06 - Trạng thái rỗng khi tài khoản chưa có đơn hàng")
    public void testEmptyOrderHistoryState() {
        // Thiết lập trạng thái tài khoản chưa có đơn hàng
        AccountTestHelper.setupEmptyOrderHistorySession(page);
        orderHistoryPage.navigateToOrderHistory();

        // Kiểm tra thông báo trạng thái rỗng hiển thị rõ ràng
        assertThat(orderHistoryPage.getEmptyStateNotice()).isVisible();
        Assert.assertTrue(orderHistoryPage.getEmptyStateNotice().innerText().contains("chưa có đơn hàng nào"),
                "Không hiển thị thông báo chưa có đơn hàng nào");
        Assert.assertEquals(orderHistoryPage.getOrderCardCount(), 0, "Vẫn hiển thị thẻ đơn hàng khi tài khoản chưa có đơn");
    }

    /**
     * Test Case ID: TC_LS_07
     * Tiêu đề: Bảo mật chống truy cập ngang (IDOR) - Truy cập chi tiết đơn hàng của tài khoản khác
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hệ thống từ chối quyền truy cập (403/404) hoặc báo không tìm thấy đơn hàng;
     * tuyệt đối không làm lộ thông tin cá nhân và đơn hàng của Tài khoản B.
     */
    @Test(description = "TC_LS_07 - Bảo mật chống truy cập ngang (IDOR)")
    public void testIdorOrderAccessForbidden() {
        // Tài khoản A cố gắng truy cập trực tiếp URL đơn hàng của Tài khoản B
        page.navigate("https://yody.vn/account/orders/SO_TAI_KHOAN_B");

        // Hệ thống phải từ chối quyền truy cập
        assertThat(page.locator("#error-title, h1:has-text('Không tìm thấy đơn hàng')")).isVisible();
        assertThat(page.locator("#error-message, p:has-text('không có quyền truy cập')")).isVisible();

        // Xác minh tuyệt đối không để lộ dữ liệu nhạy cảm (tên, số điện thoại, địa chỉ) của Tài khoản B
        String pageBody = page.locator("body").innerText();
        Assert.assertFalse(pageBody.contains("Tài khoản B"), "Bị rò rỉ thông tin tên của tài khoản B!");
        Assert.assertFalse(pageBody.contains("0988888888"), "Bị rò rỉ số điện thoại của tài khoản B!");
    }

    /**
     * Test Case ID: TC_LS_08
     * Tiêu đề: Quay lại danh sách từ trang chi tiết đơn hàng
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Nhấn nút quay lại (←) trên giao diện hoặc page.goBack(); điều hướng trở về
     * trang Danh sách lịch sử mua hàng mượt mà, không bị mất phiên đăng nhập.
     */
    @Test(description = "TC_LS_08 - Quay lại danh sách từ chi tiết đơn hàng")
    public void testBackToListFromOrderDetail() {
        OrderDetailPage detailPage = orderHistoryPage.clickFirstOrderDetail();
        assertThat(detailPage.getOrderHeaderCodeLoc()).isVisible();

        // 1. Thao tác nhấn nút quay lại (←) trên giao diện
        OrderHistoryPage returnedList = detailPage.clickBackToList();
        assertThat(returnedList.getTitleHeading()).isVisible();
        Assert.assertTrue(returnedList.getOrderCardCount() > 0, "Mất danh sách đơn hàng sau khi quay lại");

        // 2. Vào lại chi tiết và dùng page.goBack() của Playwright
        orderHistoryPage.clickFirstOrderDetail();
        assertThat(detailPage.getOrderHeaderCodeLoc()).isVisible();
        page.goBack();

        assertThat(orderHistoryPage.getTitleHeading()).isVisible();
    }
}
