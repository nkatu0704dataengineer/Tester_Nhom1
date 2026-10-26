package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CartPage represents the Shopping Cart screen (/cart) on Yody.vn.
 * Encapsulates cart items list, quantity updates, product removal, "Chọn tất cả" checkbox,
 * order summary (Tổng tiền, Giảm giá, Tạm tính, Tiết kiệm, Miễn phí vận chuyển),
 * and the "Đặt hàng" (Checkout) action.
 */
public class CartPage {
    private final Page page;

    // Locators - Header & Selection
    private final Locator pageHeading;
    private final Locator selectAllCheckbox;

    // Locators - Cart Items
    private final Locator cartItemRows;
    private final Locator emptyCartMessage;

    // Locators - Confirm Delete Modal
    private final Locator confirmDeleteModal;
    private final Locator confirmDeleteButton;

    // Locators - Order Summary
    private final Locator orderSummaryContainer;
    private final Locator totalPriceText;
    private final Locator discountAmountText;
    private final Locator subTotalPriceText;
    private final Locator savingsText;
    private final Locator freeShippingNotice;
    private final Locator checkoutButton;

    public CartPage(Page page) {
        this.page = page;

        // Header & Select All
        this.pageHeading = page.locator("div:has-text('Giỏ hàng')").first();
        this.selectAllCheckbox = page.locator("div:has-text('Chọn tất cả') button[role='checkbox']").first();

        // Cart Item Rows - exactly matched to cart item flex rows with quantity input and trash icon
        this.cartItemRows = page.locator("div.flex:has(input[inputmode='numeric']):has(svg path[d*='m14.74 9'])");
        this.emptyCartMessage = page.locator("text=Giỏ hàng hiện đang trống, text=Giỏ hàng của bạn đang trống, text=Chưa có sản phẩm nào trong giỏ hàng, text=Giỏ hàng trống, a:has-text('Mua sắm ngay')").first();

        // Confirm Delete Modal
        this.confirmDeleteModal = page.locator("div.fixed:has-text('Xoá'), div[role='dialog']:has-text('Xoá'), div.fixed:has-text('Xóa'), div[role='dialog']:has-text('Xóa')").last();
        this.confirmDeleteButton = confirmDeleteModal.locator("button").last();

        // Order Summary elements
        this.orderSummaryContainer = page.locator("div:has(> p:has-text('Chi tiết đơn hàng'))").first();
        this.totalPriceText = orderSummaryContainer.locator("div.flex:has(p:has-text('Tổng tiền')) p").last();
        this.discountAmountText = orderSummaryContainer.locator("div.flex:has(p:has-text('Giảm giá')) p").last();
        this.subTotalPriceText = orderSummaryContainer.locator("p.text-price-xl, div.flex:has(p:has-text('Tạm tính')) p.text-price-xl").first();
        this.savingsText = orderSummaryContainer.locator("p.text-price-sm:has-text('Tiết kiệm')").first();
        this.freeShippingNotice = page.locator("text=/miễn phí vận chuyển/i").first();
        this.checkoutButton = page.locator("button[aria-label='order'], button:has-text('Đặt hàng')").first();
    }

    // --- Navigation & Core Methods ---

    /**
     * Navigates directly to the cart page (/cart).
     */
    public void navigateTo() {
        page.navigate("https://yody.vn/cart");
        page.waitForLoadState();
        page.waitForTimeout(1000);
    }

    /**
     * Returns the number of distinct product item rows currently in the cart.
     */
    public int getCartItemCount() {
        page.waitForTimeout(500);
        return cartItemRows.count();
    }

    /**
     * Gets the product title of the item at the specified row index (0-based).
     */
    public String getItemTitle(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return "";
        Locator row = cartItemRows.nth(rowIndex);
        Locator titleElem = row.locator("p.text-body-sm, a[href*='/product'], p.font-medium").first();
        if (titleElem.count() > 0 && titleElem.isVisible()) {
            String text = titleElem.innerText().trim();
            if (!text.isEmpty()) return text;
        }
        String[] lines = row.innerText().split("\n");
        return lines.length > 0 ? lines[0].trim() : "";
    }

    /**
     * Gets the variant info (e.g. "Trắng 001, S") of the item at the specified row index.
     */
    public String getItemVariant(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return "";
        Locator row = cartItemRows.nth(rowIndex);
        Locator variantElem = row.locator("p:has-text(','), button:has-text(','), span:has-text(',')").first();
        if (variantElem.count() > 0 && variantElem.isVisible()) {
            return variantElem.innerText().trim();
        }
        return "";
    }

    /**
     * Gets the unit price of the item at the specified row index as a numeric long.
     */
    public long getItemUnitPrice(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return 0;
        Locator row = cartItemRows.nth(rowIndex);
        Locator priceElem = row.locator("p:has-text('đ'), span:has-text('đ')").last();
        return parsePrice(priceElem.innerText());
    }

