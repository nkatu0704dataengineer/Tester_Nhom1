package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ProductDetailPage (PDP) represents the Single Product Details Page on Yody.vn.
 * Encapsulates title, SKU, prices, discount, image gallery, color & size variants,
 * size guide modal, quantity controls, Add to Cart, header cart badge, and description.
 */
public class ProductDetailPage {
    private final Page page;

    // Locators - Product Info & Header
    private final Locator productTitle;
    private final Locator skuText;
    private final Locator currentPrice;
    private final Locator originalPrice;
    private final Locator discountPercent;
    private final Locator headerCartBadge;

    // Locators - Gallery
    private final Locator mainImage;
    private final Locator thumbnailImages;

    // Locators - Variants
    private final Locator colorSection;
    private final Locator selectedColorLabel;
    private final Locator colorButtons;
    private final Locator sizeButtons;

    // Locators - Size Guide
    private final Locator sizeGuideLink;
    private final Locator sizeGuideModal;
    private final Locator sizeGuideCloseButton;

    // Locators - Quantity & Actions
    private final Locator quantityInput;
    private final Locator decreaseQuantityButton;
    private final Locator increaseQuantityButton;
    private final Locator addToCartButton;
    private final Locator outOfStockIndicator;
    private final Locator addedToCartToast;

    // Locators - Description
    private final Locator productDescription;

    public ProductDetailPage(Page page) {
        this.page = page;

        // Info & Header
        this.productTitle = page.locator("h1").first();
        this.skuText = page.locator("*:has-text('WCPO'), *:has-text('SKU'), [class*='sku']").first();
        this.currentPrice = page.locator("p.text-red-600, p[class*='text-red-600'], p.text-price-lg").first();
        this.originalPrice = page.locator("p.line-through, [class*='line-through']").first();
        this.discountPercent = page.locator("div.bg-red-600, [class*='bg-red-600'], span:has-text('%')").first();
        this.headerCartBadge = page.locator("a[aria-label='cart'] div.rounded-full, header a[href*='/cart'] .rounded-full").first();

        // Gallery
        this.mainImage = page.locator(".aspect-\\[3\\/4\\] img, img[alt*='Polo'], img[alt*='Áo']").first();
        this.thumbnailImages = page.locator(".aspect-\\[3\\/4\\] img, img[loading='lazy']");

        // Variants
        this.colorSection = page.locator("text=Màu sắc").first();
        this.selectedColorLabel = page.locator("p:has-text('Màu sắc:'), span:has-text('Màu sắc:'), div:has-text('Màu sắc:')").first();
        this.colorButtons = page.locator("button:has(img), [class*='color'] button, button[aria-label*='màu' i]");
        this.sizeButtons = page.locator("button:has(p.text-label-sm), button:has-text('S'), button:has-text('M'), button:has-text('L'), button:has-text('XL'), button:has-text('2XL')");

        // Size Guide
        this.sizeGuideLink = page.locator("p.text-brandsecondary-text, p:has-text('size')").first();
        this.sizeGuideModal = page.locator("div.fixed:has-text('Bảng kích thước'), div:has-text('Bảng kích thước')").last();
        this.sizeGuideCloseButton = sizeGuideModal.locator("button[aria-label*='close' i], button:has(svg), button").first();

        // Quantity & Purchase
        this.quantityInput = page.locator("input[type='number']").first();
        this.decreaseQuantityButton = page.locator("div:has(> input[type='number']) svg path[d*='M5 12h14']").locator("xpath=ancestor::span[1]").first();
        this.increaseQuantityButton = page.locator("div:has(> input[type='number']) svg path[d*='M12 4.5v15']").locator("xpath=ancestor::span[1]").first();
        this.addToCartButton = page.locator("button:has-text('Thêm vào giỏ'), button[aria-label='Add to cart']").first();
        this.outOfStockIndicator = page.locator("text=Hết hàng, button:has-text('Hết hàng'), text=Tạm hết hàng, button:disabled:has-text('Thêm vào giỏ')").first();
        this.addedToCartToast = page.locator("div:has-text('Đã thêm vào giỏ hàng')").first();

        // Description
        this.productDescription = page.locator("text=Chi tiết sản phẩm, text=Chất liệu, div:has-text('Ưu điểm')").first();
    }

