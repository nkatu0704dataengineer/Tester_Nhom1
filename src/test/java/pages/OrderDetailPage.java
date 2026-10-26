package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * OrderDetailPage represents the single order breakdown view (/account/orders/{orderCode}).
 * Provides extraction of recipient details, product items, and numerical financial figures.
 */
public class OrderDetailPage {

    private final Page page;

    // Locators
    private final Locator orderHeaderCode;
    private final Locator orderStatus;
    private final Locator orderTime;

    private final Locator recipientName;
    private final Locator recipientPhone;
    private final Locator recipientAddress;

    private final Locator productName;
    private final Locator productVariant;
    private final Locator productQuantity;
    private final Locator productPrice;

    private final Locator subtotalPrice;
    private final Locator discountAmount;
    private final Locator shippingFee;
    private final Locator paymentMethod;
    private final Locator finalAmount;
    private final Locator savingNote;

    private final Locator backButton;
    private final Locator homeButton;

    public OrderDetailPage(Page page) {
        this.page = page;

        this.orderHeaderCode = page.locator("#order-code-title, h1:has-text('Đơn hàng #')").first();
        this.orderStatus = page.locator("#order-status, .status-tag").first();
        this.orderTime = page.locator("#order-time").first();

        this.recipientName = page.locator("#recipient-name").first();
        this.recipientPhone = page.locator("#recipient-phone").first();
        this.recipientAddress = page.locator("#recipient-address").first();

        this.productName = page.locator("#product-name").first();
        this.productVariant = page.locator("#product-variant").first();
        this.productQuantity = page.locator("#product-qty").first();
        this.productPrice = page.locator("#product-price").first();

        this.subtotalPrice = page.locator("#finance-subtotal").first();
        this.discountAmount = page.locator("#finance-discount").first();
        this.shippingFee = page.locator("#finance-shipping").first();
        this.paymentMethod = page.locator("#finance-payment").first();
        this.finalAmount = page.locator("#finance-final").first();
        this.savingNote = page.locator("#saving-note").first();

        this.backButton = page.locator("#btn-back-to-orders, a.btn-back, a:has-text('←')").first();
        this.homeButton = page.locator("#btn-go-home, a:has-text('Trang chủ')").first();
    }

    /**
     * Returns the order code displayed in the header title (e.g. "SO047342456" or "#SO047342456").
     */
    public String getOrderHeaderCode() {
        return orderHeaderCode.innerText().trim();
    }

    /**
     * Returns the current order status tag text (e.g. "Chờ xác nhận").
     */
    public String getOrderStatus() {
        return orderStatus.innerText().trim();
    }

    public String getRecipientName() {
        return recipientName.innerText().trim();
    }

    public String getRecipientPhone() {
        return recipientPhone.innerText().trim();
    }

    public String getRecipientAddress() {
        return recipientAddress.innerText().trim();
    }

    public String getFirstProductName() {
        return productName.innerText().trim();
    }

    public String getFirstProductVariant() {
        return productVariant.innerText().trim();
    }

    public String getFirstProductQuantity() {
        return productQuantity.innerText().trim();
    }

    /**
     * Parses numeric long value from currency text (e.g. "599.000đ" -> 599000L).
     */
    public long getTotalPrice() {
        return parseCurrencyToLong(subtotalPrice.innerText());
    }

    /**
     * Parses discount figure (e.g. "-200.000đ" or "200.000đ" -> 200000L).
     */
    public long getDiscountAmount() {
        return parseCurrencyToLong(discountAmount.innerText());
    }

    /**
     * Parses shipping fee (e.g. "20.000đ" or "0đ" -> 20000L).
     */
    public long getShippingFee() {
        String text = shippingFee.innerText();
        if (text.toLowerCase().contains("miễn phí")) {
            return 0L;
        }
        return parseCurrencyToLong(text);
    }

    /**
     * Parses final amount (e.g. "419.000đ" -> 419000L).
     */
    public long getFinalAmount() {
        return parseCurrencyToLong(finalAmount.innerText());
    }

    /**
     * Clicks back button to return to the Order History list.
     */
    public OrderHistoryPage clickBackToList() {
        backButton.click();
        return new OrderHistoryPage(page);
    }

    /**
     * Clicks "Trang chủ" button to return to homepage.
     */
    public void clickGoHome() {
        homeButton.click();
        page.waitForLoadState();
    }

    private long parseCurrencyToLong(String raw) {
        if (raw == null) return 0L;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0L;
        return Long.parseLong(digits);
    }

    // Getters for Assertions
    public Locator getOrderHeaderCodeLoc() { return orderHeaderCode; }
    public Locator getOrderStatusLoc() { return orderStatus; }
    public Locator getRecipientNameLoc() { return recipientName; }
    public Locator getRecipientPhoneLoc() { return recipientPhone; }
    public Locator getPaymentMethodLoc() { return paymentMethod; }
    public Locator getFinalAmountLoc() { return finalAmount; }
    public Locator getSavingNoteLoc() { return savingNote; }
    public Locator getBackButton() { return backButton; }
}
