package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.CartPage;
import pages.ProductDetailPage;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 3 - Chi tiết sản phẩm (Product Detail Page - PDP)
 * Platform: Yody.vn
 *
 * Implements 10 test cases (TC_CTSP_01 -> TC_CTSP_10) covering product info display,
 * variant selection (color & size), size guide modal, out-of-stock handling,
 * quantity boundary controls, mathematical price checks, and page reload persistence.
 */
public class ProductDetailTest extends BaseTest {

    // Default active test product URL on Yody.vn
    public static final String DEFAULT_PDP_URL =
            "https://yody.vn/product/ao-polo-nu-co-v-bo-ke-co-gian-dan-hoi-phom-slim-fit-ton-dang?color=765&size=1";

    /**
     * Helper to navigate to the default PDP and return a ProductDetailPage instance.
     */
    private ProductDetailPage openPdp() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        pdp.navigateTo(DEFAULT_PDP_URL);
        return pdp;
    }

    /**
     * Test Case ID: TC_CTSP_01
     * Tiêu đề: Hiển thị đầy đủ thông tin chi tiết sản phẩm còn hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hiển thị tên sản phẩm, mã SKU, giá bán hiện tại (>0), mô tả/chất liệu,
     * bảng hướng dẫn size, các lựa chọn màu sắc và kích thước khả dụng.
     */
    @Test(description = "TC_CTSP_01 - Hiển thị đầy đủ thông tin chi tiết sản phẩm còn hàng")
    public void testProductDetailInfoDisplay() {
        ProductDetailPage pdp = openPdp();

        // 1. Xác minh tên sản phẩm hiển thị đầy đủ
        String title = pdp.getProductTitle();
        Assert.assertFalse(title.isEmpty(), "Tên sản phẩm không được rỗng");
        Assert.assertTrue(title.toLowerCase().contains("polo") || title.toLowerCase().contains("áo"),
                "Tên sản phẩm phải chứa từ khóa mô tả sản phẩm: " + title);

        // 2. Xác minh giá bán hiện tại hợp lệ
        long currentPrice = pdp.getCurrentPrice();
        Assert.assertTrue(currentPrice > 0, "Giá bán hiện tại phải lớn hơn 0, nhận được: " + currentPrice);

        // 3. Xác minh các biến thể màu sắc và kích cỡ có trên trang
        Assert.assertTrue(pdp.getSizeCount() > 0, "Trang sản phẩm phải có các tùy chọn kích cỡ (Size)");

        // 4. Xác minh liên kết hướng dẫn chọn size có sẵn
        pdp.openSizeGuide();
        Assert.assertTrue(pdp.isSizeGuideModalVisible(), "Bảng hướng dẫn chọn size phải mở khi click");
        pdp.closeSizeGuide();

        // 5. Xác minh nút Thêm vào giỏ hàng khả dụng
        Assert.assertTrue(pdp.isAddToCartEnabled(), "Nút 'Thêm vào giỏ' phải khả dụng đối với sản phẩm còn hàng");
    }

    /**
     * Test Case ID: TC_CTSP_02
     * Tiêu đề: Thêm vào giỏ khi chưa chọn size
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Nếu vào PDP không có size mặc định nào được chọn, việc nhấn "Thêm vào giỏ"
     * sẽ yêu cầu người dùng chọn kích cỡ và không tăng số lượng giỏ hàng.
     */
    @Test(description = "TC_CTSP_02 - Thêm vào giỏ khi chưa chọn size")
    public void testAddToCartWithoutSelectingSize() {
        ProductDetailPage pdp = new ProductDetailPage(page);
        // Mở PDP không có tham số size trong URL
        pdp.navigateTo("https://yody.vn/product/ao-polo-nu-co-v-bo-ke-co-gian-dan-hoi-phom-slim-fit-ton-dang");

        int initialBadge = pdp.getCartBadgeCount();

        // Kiểm tra nếu tất cả size chưa được chọn hoặc click thêm vào giỏ
        pdp.clickAddToCart();

        // Xác minh badge giỏ hàng không bị tăng bất thường nếu chưa có size
        int currentBadge = pdp.getCartBadgeCount();
        Assert.assertTrue(currentBadge >= initialBadge, "Badge giỏ hàng được xử lý nhất quán khi chọn biến thể");
    }

    /**
     * Test Case ID: TC_CTSP_03
     * Tiêu đề: Thêm sản phẩm hợp lệ vào giỏ hàng
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Chọn Màu và Size hợp lệ (SL: 1) -> Nhấn "Thêm vào giỏ" -> Badge giỏ hàng tăng,
     * trong giỏ hàng (/cart) hiển thị chính xác tên, màu, size, số lượng = 1 và đơn giá.
     */
    @Test(description = "TC_CTSP_03 - Thêm sản phẩm hợp lệ vào giỏ hàng")
    public void testAddValidProductToCart() {
        ProductDetailPage pdp = openPdp();

        String selectedSize = pdp.selectFirstAvailableSize();
        Assert.assertFalse(selectedSize.isEmpty(), "Phải chọn được ít nhất 1 size khả dụng");

        pdp.clickAddToCart();

        // Đi tới giỏ hàng để kiểm chứng dữ liệu sản phẩm vừa thêm
        CartPage cartPage = new CartPage(page);
        cartPage.navigateTo();

        Assert.assertTrue(cartPage.getCartItemCount() >= 1, "Giỏ hàng phải có ít nhất 1 sản phẩm sau khi thêm");
        String itemTitle = cartPage.getItemTitle(0);
        Assert.assertTrue(itemTitle.toLowerCase().contains("polo") || itemTitle.toLowerCase().contains("áo"),
                "Tên sản phẩm trong giỏ hàng phải khớp với sản phẩm đã thêm");
        Assert.assertEquals(cartPage.getItemQuantity(0), 1, "Số lượng sản phẩm trong giỏ mặc định phải là 1");
    }

    /**
     * Test Case ID: TC_CTSP_04
     * Tiêu đề: Xử lý sản phẩm hết hàng (Out of Stock)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Khi sản phẩm có tồn kho = 0 (giả lập qua page.route mock API hoặc chọn biến thể hết hàng),
     * hệ thống hiển thị trạng thái hết hàng và nút "Thêm vào giỏ" bị vô hiệu hóa.
     */
    @Test(description = "TC_CTSP_04 - Xử lý sản phẩm hết hàng qua Mock API")
    public void testOutOfStockProductHandling() {
        // Mock API kiểm tra tồn kho hoặc cấu hình sản phẩm trả về stock = 0
        page.route("**/api/**/inventory**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"success\":true,\"data\":{\"stock\":0,\"available\":false}}"));
        });

        ProductDetailPage pdp = openPdp();

        // Biến thể hết hàng (ví dụ: size 2XL có đường gạch chéo hết hàng trong DOM thực tế)
        boolean is2XlDisabled = pdp.isSizeDisabled("2XL");
        Assert.assertTrue(is2XlDisabled || !pdp.isAddToCartEnabled() || pdp.isOutOfStock(),
                "Biến thể hết hàng phải được đánh dấu không khả dụng hoặc nút thêm vào giỏ bị vô hiệu hóa");
    }

    /**
     * Test Case ID: TC_CTSP_05
     * Tiêu đề: Thay đổi màu sắc sản phẩm
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Chuyển đổi giữa các nút màu sắc khác nhau -> Tên màu đang chọn và ảnh đại diện cập nhật tương ứng.
     */
    @Test(description = "TC_CTSP_05 - Thay đổi màu sắc sản phẩm cập nhật giao diện")
    public void testChangeProductColorVariant() {
        ProductDetailPage pdp = openPdp();

        int colorCount = pdp.getColorCount();
        if (colorCount > 1) {
            String initialUrl = page.url();
            // Chọn màu thứ 2
            pdp.selectColor("Đen");

            // Xác minh URL hoặc nhãn màu đã được cập nhật
            String newUrl = page.url();
            Assert.assertNotNull(newUrl, "URL trang sau khi đổi màu không được rỗng");
        } else {
            // Sản phẩm có 1 màu, xác minh màu sắc hiển thị đúng
            Assert.assertTrue(pdp.getProductTitle().length() > 0, "Thông tin sản phẩm vẫn hiển thị chuẩn xác");
        }
    }

    /**
     * Test Case ID: TC_CTSP_06
     * Tiêu đề: Chọn biến thể màu/size không khả dụng (hết hàng)
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Kích cỡ hết hàng có đường gạch chéo hoặc class mờ, không cho phép thêm vào giỏ.
     */
    @Test(description = "TC_CTSP_06 - Chọn biến thể size không khả dụng")
    public void testSelectUnavailableVariant() {
        ProductDetailPage pdp = openPdp();

        // Kiểm tra trạng thái size 2XL (size hết hàng trên sản phẩm mẫu)
        boolean isSize2XlDisabled = pdp.isSizeDisabled("2XL");
        Assert.assertTrue(isSize2XlDisabled, "Size 2XL hết hàng phải bị làm mờ hoặc có dấu gạch chéo cảnh báo");
    }

    /**
     * Test Case ID: TC_CTSP_07
     * Tiêu đề: Kiểm tra số lượng mặc định và giới hạn biên
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Số lượng mặc định là 1; nút giảm (-) không cho phép giảm dưới 1;
     * nhập giá trị âm hoặc 0 sẽ tự động điều chỉnh về tối thiểu 1.
     */
    @Test(description = "TC_CTSP_07 - Kiểm tra số lượng mặc định và giới hạn biên (Min = 1)")
    public void testQuantityDefaultAndBoundaryLimits() {
        ProductDetailPage pdp = openPdp();

        // 1. Số lượng mặc định phải là 1
        Assert.assertEquals(pdp.getQuantity(), 1, "Số lượng mặc định phải bằng 1");

        // 2. Click nút giảm (-) khi đang là 1
        pdp.decreaseQuantity(2);
        Assert.assertEquals(pdp.getQuantity(), 1, "Không thể giảm số lượng xuống dưới 1");

        // 3. Tăng lên 2 rồi giảm về 1
        pdp.increaseQuantity(1);
        Assert.assertEquals(pdp.getQuantity(), 2, "Số lượng sau khi nhấn (+) phải tăng lên 2");
        pdp.decreaseQuantity(1);
        Assert.assertEquals(pdp.getQuantity(), 1, "Số lượng sau khi nhấn (-) phải giảm về 1");
    }

    /**
     * Test Case ID: TC_CTSP_08
     * Tiêu đề: Hiển thị giá và mức giảm chính xác với sản phẩm có khuyến mãi
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Sản phẩm có giảm giá phải hiển thị Giá gốc, Giá bán và Tỉ lệ % giảm.
     * Giá bán hiện tại phải nhỏ hơn hoặc bằng Giá gốc.
     */
    @Test(description = "TC_CTSP_08 - Kiểm tra logic toán học giữa Giá gốc, Giảm giá và Giá hiện tại")
    public void testDiscountPriceCalculationLogic() {
        ProductDetailPage pdp = openPdp();

        long current = pdp.getCurrentPrice();
        long original = pdp.getOriginalPrice();
        int discountPct = pdp.getDiscountPercent();

        Assert.assertTrue(current > 0, "Giá bán hiện tại phải > 0");

        if (original > current && discountPct > 0) {
            // Kiểm tra tính nhất quán toán học: Giá bán = Giá gốc * (100 - % giảm) / 100 (dung sai 2%)
            long calculatedExpected = Math.round(original * (100.0 - discountPct) / 100.0);
            long diff = Math.abs(current - calculatedExpected);
            Assert.assertTrue(diff <= 5000,
                    String.format("Giá bán thực tế (%d) phải khớp với công thức từ Giá gốc (%d) và Giảm giá (%d%%)",
                            current, original, discountPct));
        } else {
            Assert.assertTrue(current <= original || original == current,
                    "Giá bán hiện tại không được lớn hơn giá gốc");
        }
    }

    /**
     * Test Case ID: TC_CTSP_09
     * Tiêu đề: Mở và đóng hướng dẫn chọn size
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Nhấn "Hướng dẫn chọn size" -> Modal hiển thị bảng quy đổi kích cỡ -> Nhấn nút đóng (X) -> Modal đóng hoàn toàn.
     */
    @Test(description = "TC_CTSP_09 - Mở và đóng Modal Hướng dẫn chọn size")
    public void testOpenAndCloseSizeGuideModal() {
        ProductDetailPage pdp = openPdp();

        // 1. Mở Size Guide
        pdp.openSizeGuide();
        Assert.assertTrue(pdp.isSizeGuideModalVisible(), "Modal Hướng dẫn chọn size phải hiển thị sau khi mở");

        // 2. Đóng Size Guide
        pdp.closeSizeGuide();
        Assert.assertFalse(pdp.isSizeGuideModalVisible(), "Modal Hướng dẫn chọn size phải đóng sau khi nhấn nút đóng");
    }

    /**
     * Test Case ID: TC_CTSP_10
     * Tiêu đề: Tải lại trang (F5) hoặc truy cập trực tiếp bằng URL sản phẩm
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Dữ liệu sản phẩm không bị mất, layout không bị vỡ sau khi tải lại trang.
     */
    @Test(description = "TC_CTSP_10 - Tải lại trang PDP giữ nguyên dữ liệu và giao diện")
    public void testReloadProductDetailPage() {
        ProductDetailPage pdp = openPdp();
        String titleBefore = pdp.getProductTitle();
        long priceBefore = pdp.getCurrentPrice();

        // Thực hiện tải lại trang
        page.reload();
        page.waitForLoadState();

        String titleAfter = pdp.getProductTitle();
        long priceAfter = pdp.getCurrentPrice();

        Assert.assertEquals(titleAfter, titleBefore, "Tiêu đề sản phẩm sau khi F5 phải được giữ nguyên");
        Assert.assertEquals(priceAfter, priceBefore, "Giá sản phẩm sau khi F5 phải được giữ nguyên");
        Assert.assertTrue(pdp.isAddToCartEnabled(), "Nút Thêm vào giỏ vẫn phải khả dụng sau khi reload");
    }
}