    /**
     * Gets the current quantity of the item at the specified row index.
     */
    public int getItemQuantity(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return 1;
        Locator row = cartItemRows.nth(rowIndex);
        Locator input = row.locator("input[inputmode='numeric'], input").first();
        if (input.count() > 0 && input.isVisible()) {
            String val = input.inputValue();
            if (val == null || val.trim().isEmpty()) {
                val = input.getAttribute("value");
            }
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * Updates the quantity of the item at the specified row index to the target quantity.
     */
    public void updateItemQuantity(int rowIndex, int targetQty) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return;
        Locator row = cartItemRows.nth(rowIndex);
        Locator input = row.locator("input[inputmode='numeric'], input").first();
        if (input.count() > 0 && input.isVisible()) {
            input.fill(String.valueOf(targetQty));
            input.press("Enter");
            page.locator("body").click();
            page.waitForTimeout(800);
        }
    }

    /**
     * Increases quantity of item at rowIndex by clicking the plus (+) button.
     */
    public void increaseItemQuantity(int rowIndex, int times) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return;
        Locator row = cartItemRows.nth(rowIndex);
        Locator plusBtn = row.locator("svg:has(path[d*='M12 4.5v15']), svg path[d*='M12 4.5v15']").first();
        for (int i = 0; i < times; i++) {
            if (plusBtn.isVisible()) {
                plusBtn.click();
            } else {
                updateItemQuantity(rowIndex, getItemQuantity(rowIndex) + 1);
            }
            page.waitForTimeout(600);
        }
    }

    /**
     * Decreases quantity of item at rowIndex by clicking the minus (-) button.
     */
    public void decreaseItemQuantity(int rowIndex, int times) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return;
        Locator row = cartItemRows.nth(rowIndex);
        Locator minusBtn = row.locator("svg:has(path[d*='M5 12h14']), svg path[d*='M5 12h14']").first();
        for (int i = 0; i < times; i++) {
            if (minusBtn.isVisible()) {
                minusBtn.click();
            } else {
                int current = getItemQuantity(rowIndex);
                if (current > 1) {
                    updateItemQuantity(rowIndex, current - 1);
                }
            }
            page.waitForTimeout(600);
        }
    }

    /**
     * Removes the item at the specified row index by clicking its trash icon.
     */
    public void removeItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= cartItemRows.count()) return;
        Locator row = cartItemRows.nth(rowIndex);
        Locator trashIcon = row.locator("svg:has(path[d*='m14.74 9']), svg path[d*='m14.74 9']").first();
        trashIcon.click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(600);

        // Check if confirm delete dialog appears and click confirmation button
        Locator confirmModal = page.locator("div.fixed:has-text('Xoá'), div[role='dialog']:has-text('Xoá'), div.fixed:has-text('Xóa'), div[role='dialog']:has-text('Xóa')").last();
        if (confirmModal.isVisible()) {
            Locator confirmBtn = confirmModal.locator("button").last();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(1000);
            }
        }
    }

    /**
     * Removes all items from the cart until empty.
     */
    public void clearAllItems() {
        int safetyLimit = 15;
        while (cartItemRows.count() > 0 && safetyLimit-- > 0) {
            removeItem(0);
            page.waitForTimeout(800);
        }
    }

    // --- Order Summary Methods ---

    /**
     * Gets "Tổng tiền" (gross total before discounts) as a numeric long.
     */
    public long getTotalPrice() {
        if (totalPriceText.count() > 0 && totalPriceText.isVisible()) {
            return parsePrice(totalPriceText.innerText());
        }
        return 0;
    }

    /**
     * Gets "Giảm giá" (discount amount) as a numeric long.
     */
    public long getDiscountAmount() {
        if (discountAmountText.count() > 0 && discountAmountText.isVisible()) {
            return parsePrice(discountAmountText.innerText());
        }
        return 0;
    }

    /**
     * Gets "Tạm tính" (final order subtotal after discount) as a numeric long.
     */
    public long getSubTotalPrice() {
        if (subTotalPriceText.count() > 0 && subTotalPriceText.isVisible()) {
            return parsePrice(subTotalPriceText.innerText());
        }
        return 0;
    }

    /**
     * Gets "Tiết kiệm" amount as a numeric long.
     */
    public long getSavingsAmount() {
        if (savingsText.count() > 0 && savingsText.isVisible()) {
            return parsePrice(savingsText.innerText());
        }
        return 0;
    }

    /**
     * Checks if free shipping notice is displayed.
     */
    public boolean isFreeShippingNotified() {
        return freeShippingNotice.count() > 0 && freeShippingNotice.isVisible();
    }

    /**
     * Clicks "Đặt hàng" button to proceed to Checkout.
     */
    public void clickCheckoutButton() {
        checkoutButton.click();
        page.waitForTimeout(2000);
    }

    /**
     * Checks whether "Đặt hàng" button is disabled or absent.
     */
    public boolean isCheckoutButtonDisabled() {
        if (checkoutButton.count() == 0 || !checkoutButton.isVisible()) return true;
        return checkoutButton.getAttribute("disabled") != null || checkoutButton.isDisabled();
    }

    /**
     * Checks if empty cart message is currently displayed.
     */
    public boolean isEmptyCartDisplayed() {
        return emptyCartMessage.count() > 0 && emptyCartMessage.isVisible();
    }

    /**
     * Toggles the "Chọn tất cả" checkbox.
     */
    public void toggleSelectAll() {
        selectAllCheckbox.click();
        page.waitForTimeout(400);
    }

    /**
     * Checks if "Chọn tất cả" checkbox is currently checked.
     */
    public boolean isSelectAllChecked() {
        String state = selectAllCheckbox.getAttribute("data-state");
        return "checked".equalsIgnoreCase(state) || "true".equalsIgnoreCase(selectAllCheckbox.getAttribute("aria-checked"));
    }

    /**
     * Helper to parse currency text into numeric long.
     */
    public static long parsePrice(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) return 0;
        String cleaned = priceText.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- Locators Getters ---

    public Locator getCheckoutButton() {
        return checkoutButton;
    }

    public Locator getEmptyCartMessage() {
        return emptyCartMessage;
    }

    public Locator getCartItemRows() {
        return cartItemRows;
    }
}