    // --- Action Methods ---

    /**
     * Navigates directly to the given product PDP URL.
     */
    public void navigateTo(String pdpUrl) {
        page.navigate(pdpUrl);
        page.waitForLoadState();
        productTitle.waitFor();
    }

    /**
     * Selects a color variant by its visible name or partial alt text.
     */
    public void selectColor(String colorName) {
        Locator targetColor = colorButtons.filter(new Locator.FilterOptions().setHas(page.locator("img[alt*='" + colorName + "']")));
        if (targetColor.count() > 0) {
            targetColor.first().click();
        } else {
            // Fallback to text matching
            Locator textMatch = page.locator("button:has-text('" + colorName + "')");
            if (textMatch.count() > 0) {
                textMatch.first().click();
            } else if (colorButtons.count() > 0) {
                colorButtons.first().click();
            }
        }
        page.waitForTimeout(300);
    }

    /**
     * Selects a size variant (S, M, L, XL, 2XL...) if available.
     */
    public void selectSize(String sizeName) {
        Locator targetSize = sizeButtons.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + Pattern.quote(sizeName) + "\\s*$")));
        if (targetSize.count() > 0) {
            targetSize.first().click();
        } else {
            Locator fallback = page.locator("button:has(p:has-text('" + sizeName + "'))").first();
            if (fallback.count() > 0) {
                fallback.click();
            }
        }
        page.waitForTimeout(300);
    }

    /**
     * Selects the first enabled (in-stock) size variant available on the page.
     */
    public String selectFirstAvailableSize() {
        int count = sizeButtons.count();
        for (int i = 0; i < count; i++) {
            Locator sb = sizeButtons.nth(i);
            if (sb.isVisible() && isSizeAvailable(sb)) {
                String sizeText = sb.innerText().trim().split("\n")[0];
                sb.click();
                page.waitForTimeout(300);
                return sizeText;
            }
        }
        return "";
    }

    /**
     * Checks if a size button is available (not disabled and not struck through).
     */
    public boolean isSizeAvailable(Locator sizeBtn) {
        if (sizeBtn.getAttribute("disabled") != null) return false;
        String cls = sizeBtn.getAttribute("class");
        if (cls != null && (cls.contains("disabled") || cls.contains("opacity-50"))) return false;
        // Strike-through line indicates out of stock
        Locator strikeThrough = sizeBtn.locator("span.rotate-\\[55deg\\], [class*='rotate']");
        return strikeThrough.count() == 0;
    }

    /**
     * Checks if a specific size by name is currently disabled / out of stock.
     */
    public boolean isSizeDisabled(String sizeName) {
        Locator targetSize = sizeButtons.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + Pattern.quote(sizeName) + "\\s*$"))).first();
        if (targetSize.count() == 0) return true;
        return !isSizeAvailable(targetSize);
    }

    /**
     * Sets the quantity directly by filling the numeric input.
     */
    public void setQuantity(int quantity) {
        quantityInput.fill(String.valueOf(quantity));
        quantityInput.press("Enter");
        page.waitForTimeout(200);
    }

    /**
     * Increases quantity by clicking the plus (+) button multiple times.
     */
    public void increaseQuantity(int times) {
        for (int i = 0; i < times; i++) {
            if (increaseQuantityButton.isVisible()) {
                increaseQuantityButton.click();
            } else {
                // Fallback to updating input value directly
                int current = getQuantity();
                setQuantity(current + 1);
            }
            page.waitForTimeout(150);
        }
    }

    /**
     * Decreases quantity by clicking the minus (-) button multiple times.
     */
    public void decreaseQuantity(int times) {
        for (int i = 0; i < times; i++) {
            if (decreaseQuantityButton.isVisible()) {
                decreaseQuantityButton.click();
            } else {
                int current = getQuantity();
                if (current > 1) {
                    setQuantity(current - 1);
                }
            }
            page.waitForTimeout(150);
        }
    }

    /**
     * Reads the current numeric value inside the quantity input.
     */
    public int getQuantity() {
        if (!quantityInput.isVisible()) return 1;
        String val = quantityInput.inputValue();
        if (val == null || val.trim().isEmpty()) {
            val = quantityInput.getAttribute("value");
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Clicks "Thêm vào giỏ" button and waits for toast or response.
     */
    public void clickAddToCart() {
        addToCartButton.click();
        page.waitForTimeout(1500);
    }

    /**
     * Opens the Size Guide modal by clicking "Hướng dẫn chọn size".
     */
    public void openSizeGuide() {
        sizeGuideLink.click();
        page.waitForTimeout(1000);
    }

    /**
     * Closes the Size Guide modal.
     */
    public void closeSizeGuide() {
        if (sizeGuideCloseButton.count() > 0 && sizeGuideCloseButton.isVisible()) {
            sizeGuideCloseButton.click();
        } else {
            page.keyboard().press("Escape");
        }
        page.waitForTimeout(800);
    }

    public boolean isSizeGuideModalVisible() {
        Locator modal = page.locator("div.fixed:has-text('Bảng kích thước'), *:has-text('Bảng kích thước')").first();
        return modal.count() > 0 && modal.isVisible();
    }

    /**
     * Gets the count shown on the cart badge in the header (returns 0 if badge is hidden).
     */
    public int getCartBadgeCount() {
        if (!headerCartBadge.isVisible()) {
            return 0;
        }
        String text = headerCartBadge.innerText().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses a Vietnamese formatted currency string (e.g. "423.200đ", "349.000₫", "-69.800đ") into long.
     */
    public static long parsePrice(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) return 0;
        String cleaned = priceText.replaceAll("[^0-9\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-")) return 0;
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- Getters ---

    public String getProductTitle() {
        return productTitle.innerText().trim();
    }

    public String getSku() {
        if (skuText.count() > 0 && skuText.isVisible()) {
            return skuText.innerText().trim();
        }
        return "";
    }

    public long getCurrentPrice() {
        return parsePrice(currentPrice.innerText());
    }

    public long getOriginalPrice() {
        if (originalPrice.count() > 0 && originalPrice.isVisible()) {
            return parsePrice(originalPrice.innerText());
        }
        return getCurrentPrice();
    }

    public int getDiscountPercent() {
        if (discountPercent.count() > 0 && discountPercent.isVisible()) {
            String text = discountPercent.innerText();
            Matcher matcher = Pattern.compile("(\\d+)%").matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return 0;
    }

    public boolean isAddToCartEnabled() {
        return addToCartButton.isVisible() && addToCartButton.isEnabled()
                && addToCartButton.getAttribute("disabled") == null;
    }

    public boolean isOutOfStock() {
        return outOfStockIndicator.isVisible() || !isAddToCartEnabled();
    }

    public boolean isAddedToCartToastVisible() {
        return addedToCartToast.isVisible();
    }

    public boolean hasProductDescription() {
        return productDescription.isVisible();
    }

    public int getColorCount() {
        return colorButtons.count();
    }

    public int getSizeCount() {
        return sizeButtons.count();
    }

    public String getSelectedColorText() {
        if (selectedColorLabel.isVisible()) {
            return selectedColorLabel.innerText().trim();
        }
        return "";
    }

    public Locator getAddToCartButton() {
        return addToCartButton;
    }

    public Locator getProductTitleLocator() {
        return productTitle;
    }

    public Locator getHeaderCartBadge() {
        return headerCartBadge;
    }
}
