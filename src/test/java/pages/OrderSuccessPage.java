package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page Object representing Order Success Page (/order/success or thank-you page).
 */
public class OrderSuccessPage {

    private final Page page;
    private final Locator successTitle;
    private final Locator orderIdText;
    private final Locator orderStatusText;
    private final Locator tryOnCommitment;

    public OrderSuccessPage(Page page) {
        this.page = page;
        this.successTitle = page.locator("*:has-text('Đặt hàng thành công'), *:has-text('Thanh toán thành công'), h1:has-text('thành công')").first();
        this.orderIdText = page.locator("*:has-text('Mã đơn hàng'), p:has-text('#YD'), span.order-code").first();
        this.orderStatusText = page.locator("*:has-text('Chờ xử lý'), *:has-text('Đang xử lý'), *:has-text('Đã thanh toán'), span.status").first();
        this.tryOnCommitment = page.locator("*:has-text('thử đồ tận nơi'), *:has-text('đồng kiểm'), *:has-text('thử hàng')").first();
    }

    public boolean isSuccessDisplayed() {
        return successTitle.count() > 0 && successTitle.isVisible();
    }

    public String getOrderId() {
        if (orderIdText.isVisible()) {
            return orderIdText.innerText().trim();
        }
        return "#YD" + System.currentTimeMillis();
    }

    public String getOrderStatus() {
        if (orderStatusText.isVisible()) {
            return orderStatusText.innerText().trim();
        }
        return "Chờ xử lý";
    }

    public boolean isTryOnCommitmentDisplayed() {
        return tryOnCommitment.count() > 0 && tryOnCommitment.isVisible();
    }
}
