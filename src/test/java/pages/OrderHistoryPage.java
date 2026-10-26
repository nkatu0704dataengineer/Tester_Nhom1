package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * OrderHistoryPage represents the user's order history screen (/account/orders).
 * Provides tab filtering by status and access to individual order details.
 */
public class OrderHistoryPage {

    private final Page page;

    // Locators
    private final Locator titleHeading;
    private final Locator backButton;
    private final Locator statusTabs;
    private final Locator orderCards;
    private final Locator emptyStateNotice;

    public OrderHistoryPage(Page page) {
        this.page = page;

        this.titleHeading = page.locator("h1:has-text('Lịch sử mua hàng')").first();
        this.backButton = page.locator("#btn-back, a.btn-back, button:has-text('←'), a:has-text('←')").first();
        this.statusTabs = page.locator("#status-tabs button, .tabs-nav button");
        this.orderCards = page.locator(".order-card:visible");
        this.emptyStateNotice = page.locator("#empty-order-state, #empty-state-title, :has-text('Bạn chưa có đơn hàng nào')").first();
    }

    /**
     * Navigates directly to the Order History URL.
     */
    public OrderHistoryPage navigateToOrderHistory() {
        page.navigate("https://yody.vn/account/orders");
        page.waitForLoadState();
        return this;
    }

    /**
     * Filters order cards by clicking on a status tab (e.g. "Tất cả", "Chờ xác nhận", "Đang giao hàng").
     */
    public OrderHistoryPage filterByStatusTab(String tabName) {
        Locator tab = page.locator(String.format("button.tab-btn:has-text('%s')", tabName)).first();
        tab.click();
        page.waitForTimeout(300);
        return this;
    }

    /**
     * Returns the count of visible order cards currently listed.
     */
    public int getOrderCardCount() {
        return orderCards.count();
    }

    /**
     * Returns the order code of the first visible card in the list (e.g. "#SO047342456").
     */
    public String getFirstOrderCode() {
        return orderCards.first().locator(".order-code").innerText().trim();
    }

    /**
     * Returns the order status label of the first visible card in the list.
     */
    public String getFirstOrderStatus() {
        return orderCards.first().locator(".status-tag").innerText().trim();
    }

    /**
     * Returns the total amount string of the first visible card in the list (e.g. "419.000đ").
     */
    public String getFirstOrderTotalAmount() {
        return orderCards.first().locator(".total-val").innerText().trim();
    }

    /**
     * Clicks "Xem chi tiết" on the first order card.
     */
    public OrderDetailPage clickFirstOrderDetail() {
        orderCards.first().locator(".btn-view-detail").click();
        return new OrderDetailPage(page);
    }

    /**
     * Clicks "Xem chi tiết" for an order matching the specified order code.
     */
    public OrderDetailPage clickOrderToViewDetail(String orderCode) {
        Locator card = page.locator(String.format(".order-card:has(.order-code:has-text('%s'))", orderCode)).first();
        card.locator(".btn-view-detail").click();
        return new OrderDetailPage(page);
    }

    /**
     * Clicks back button to return to the Account Profile screen.
     */
    public AccountProfilePage clickBackToAccount() {
        backButton.click();
        return new AccountProfilePage(page);
    }

    // Getters for Assertions
    public Locator getTitleHeading() { return titleHeading; }
    public Locator getBackButton() { return backButton; }
    public Locator getEmptyStateNotice() { return emptyStateNotice; }
    public Locator getOrderCards() { return orderCards; }
    public Locator getFirstOrderCard() { return orderCards.first(); }
}
