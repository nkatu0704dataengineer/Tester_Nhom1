package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SearchAndFilterPage represents the Product Search Results and Category Listing Page on Yody.vn.
 * Encapsulates search results, filter drawer (Category, Color, Size, Price), sorting, and pagination.
 */
public class SearchAndFilterPage {
    private final Page page;

    // Locators
    private final Locator pageTitle;
    private final Locator resultCountText;
    private final Locator emptyStateMessage;
    private final Locator productCards;
    private final Locator productTitles;
    private final Locator productPrices;

    // Filter controls
    private final Locator filterButton;
    private final Locator filterDrawer;
    private final Locator closeDrawerButton;
    private final Locator resetFilterButton;
    private final Locator applyFilterButton;

    // Sort & Pagination controls
    private final Locator sortPriceButton;
    private final Locator loadMoreButton;

    public SearchAndFilterPage(Page page) {
        this.page = page;

        this.pageTitle = page.locator("h1").first();
        this.resultCountText = page.locator("main").getByText(Pattern.compile("\\d+\\s*(sản phẩm|kết quả)", Pattern.CASE_INSENSITIVE)).first();
        this.emptyStateMessage = page.getByText("Không có sản phẩm nào phù hợp nhu cầu của bạn").first();

        // Product Cards & Details
        this.productCards = page.locator(".grid > div:has(h3), div.relative.space-y-2:has(h3), a[href*='-p']");
        this.productTitles = page.locator(".grid h3, [class*='grid'] h3");
        this.productPrices = page.locator(".grid p.text-red-600, [class*='grid'] p.text-red-600, .grid p:has-text('đ')");

        // Filter Drawer
        this.filterButton = page.locator("button[aria-label='open filter'], button:has-text('Lọc')").first();
        this.filterDrawer = page.locator(".drawer-content, [role='dialog']:has-text('Bộ lọc')").first();
        this.closeDrawerButton = page.locator("button[aria-label='Close drawer']").first();
        this.resetFilterButton = page.locator("button[aria-label='remove filter'], button:has-text('Xóa lọc')").first();
        this.applyFilterButton = page.locator("button[aria-label='apply filter'], button:has-text('Áp dụng')").first();

        // Sort & Load more
        this.sortPriceButton = page.locator("main").getByText("Giá").first();
        this.loadMoreButton = page.locator("button[aria-label='load more products'], button:has-text('Xem thêm')").first();
    }

    /**
     * Returns the total count of product cards displayed on the page.
     * Automatically waits for products to render if not in empty state.
     */
    public int getProductCount() {
        if (emptyStateMessage.isVisible()) {
            return 0;
        }
        try {
            productTitles.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
        } catch (Exception ignored) {
        }
        return productTitles.count();
    }

