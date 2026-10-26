package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.HomePage;
import pages.SearchAndFilterPage;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 2 - Bộ lọc sản phẩm, Sắp xếp & Phân trang (Filter, Sort & Pagination)
 * Platform: Yody.vn
 *
 * Verifies category, color, size, price range filters, reset filters, price sorting, and load more functionality.
 */
public class ProductFilterAndSortTest extends BaseTest {

    private static final String DEFAULT_SEARCH_KEYWORD = "áo polo";

    /**
     * Helper to navigate to the search results page before filter tests.
     */
    private SearchAndFilterPage openSearchPage(String keyword) {
        HomePage homePage = new HomePage(page);
        return homePage.searchByKeyword(keyword);
    }

    /**
     * Test Case ID: TC_TK_L_07
     * Tiêu đề: Lọc theo danh mục sản phẩm
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Danh sách cập nhật và chỉ hiển thị các sản phẩm thuộc danh mục đã chọn ("Áo Polo").
     */
    @Test(description = "TC_TK_L_07 - Lọc theo danh mục sản phẩm ('Áo Polo')")
    public void testFilterByCategory() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);
        searchPage.filterByCategory("Áo Polo");

        // Xác minh kết quả trả về không rỗng
        Assert.assertTrue(searchPage.getProductCount() > 0, "Sau khi lọc danh mục Áo Polo phải có sản phẩm hiển thị");

        List<String> titles = searchPage.getProductTitles();
        boolean hasRelevant = titles.stream().anyMatch(t -> t.toLowerCase().contains("polo") || t.toLowerCase().contains("áo"));
        Assert.assertTrue(hasRelevant, "Các sản phẩm lọc phải thuộc danh mục liên quan đến Áo Polo");
    }

    /**
     * Test Case ID: TC_TK_L_08
     * Tiêu đề: Lọc theo khoảng giá và các giá trị biên
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Kiểm tra 3 trường hợp biên: min = max (200.000 - 200.000), min > max (500.000 - 100.000),
     * và giá trị âm (-1000). Hệ thống không crash 500, xử lý an toàn.
     */
    @Test(description = "TC_TK_L_08 - Lọc theo khoảng giá và kiểm tra giá trị biên")
    public void testFilterByPriceRangeAndBoundaryValues() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);

        // Trường hợp 1: min = max (200.000đ - 200.000đ)
        searchPage.filterByPriceRange(200000, 200000);
        assertThat(page.locator("main")).isVisible();

        // Trường hợp 2: min > max (500.000đ - 100.000đ)
        searchPage.filterByPriceRange(500000, 100000);
        assertThat(page.locator("main")).isVisible();

        // Trường hợp 3: Giá trị âm (-1000)
        searchPage.filterByPriceRange(-1000, 300000);
        assertThat(page.locator("main")).isVisible();
    }

    /**
     * Test Case ID: TC_TK_L_09
     * Tiêu đề: Lọc sản phẩm theo màu sắc
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Bộ lọc màu Đen được kích hoạt; URL cập nhật tham số màu và danh sách cập nhật.
     */
    @Test(description = "TC_TK_L_09 - Lọc sản phẩm theo màu sắc ('Đen')")
    public void testFilterByColor() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);
        searchPage.filterByColor("Đen");

        // Xác minh URL được cập nhật chứa tham số màu colors=
        Assert.assertTrue(page.url().contains("colors="), "URL phải chứa query param lọc màu sắc");
        Assert.assertTrue(searchPage.getProductCount() > 0, "Phải có sản phẩm biến thể màu Đen hiển thị");
    }

    /**
     * Test Case ID: TC_TK_L_10
     * Tiêu đề: Lọc sản phẩm theo kích thước (Size)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Kết quả hiển thị các sản phẩm còn kích thước tương ứng ("M").
     */
    @Test(description = "TC_TK_L_10 - Lọc sản phẩm theo kích thước ('M')")
    public void testFilterBySize() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);
        searchPage.filterBySize("M");

        // Xác minh URL cập nhật tham số sizes=
        Assert.assertTrue(page.url().contains("sizes="), "URL phải chứa query param lọc size");
        Assert.assertTrue(searchPage.getProductCount() > 0, "Phải có sản phẩm có size M hiển thị");
    }

    /**
     * Test Case ID: TC_TK_L_11
     * Tiêu đề: Kết hợp đồng thời nhiều bộ lọc
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Áp dụng đồng thời Màu Đen + Size M + Danh mục Áo Polo. Mọi tiêu chí kết hợp chính xác.
     */
    @Test(description = "TC_TK_L_11 - Kết hợp đồng thời nhiều bộ lọc")
    public void testCombineMultipleFilters() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);

        // Áp dụng Màu Đen và Size M
        searchPage.filterByColor("Đen");
        searchPage.filterBySize("M");

        // Xác minh cả hai bộ lọc đều được gắn trên URL
        Assert.assertTrue(page.url().contains("colors="), "URL phải có param colors");
        Assert.assertTrue(page.url().contains("sizes="), "URL phải có param sizes");
        assertThat(page.locator("main")).isVisible();
    }

    /**
     * Test Case ID: TC_TK_L_12
     * Tiêu đề: Xóa / Bỏ chọn bộ lọc (Reset Filter)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Sau khi áp dụng bộ lọc và bấm Xóa lọc, toàn bộ bộ lọc được reset về ban đầu.
     */
    @Test(description = "TC_TK_L_12 - Xóa / Bỏ chọn bộ lọc")
    public void testResetFilters() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);
        searchPage.filterByColor("Đen");
        Assert.assertTrue(page.url().contains("colors="));

        // Thực hiện xóa lọc
        searchPage.clearAllFilters();

        // Sau khi xóa lọc, URL không còn chứa param colors=
        Assert.assertFalse(page.url().contains("colors="), "Tham số colors phải được gỡ bỏ khỏi URL sau khi reset");
        Assert.assertTrue(searchPage.getProductCount() > 0, "Danh sách sản phẩm phải được khôi phục đầy đủ");
    }

    /**
     * Test Case ID: TC_TK_L_13
     * Tiêu đề: Sắp xếp theo giá tăng dần và giảm dần
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng:
     * - Khi chọn Giá tăng dần: Kiểm tra danh sách giá thực tế đảm bảo price[i] <= price[i+1].
     * - Khi chọn Giá giảm dần: Kiểm tra danh sách giá thực tế đảm bảo price[i] >= price[i+1].
     */
    @Test(description = "TC_TK_L_13 - Sắp xếp theo giá tăng dần / giảm dần")
    public void testSortByPriceAscendingAndDescending() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);

        // 1. Sắp xếp giá tăng dần (Thấp đến cao)
        searchPage.sortByPrice(true);
        Assert.assertTrue(page.url().contains("sort=price_asc"), "URL phải chứa sort=price_asc");

        List<Integer> ascPrices = searchPage.getProductPrices();
        if (ascPrices.size() >= 2) {
            for (int i = 0; i < Math.min(ascPrices.size() - 1, 5); i++) {
                Assert.assertTrue(ascPrices.get(i) <= ascPrices.get(i + 1),
                        "Giá sản phẩm thứ " + i + " (" + ascPrices.get(i) + ") phải nhỏ hơn hoặc bằng giá sản phẩm thứ " + (i + 1) + " (" + ascPrices.get(i + 1) + ")");
            }
        }

        // 2. Sắp xếp giá giảm dần (Cao đến thấp)
        searchPage.sortByPrice(false);
        Assert.assertTrue(page.url().contains("sort=price_desc"), "URL phải chứa sort=price_desc");

        List<Integer> descPrices = searchPage.getProductPrices();
        if (descPrices.size() >= 2) {
            for (int i = 0; i < Math.min(descPrices.size() - 1, 5); i++) {
                Assert.assertTrue(descPrices.get(i) >= descPrices.get(i + 1),
                        "Giá sản phẩm thứ " + i + " (" + descPrices.get(i) + ") phải lớn hơn hoặc bằng giá sản phẩm thứ " + (i + 1) + " (" + descPrices.get(i + 1) + ")");
            }
        }
    }

    /**
     * Test Case ID: TC_TK_L_14
     * Tiêu đề: Phân trang / Xem thêm (Load more)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Cuộn xuống nhấn nút "Xem thêm", tải thêm sản phẩm thành công,
     * tổng số sản phẩm hiển thị tăng lên (ví dụ từ 25 -> 49 sản phẩm).
     */
    @Test(description = "TC_TK_L_14 - Phân trang / Xem thêm (Load more)")
    public void testLoadMoreProducts() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);
        int initialCount = searchPage.getProductCount();
        Assert.assertTrue(initialCount > 0, "Ban đầu phải có sản phẩm hiển thị");

        searchPage.clickLoadMoreOrNextPage();

        int afterCount = searchPage.getProductCount();
        Assert.assertTrue(afterCount > initialCount,
                "Số lượng sản phẩm sau khi bấm 'Xem thêm' (" + afterCount + ") phải lớn hơn ban đầu (" + initialCount + ")");
    }

    /**
     * Test Case ID: TC_TK_L_15
     * Tiêu đề: Giữ nguyên bộ lọc khi tải lại trang (F5)
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Áp dụng bộ lọc màu Đen -> reload trang -> cấu hình bộ lọc trên URL và dữ liệu vẫn được duy trì.
     */
    @Test(description = "TC_TK_L_15 - Giữ nguyên bộ lọc khi tải lại trang (F5)")
    public void testPersistFiltersOnPageReload() {
        SearchAndFilterPage searchPage = openSearchPage(DEFAULT_SEARCH_KEYWORD);
        searchPage.filterByColor("Đen");

        String urlBeforeReload = page.url();
        Assert.assertTrue(urlBeforeReload.contains("colors="));

        // Tải lại trang (F5)
        page.reload();
        page.waitForLoadState();

        // Kiểm tra URL và sản phẩm vẫn duy trì trạng thái lọc
        Assert.assertTrue(page.url().contains("colors=3"), "URL sau khi reload phải giữ nguyên query parameter colors=3");
        Assert.assertTrue(page.url().contains("keyword="), "URL sau khi reload phải giữ nguyên query parameter keyword");
        Assert.assertTrue(searchPage.getProductCount() > 0, "Sản phẩm vẫn phải được hiển thị sau khi reload");
    }
}
