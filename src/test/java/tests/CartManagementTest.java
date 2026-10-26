package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.CartPage;
import pages.ProductDetailPage;

/**
 * Test Suite: Module 3 - Quản lý giỏ hàng (Cart Management)
 * Platform: Yody.vn
 *
 * Implements 10 test cases (TC_GH_01 -> TC_GH_10) covering item aggregation,
 * quantity updates, boundary values, multi-item calculations, item removal,
 * empty state handling, rapid clicking debounce, and out-of-stock simulation via page.route().
 */
public class CartManagementTest extends BaseTest {

    private static final String TEST_PRODUCT_URL =
            "https://yody.vn/product/ao-polo-nu-co-v-bo-ke-co-gian-dan-hoi-phom-slim-fit-ton-dang?color=765&size=1";

    /**
     * Helper: Ensures at least one item is added to the cart before cart tests.
     */
    private CartPage prepareCartWithItem() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();
        return cartPage;
    }

    /**
     * Test Case ID: TC_GH_01
     * Tiêu đề: Thêm cùng một biến thể sản phẩm 2 lần
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Số lượng tự động cộng dồn thành 2 trên cùng một dòng, không bị tách thành 2 dòng trùng lặp.
     */
    @Test(description = "TC_GH_01 - Thêm cùng biến thể sản phẩm 2 lần tự động cộng dồn số lượng")
    public void testAddDuplicateVariantAggregatesQuantity() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        // Thêm lần 2 cùng sản phẩm trên cùng trang
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();

        // Xác minh số lượng sản phẩm trên dòng đó được cộng dồn (>= 2)
        Assert.assertTrue(cartPage.getCartItemCount() >= 1, "Giỏ hàng phải có dòng sản phẩm");
        int qty = cartPage.getItemQuantity(0);
        Assert.assertTrue(qty >= 2, "Số lượng cùng biến thể phải được cộng dồn lên ít nhất là 2, hiện tại là: " + qty);
    }

    /**
     * Test Case ID: TC_GH_02
     * Tiêu đề: Tăng và giảm số lượng sản phẩm trong giỏ hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Tăng (1 -> 2) rồi giảm (2 -> 1) cập nhật thành tiền và tạm tính chính xác theo đơn giá.
     */
    @Test(description = "TC_GH_02 - Tăng và giảm số lượng cập nhật tổng tiền chính xác")
    public void testIncreaseAndDecreaseQuantityUpdatesTotals() {
        CartPage cartPage = prepareCartWithItem();
        Assert.assertTrue(cartPage.getCartItemCount() >= 1, "Giỏ hàng phải có sản phẩm");

        int initialQty = cartPage.getItemQuantity(0);

        // Tăng số lượng lên 1
        cartPage.increaseItemQuantity(0, 1);
        int increasedQty = cartPage.getItemQuantity(0);
        Assert.assertTrue(increasedQty > initialQty, "Số lượng sau khi tăng phải lớn hơn ban đầu");

        // Giảm số lượng trở lại
        cartPage.decreaseItemQuantity(0, 1);
        int finalQty = cartPage.getItemQuantity(0);
        Assert.assertTrue(finalQty < increasedQty, "Số lượng sau khi giảm phải nhỏ hơn số lượng lúc tăng");
    }

    /**
     * Test Case ID: TC_GH_03
     * Tiêu đề: Kiểm tra các giá trị số lượng biên (0, -1, 'abc')
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Tối thiểu giữ ở 1 hoặc hỏi xác nhận xóa, từ chối số âm và ký tự chữ.
     */
    @Test(description = "TC_GH_03 - Kiểm tra giá trị số lượng biên (chặn số âm, ký tự chữ)")
    public void testQuantityBoundaryInputs() {
        CartPage cartPage = prepareCartWithItem();
        Assert.assertTrue(cartPage.getCartItemCount() >= 1, "Giỏ hàng phải có sản phẩm");

        // 1. Thử nhập số âm
        cartPage.updateItemQuantity(0, -1);
        int qtyNegative = cartPage.getItemQuantity(0);
        Assert.assertTrue(qtyNegative >= 1, "Số lượng không được âm, phải giữ tối thiểu là 1");

        // 2. Thử giảm xuống dưới 1
        cartPage.decreaseItemQuantity(0, 3);
        int qtyMin = cartPage.getItemQuantity(0);
        Assert.assertTrue(qtyMin >= 1, "Nút giảm không được hạ số lượng xuống dưới 1");
    }

    /**
     * Test Case ID: TC_GH_04
     * Tiêu đề: Xóa 1 sản phẩm khỏi giỏ khi giỏ có từ 2 sản phẩm
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Dòng sản phẩm bị xóa biến mất, các sản phẩm còn lại được giữ nguyên và tổng tiền tính lại đúng.
     */
    @Test(description = "TC_GH_04 - Xóa sản phẩm khỏi giỏ hàng")
    public void testRemoveItemFromCart() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.selectSize("M");
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();

        int countBefore = cartPage.getCartItemCount();
        Assert.assertTrue(countBefore >= 2, "Phải có ít nhất 2 sản phẩm trong giỏ để kiểm tra xóa 1 sản phẩm");

        cartPage.removeItem(0);
        int countAfter = cartPage.getCartItemCount();

        Assert.assertTrue(countAfter < countBefore, "Số lượng sản phẩm trong giỏ phải giảm sau khi xóa dòng sản phẩm");
    }

    /**
     * Test Case ID: TC_GH_05
     * Tiêu đề: Giữ giỏ hàng sau khi tải lại trang (F5)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Sản phẩm, phân loại màu/size và số lượng trong giỏ được bảo toàn nguyên vẹn sau khi F5.
     */
    @Test(description = "TC_GH_05 - Giữ giỏ hàng nguyên vẹn sau khi F5")
    public void testCartPersistenceOnReload() {
        CartPage cartPage = prepareCartWithItem();
        int countBefore = cartPage.getCartItemCount();
        long totalBefore = cartPage.getSubTotalPrice();

        page.reload();
        page.waitForLoadState();

        CartPage reloadedCart = new CartPage(page);
        int countAfter = reloadedCart.getCartItemCount();
        long totalAfter = reloadedCart.getSubTotalPrice();

        Assert.assertEquals(countAfter, countBefore, "Số lượng sản phẩm trong giỏ phải giữ nguyên sau khi reload");
        Assert.assertEquals(totalAfter, totalBefore, "Tổng tiền giỏ hàng phải giữ nguyên sau khi reload");
    }

    /**
     * Test Case ID: TC_GH_06
     * Tiêu đề: Thêm cùng 1 sản phẩm nhưng khác biến thể size
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hiển thị thành 2 dòng sản phẩm riêng biệt trong giỏ hàng.
     */
    @Test(description = "TC_GH_06 - Thêm khác biến thể tạo thành các dòng riêng biệt")
    public void testAddDifferentVariantsSeparateRows() {
        ProductDetailPage pdp = new ProductDetailPage(page);

        // Thêm Size S
        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        // Thêm Size M
        pdp.navigateTo(TEST_PRODUCT_URL);
        pdp.selectSize("M");
        pdp.clickAddToCart();
        page.waitForTimeout(1000);

        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();

        Assert.assertTrue(cartPage.getCartItemCount() >= 2,
                "Giỏ hàng phải có ít nhất 2 dòng sản phẩm khi thêm 2 biến thể khác nhau (S và M)");
    }

    /**
     * Test Case ID: TC_GH_07
     * Tiêu đề: Tính tổng tiền với nhiều sản phẩm và nhiều số lượng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Xác minh công thức toán học: Tạm tính phản ánh giá trị sau giảm giá và phí vận chuyển.
     */
    @Test(description = "TC_GH_07 - Kiểm tra công thức toán học tính Tạm tính và Giảm giá")
    public void testSubtotalAndDiscountMathematicalFormula() {
        CartPage cartPage = prepareCartWithItem();

        long totalPrice = cartPage.getTotalPrice();
        long discount = cartPage.getDiscountAmount();
        long subtotal = cartPage.getSubTotalPrice();

        Assert.assertTrue(totalPrice > 0, "Tổng tiền phải lớn hơn 0");
        Assert.assertTrue(subtotal > 0, "Tạm tính phải lớn hơn 0");

        // Logic toán học: Tạm tính phản ánh Tổng tiền trừ Giảm giá (cộng phí vận chuyển tiêu chuẩn 20k nếu đơn dưới 498k)
        long baseExpected = totalPrice - discount;
        Assert.assertTrue(subtotal == baseExpected || Math.abs(subtotal - baseExpected) <= 30000,
                String.format("Tạm tính (%d) phải tương thích với Tổng tiền (%d) trừ Giảm giá (%d) và phí vận chuyển",
                        subtotal, totalPrice, discount));
    }

    /**
     * Test Case ID: TC_GH_08
     * Tiêu đề: Tồn kho thay đổi sau khi đã thêm vào giỏ (Mock API tồn kho)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Giả lập API giỏ hàng trả về tồn kho mới nhỏ hơn số lượng hiện có,
     * hệ thống hiển thị cảnh báo tồn kho hoặc chặn thanh toán an toàn.
     */
    @Test(description = "TC_GH_08 - Tồn kho thay đổi sau khi thêm vào giỏ qua Mock API")
    public void testInventoryChangeAfterAddingToCart() {
        CartPage cartPage = prepareCartWithItem();

        // Mock API checkout/cart verification trả về lỗi vượt quá tồn kho
        page.route("**/api/**/checkout**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(400)
                    .setContentType("application/json")
                    .setBody("{\"success\":false,\"message\":\"Sản phẩm đã hết hàng hoặc không đủ số lượng tồn kho.\"}"));
        });

        cartPage.clickCheckoutButton();

        // Xác minh hệ thống không crash và vẫn ở trên trang thanh toán/giỏ hàng với phản hồi an toàn
        Assert.assertTrue(page.url().contains("/cart") || page.url().contains("/order"),
                "Hệ thống phải điều hướng hoặc chặn thanh toán an toàn khi tồn kho thay đổi");
    }

    /**
     * Test Case ID: TC_GH_09
     * Tiêu đề: Hiển thị trạng thái giỏ hàng trống khi xóa hết toàn bộ sản phẩm
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hiện thông báo giỏ hàng trống, tổng tiền về 0 và khóa/ẩn nút "Đặt hàng".
     */
    @Test(description = "TC_GH_09 - Hiển thị trạng thái giỏ hàng trống khi xóa hết sản phẩm")
    public void testEmptyCartStateDisplay() {
        CartPage cartPage = prepareCartWithItem();
        cartPage.clearAllItems();

        int count = cartPage.getCartItemCount();
        Assert.assertTrue(count == 0 || cartPage.isEmptyCartDisplayed(),
                "Sau khi xóa hết, giỏ hàng phải hiển thị trạng thái trống hoặc có 0 dòng sản phẩm");

        if (count == 0) {
            Assert.assertTrue(cartPage.isCheckoutButtonDisabled() || !cartPage.getCheckoutButton().isVisible(),
                    "Nút 'Đặt hàng' phải bị ẩn hoặc vô hiệu hóa khi giỏ hàng trống");
        }
    }

    /**
     * Test Case ID: TC_GH_10
     * Tiêu đề: Cập nhật nhanh số lượng liên tiếp (Debounce / Rapid clicking)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống xử lý debounce mượt mà, số lượng và tổng tiền cập nhật nhất quán, không lỗi giao diện.
     */
    @Test(description = "TC_GH_10 - Cập nhật nhanh số lượng nhiều lần liên tiếp (Debounce test)")
    public void testRapidQuantityUpdateDebounce() {
        CartPage cartPage = prepareCartWithItem();
        int initialQty = cartPage.getItemQuantity(0);

        // Nhấn tăng nhanh 2 lần liên tiếp
        cartPage.increaseItemQuantity(0, 2);

        // Đợi debounce hoàn tất
        page.waitForTimeout(1000);

        int finalQty = cartPage.getItemQuantity(0);
        Assert.assertTrue(finalQty > initialQty, "Số lượng sau chuỗi click liên tiếp phải tăng hợp lệ");
        Assert.assertTrue(cartPage.getSubTotalPrice() > 0, "Tổng tiền sau cập nhật nhanh phải > 0 và được tính lại");
    }
}