    /**
     * Returns list of product titles displayed on the current page.
     */
    public List<String> getProductTitles() {
        getProductCount();
        List<String> titles = new ArrayList<>();
        int count = productTitles.count();
        for (int i = 0; i < count; i++) {
            String title = productTitles.nth(i).innerText().trim();
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        return titles;
    }

    /**
     * Parses and returns the list of selling prices (integer in VND) from visible product cards.
     */
    public List<Integer> getProductPrices() {
        getProductCount();
        List<Integer> prices = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d[\\d.]*)\\s*đ");

        int cardCount = productCards.count();
        for (int i = 0; i < cardCount; i++) {
            Locator card = productCards.nth(i);
            String cardText = card.innerText();
            Matcher matcher = pattern.matcher(cardText);
            if (matcher.find()) {
                String raw = matcher.group(1).replace(".", "").replace(",", "").trim();
                try {
                    prices.add(Integer.parseInt(raw));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return prices;
    }

    /**
     * Opens the Filter Drawer if not already open.
     */
    public SearchAndFilterPage openFilterDrawer() {
        if (!filterDrawer.isVisible()) {
            filterButton.click();
            filterDrawer.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            page.waitForTimeout(600);
        }
        return this;
    }

    /**
     * Closes the Filter Drawer.
     */
    public SearchAndFilterPage closeFilterDrawer() {
        if (closeDrawerButton.isVisible()) {
            closeDrawerButton.click();
            page.waitForTimeout(600);
        }
        return this;
    }

    /**
     * Expands a specific collapsible filter section inside the drawer.
     *
     * @param sectionName Name of the section (e.g. "Màu sắc", "Size", "Giá", "Loại sản phẩm")
     */
    public SearchAndFilterPage expandSection(String sectionName) {
        openFilterDrawer();
        Locator sectionHeader = filterDrawer.locator("p:text-is('" + sectionName + "')").first();
        if (sectionHeader.isVisible()) {
            sectionHeader.click();
            page.waitForTimeout(600);
        }
        return this;
    }

    /**
     * Filters products by category / product type.
     *
     * @param categoryName Name of category (e.g. "Áo Polo", "Áo Len")
     */
    public SearchAndFilterPage filterByCategory(String categoryName) {
        expandSection("Loại sản phẩm");
        Locator option = filterDrawer.getByText(categoryName).first();
        option.click();
        applyFilterButton.click();
        page.waitForTimeout(600);
        page.waitForLoadState();
        return this;
    }

    /**
     * Filters products by color.
     *
     * @param colorName Name of color (e.g. "Đen", "Trắng", "Xanh")
     */
    public SearchAndFilterPage filterByColor(String colorName) {
        expandSection("Màu sắc");
        Locator option = filterDrawer.getByText(colorName).first();
        option.click();
        applyFilterButton.click();
        page.waitForTimeout(600);
        page.waitForLoadState();
        return this;
    }

    /**
     * Filters products by size.
     *
     * @param sizeName Size name (e.g. "S", "M", "L", "XL")
     */
    public SearchAndFilterPage filterBySize(String sizeName) {
        expandSection("Size");
        Locator option = filterDrawer.getByText(sizeName, new Locator.GetByTextOptions().setExact(true)).first();
        option.click();
        applyFilterButton.click();
        page.waitForTimeout(600);
        page.waitForLoadState();
        return this;
    }

    /**
     * Filters products by price range.
     * Selects available preset range in drawer, or updates price parameters safely.
     *
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     */
    public SearchAndFilterPage filterByPriceRange(int minPrice, int maxPrice) {
        String rangeText = "";
        if (minPrice == 0 && maxPrice == 99000) {
            rangeText = "Dưới 99.000";
        } else if (minPrice == 100000 && maxPrice == 199000) {
            rangeText = "100.000đ - 199.000đ";
        } else if (minPrice == 200000 && maxPrice == 299000) {
            rangeText = "200.000đ - 299.000đ";
        } else if (minPrice == 300000 && maxPrice == 499000) {
            rangeText = "300.000đ - 499.000đ";
        } else if (minPrice == 500000 && maxPrice == 1000000) {
            rangeText = "500.000đ - 1.000.000đ";
        } else if (minPrice == 1000000 && maxPrice == Integer.MAX_VALUE) {
            rangeText = "Trên 1.000.000";
        }

        if (!rangeText.isEmpty()) {
            expandSection("Giá");
            filterDrawer.getByText(rangeText).first().click();
            applyFilterButton.click();
            page.waitForTimeout(600);
            page.waitForLoadState();
        } else {
            // For custom/boundary price testing (min=max, min>max, negative) pass via URL params
            String baseUrl = page.url().replaceAll("[&?]price_from=[^&]*", "").replaceAll("[&?]price_to=[^&]*", "");
            String separator = baseUrl.contains("?") ? "&" : "?";
            page.navigate(baseUrl + separator + "price_from=" + minPrice + "&price_to=" + maxPrice);
            page.waitForLoadState();
        }
        return this;
    }

    /**
     * Clears / resets all currently applied filters.
     */
    public SearchAndFilterPage clearAllFilters() {
        openFilterDrawer();
        if (resetFilterButton.isVisible()) {
            resetFilterButton.click();
            page.waitForTimeout(400);
        }
        if (applyFilterButton.isVisible()) {
            applyFilterButton.click();
            page.waitForTimeout(600);
        }
        page.waitForLoadState();
        return this;
    }

    /**
     * Sorts products by price.
     *
     * @param isAscending true for lowest-to-highest (price_asc), false for highest-to-lowest (price_desc)
     */
    public SearchAndFilterPage sortByPrice(boolean isAscending) {
        String targetSort = isAscending ? "price_asc" : "price_desc";

        // Click sort price button up to 2 times until target sort is reached
        for (int i = 0; i < 2; i++) {
            if (page.url().contains(targetSort)) {
                break;
            }
            sortPriceButton.click();
            page.waitForTimeout(1000);
        }
        page.waitForLoadState();
        return this;
    }

    /**
     * Clicks "Xem thêm" (Load more) button to load more products.
     */
    public SearchAndFilterPage clickLoadMoreOrNextPage() {
        try {
            loadMoreButton.scrollIntoViewIfNeeded();
            loadMoreButton.click();
            page.waitForTimeout(2500);
        } catch (Exception ignored) {
        }
        return this;
    }

    // Getters
    public Locator getPageTitleLoc() {
        return pageTitle;
    }

    public Locator getResultCountLoc() {
        return resultCountText;
    }

    public Locator getEmptyStateLoc() {
        return emptyStateMessage;
    }

    public Locator getProductCardsLoc() {
        return productCards;
    }

    public Locator getProductTitlesLoc() {
        return productTitles;
    }

    public Locator getProductPricesLoc() {
        return productPrices;
    }

    public Locator getFilterBtnLoc() {
        return filterButton;
    }

    public Locator getFilterDrawerLoc() {
        return filterDrawer;
    }

    public Locator getResetFilterBtnLoc() {
        return resetFilterButton;
    }

    public Locator getApplyFilterBtnLoc() {
        return applyFilterButton;
    }

    public Locator getSortPriceBtnLoc() {
        return sortPriceButton;
    }

    public Locator getLoadMoreBtnLoc() {
        return loadMoreButton;
    }
}
