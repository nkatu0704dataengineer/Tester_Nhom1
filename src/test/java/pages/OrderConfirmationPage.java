package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Page Object for Yody.vn Order Confirmation Page (/order).
 */
public class OrderConfirmationPage {

    private final Page page;

    // Delivery method tabs
    private final Locator homeDeliveryTab;
    private final Locator storePickupTab;

    // Recipient fields
    private final Locator fullNameInput;
    private final Locator phoneInput;
    private final Locator emailInput;
    private final Locator genderMaleRadio;
    private final Locator genderFemaleRadio;

    // Address selector
    private final Locator openAddressBtn;
    private final Locator addressModal;
    private final Locator detailedAddressInput;

    // Order Note
    private final Locator noteInput;

    // Voucher & Payment
    private final Locator voucherTrigger;
    private final Locator paymentMethodRow;
    private final Locator paymentModal;

    // Order Summary
    private final Locator totalPriceText;
    private final Locator discountText;
    private final Locator shippingFeeText;
    private final Locator finalTotalText;

    // Policy & Place Order
    private final Locator policyConsentCheckbox;
    private final Locator placeOrderBtn;

    public OrderConfirmationPage(Page page) {
        this.page = page;

        this.homeDeliveryTab = page.locator("button[role='tab']:has-text('Giao tận nơi')").first();
        this.storePickupTab = page.locator("button[role='tab']:has-text('Nhận tại cửa hàng')").first();

        this.fullNameInput = page.locator("input[name='full_name'], input[placeholder*='tên khách hàng']").first();
        this.phoneInput = page.locator("input[name='phone'], input[placeholder*='số điện thoại']").first();
        this.emailInput = page.locator("input[name='email'], input[placeholder*='email']").first();
        this.genderMaleRadio = page.locator("label[for='male'], input[value='male']").first();
        this.genderFemaleRadio = page.locator("label[for='female'], input[value='female']").first();

        this.openAddressBtn = page.locator("button[aria-label='open address'], button:has-text('Tỉnh/Thành phố')").first();
        this.addressModal = page.locator("div.fixed:has-text('địa chỉ nhận hàng'), [role='dialog']").last();
        this.detailedAddressInput = page.locator("input[name='address'], input[placeholder*='Ví dụ:']").first();

        this.noteInput = page.locator("textarea[name='note'], textarea[placeholder*='Yêu cầu khác']").first();

        this.voucherTrigger = page.locator("div.cursor-pointer:has(p:has-text('Dùng mã khuyến mãi')), *:has-text('Dùng mã khuyến mãi')").last();
        this.paymentMethodRow = page.locator("div.cursor-pointer:has(p:has-text('Thanh toán')), div:has-text('(COD) Thanh toán khi nhận hàng')").last();
        this.paymentModal = page.locator("div.fixed:has-text('Chọn phương thức thanh toán'), [role='dialog']").last();

        this.totalPriceText = page.locator("div.flex:has(p:has-text('Tổng tiền')) p").last();
        this.discountText = page.locator("div.flex:has(p:has-text('Giảm giá')) p").last();
        this.shippingFeeText = page.locator("div.flex:has(p:has-text('Phí vận chuyển')) p").last();
        this.finalTotalText = page.locator("p.text-price-xl, div.flex:has(p:has-text('Thành tiền')) p.font-bold").first();

        this.policyConsentCheckbox = page.locator("button#policy_consent_version[role='checkbox'], input[name='policy_consent_version']").first();
        this.placeOrderBtn = page.locator("button[aria-label='submit order'], button:has-text('Đặt hàng')").last();
    }

