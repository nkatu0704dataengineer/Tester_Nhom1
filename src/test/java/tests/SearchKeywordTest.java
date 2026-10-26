package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.HomePage;
import pages.SearchAndFilterPage;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 2 - Tìm kiếm sản phẩm theo từ khóa (Keyword Search)
 * Platform: Yody.vn
 *
 * Verifies search functionality across valid, unaccented, non-existent, empty, special characters, and long strings.
 */
public class SearchKeywordTest extends BaseTest {

    /**
     * Test Case ID: TC_TK_L_01
     * Tiêu đề: Tìm kiếm từ khóa hợp lệ
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Trang kết quả hiển thị danh sách sản phẩm liên quan đến "áo polo";
     * số lượng kết quả > 0; tiêu đề các sản phẩm trả về có chứa từ khóa liên quan.
     */
    @Test(description = "TC_TK_L_01 - Tìm kiếm từ khóa hợp lệ ('áo polo')")
    public void testSearchValidKeyword() {
        HomePage homePage = new HomePage(page);
        SearchAndFilterPage searchPage = homePage.searchByKeyword("áo polo");

        // Xác minh tiêu đề hoặc trang kết quả hiển thị từ khóa tìm kiếm
        assertThat(searchPage.getPageTitleLoc()).containsText("áo polo");

        // Xác minh số lượng sản phẩm tìm thấy > 0
        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Số lượng sản phẩm tìm kiếm với 'áo polo' phải lớn hơn 0");

        // Xác minh danh sách tiêu đề có chứa từ khóa liên quan
        List<String> titles = searchPage.getProductTitles();
        boolean hasRelevantProduct = titles.stream()
                .anyMatch(t -> t.toLowerCase().contains("polo") || t.toLowerCase().contains("áo"));
        Assert.assertTrue(hasRelevantProduct, "Danh sách sản phẩm phải chứa sản phẩm liên quan đến 'áo polo'");
    }

    /**
     * Test Case ID: TC_TK_L_02
     * Tiêu đề: Tìm kiếm từ khóa không dấu
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống xử lý tốt tiếng Việt không dấu ("ao thun"),
     * trả về danh sách sản phẩm liên quan với số lượng > 0.
     */
    @Test(description = "TC_TK_L_02 - Tìm kiếm từ khóa không dấu ('ao thun')")
    public void testSearchUnaccentedKeyword() {
        HomePage homePage = new HomePage(page);
        SearchAndFilterPage searchPage = homePage.searchByKeyword("ao thun");

        // Xác minh số lượng sản phẩm trả về > 0
        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Tìm kiếm không dấu 'ao thun' phải trả về sản phẩm tương ứng");

        List<String> titles = searchPage.getProductTitles();
        boolean hasRelevantProduct = titles.stream()
                .anyMatch(t -> t.toLowerCase().contains("thun") || t.toLowerCase().contains("áo"));
        Assert.assertTrue(hasRelevantProduct, "Sản phẩm tìm thấy phải liên quan đến áo thun");
    }

    /**
     * Test Case ID: TC_TK_L_03
     * Tiêu đề: Tìm kiếm từ khóa không tồn tại
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hiển thị thông báo không tìm thấy sản phẩm phù hợp;
     * danh sách sản phẩm rỗng (count = 0); trang không bị lỗi crash.
     */
    @Test(description = "TC_TK_L_03 - Tìm kiếm từ khóa không tồn tại ('xyzabc123')")
    public void testSearchNonExistentKeyword() {
        HomePage homePage = new HomePage(page);
        SearchAndFilterPage searchPage = homePage.searchByKeyword("xyzabc123");

        // Xác minh thông báo không có sản phẩm phù hợp được hiển thị
        assertThat(searchPage.getEmptyStateLoc()).isVisible();

        // Danh sách thẻ sản phẩm phải rỗng
        Assert.assertEquals(searchPage.getProductCount(), 0, "Không được hiển thị thẻ sản phẩm khi từ khóa không tồn tại");
    }

    /**
     * Test Case ID: TC_TK_L_04
     * Tiêu đề: Tìm kiếm với ô trống
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Để trống ô tìm kiếm và nhấn Enter; hệ thống không phát sinh lỗi,
     * duy trì trang hiện tại hoặc giữ nguyên trạng thái hợp lệ.
     */
    @Test(description = "TC_TK_L_04 - Tìm kiếm với ô trống")
    public void testSearchWithEmptyKeyword() {
        HomePage homePage = new HomePage(page);
        homePage.searchByKeyword("");

        // Xác minh hệ thống không bị lỗi 500 hay crash trang
        Assert.assertTrue(page.url().contains("yody.vn"), "Hệ thống phải duy trì trạng thái an toàn trên domain Yody");
    }

    /**
     * Test Case ID: TC_TK_L_05
     * Tiêu đề: Từ khóa chứa ký tự đặc biệt
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Hệ thống escape ký tự an toàn, không xảy ra lỗi 500 hay trang trắng,
     * xử lý an toàn và không bị inject lỗi.
     */
    @Test(description = "TC_TK_L_05 - Từ khóa chứa ký tự đặc biệt ('@#$%^&*()')")
    public void testSearchSpecialCharacters() {
        HomePage homePage = new HomePage(page);
        SearchAndFilterPage searchPage = homePage.searchByKeyword("@#$%^&*()");

        // Xác minh trang phản hồi an toàn, URL được escape và có nội dung hiển thị
        Assert.assertTrue(page.url().contains("search"), "URL tìm kiếm phải được định tuyến an toàn");
        assertThat(page.locator("main")).isVisible();
    }

    /**
     * Test Case ID: TC_TK_L_06
     * Tiêu đề: Từ khóa rất dài vượt giới hạn
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Nhập chuỗi 256 ký tự 'a'; hệ thống không bị treo, giao diện không vỡ,
     * phản hồi an toàn không có kết quả hoặc cắt chuỗi hợp lý.
     */
    @Test(description = "TC_TK_L_06 - Từ khóa rất dài vượt giới hạn (256 ký tự)")
    public void testSearchLongKeyword() {
        HomePage homePage = new HomePage(page);
        String longKeyword = "a".repeat(256);
        SearchAndFilterPage searchPage = homePage.searchByKeyword(longKeyword);

        // Xác minh hệ thống không bị crash, trang render bình thường
        assertThat(page.locator("main")).isVisible();
        Assert.assertEquals(searchPage.getProductCount(), 0, "Từ khóa 256 ký tự rác không được trả về sản phẩm hợp lệ");
    }
}