    public void navigateTo() {
        page.navigate("https://yody.vn/order", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForTimeout(1000);
    }

    public void selectDeliveryMethod(boolean isHomeDelivery) {
        if (isHomeDelivery) {
            homeDeliveryTab.click();
        } else {
            storePickupTab.click();
        }
        page.waitForTimeout(500);
    }

    public void fillRecipientInfo(String fullName, String phone, String email) {
        if (fullName != null) {
            fullNameInput.fill(fullName);
        }
        if (phone != null) {
            phoneInput.fill(phone);
        }
        if (email != null && emailInput.isVisible()) {
            emailInput.fill(email);
        }
    }

    public void openAddressModalAndFill(String fullName, String phone, String province, String district, String ward, String detailAddress) {
        fillRecipientInfo(fullName, phone, null);

        if (province != null && openAddressBtn.isVisible()) {
            openAddressBtn.click();
            page.waitForTimeout(600);

            // Select Province
            page.locator("div.fixed p:has-text('" + province + "')").first().click();
            page.waitForTimeout(600);

            // Select District
            if (district != null) {
                page.locator("div.fixed p:has-text('" + district + "')").first().click();
                page.waitForTimeout(600);
            }

            // Select Ward
            if (ward != null) {
                page.locator("div.fixed p:has-text('" + ward + "')").first().click();
                page.waitForTimeout(600);
            }

            // Click submit address if visible, or close dialog
            Locator submitAddressBtn = page.locator("button[aria-label='submit address']");
            if (submitAddressBtn.count() > 0 && submitAddressBtn.first().isVisible()) {
                submitAddressBtn.first().click();
                page.waitForTimeout(600);
            }
            Locator closeDialogBtn = page.locator("div.fixed:has-text('địa chỉ') button[aria-label='Close dialog']");
            if (closeDialogBtn.count() > 0 && closeDialogBtn.first().isVisible()) {
                closeDialogBtn.first().click();
                page.waitForTimeout(600);
            }
        }

        if (detailAddress != null && detailedAddressInput.isVisible()) {
            detailedAddressInput.fill(detailAddress);
        }
    }

    public void enterOrderNote(String note) {
        if (noteInput.isVisible()) {
            noteInput.fill(note);
        }
    }

    public String getOrderNote() {
        return noteInput.inputValue();
    }

    public void selectPaymentMethod(String methodName) {
        paymentMethodRow.click();
        page.waitForTimeout(600);

        if (methodName.equalsIgnoreCase("COD")) {
            page.locator("label[for='COD']").first().click();
        } else if (methodName.equalsIgnoreCase("momo") || methodName.toLowerCase().contains("momo")) {
            page.locator("label[for='momo']").first().click();
        } else if (methodName.equalsIgnoreCase("bank") || methodName.toLowerCase().contains("atm")) {
            page.locator("label[for='bank']").first().click();
        } else if (methodName.equalsIgnoreCase("credit") || methodName.toLowerCase().contains("quốc tế")) {
            page.locator("label[for='credit']").first().click();
        } else if (methodName.equalsIgnoreCase("vnpay") || methodName.toLowerCase().contains("vnpay")) {
            page.locator("label[for='vnpay']").first().click();
        }

        Locator confirmBtn = page.locator("button[aria-label='confirm payment method'], button:has-text('Xác nhận')").last();
        if (confirmBtn.isVisible()) {
            confirmBtn.click();
            page.waitForTimeout(500);
        }
    }

    public void setPrivacyPolicyAgreement(boolean agreed) {
        if (policyConsentCheckbox.isVisible()) {
            String state = policyConsentCheckbox.getAttribute("data-state");
            boolean isChecked = "checked".equalsIgnoreCase(state) || policyConsentCheckbox.isChecked();
            if (agreed != isChecked) {
                policyConsentCheckbox.click();
                page.waitForTimeout(300);
            }
        }
    }

    public boolean isPrivacyPolicyAgreed() {
        if (!policyConsentCheckbox.isVisible()) return true;
        String state = policyConsentCheckbox.getAttribute("data-state");
        return "checked".equalsIgnoreCase(state) || policyConsentCheckbox.isChecked();
    }

    public void clickPlaceOrder() {
        Locator closeDialogBtn = page.locator("div.fixed:has-text('địa chỉ') button[aria-label='Close dialog']");
        if (closeDialogBtn.count() > 0 && closeDialogBtn.first().isVisible()) {
            closeDialogBtn.first().click();
            page.waitForTimeout(400);
        }
        placeOrderBtn.click(new Locator.ClickOptions().setForce(true));
    }

    public boolean isPlaceOrderButtonEnabled() {
        return placeOrderBtn.isEnabled();
    }

    public String getShippingFeeText() {
        Locator loc = page.locator("div.flex:has(p:has-text('Phí vận chuyển')) p.text-price-md, div.flex:has(p:has-text('Phí vận chuyển')) p");
        for (int i = 0; i < loc.count(); i++) {
            if (loc.nth(i).isVisible() && !loc.nth(i).innerText().contains("Phí vận chuyển")) {
                return loc.nth(i).innerText().trim();
            }
        }
        if (shippingFeeText.isVisible()) {
            return shippingFeeText.innerText().trim();
        }
        return "miễn phí";
    }

    public long getShippingFeeAmount() {
        String fee = getShippingFeeText().toLowerCase();
        if (fee.contains("miễn phí")) {
            return 0;
        }
        return parsePrice(fee);
    }

    public long getTotalPrice() {
        Locator loc = page.locator("div.flex:has(p:has-text('Tổng tiền')) p.text-price-md, div.flex:has(p:has-text('Tổng tiền')) p");
        for (int i = 0; i < loc.count(); i++) {
            if (loc.nth(i).isVisible() && !loc.nth(i).innerText().contains("Tổng tiền")) {
                return parsePrice(loc.nth(i).innerText());
            }
        }
        if (totalPriceText.isVisible()) {
            return parsePrice(totalPriceText.innerText());
        }
        return 0;
    }

    public long getDiscountAmount() {
        Locator loc = page.locator("div.flex:has(p:has-text('Giảm giá')) p, p:has-text('Tiết kiệm')");
        for (int i = 0; i < loc.count(); i++) {
            if (loc.nth(i).isVisible() && !loc.nth(i).innerText().equals("Giảm giá")) {
                return parsePrice(loc.nth(i).innerText());
            }
        }
        if (discountText.isVisible()) {
            return parsePrice(discountText.innerText());
        }
        return 0;
    }

    public long getFinalTotalAmount() {
        Locator candidates = page.locator("p.text-price-xl, div.flex:has(p:has-text('Thành tiền')) p:has-text('đ')");
        for (int i = 0; i < candidates.count(); i++) {
            if (candidates.nth(i).isVisible()) {
                String text = candidates.nth(i).innerText();
                if (!text.toLowerCase().contains("tiết kiệm") && !text.toLowerCase().contains("thành tiền")) {
                    return parsePrice(text);
                }
            }
        }
        return 0;
    }

    public VoucherModal openVoucherModal() {
        voucherTrigger.click();
        page.waitForTimeout(600);
        return new VoucherModal(page);
    }

    public boolean isErrorMessageDisplayed(String keyword) {
        Locator errorLoc = page.locator("p.text-red-500, [data-error='true'], [role='alert'], p:has-text('" + keyword + "')");
        return errorLoc.count() > 0 && errorLoc.first().isVisible();
    }

    public long parsePrice(String text) {
        if (text == null) return 0;
        String clean = text.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return 0;
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
