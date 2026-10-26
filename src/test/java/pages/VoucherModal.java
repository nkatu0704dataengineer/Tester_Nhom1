package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * VoucherModal represents the Promo / Voucher selection and application dialog on Yody.vn.
 * Can be opened from Cart or Order screen (/order).
 * Handles voucher code input, searching/applying, voucher cards selection, validation errors,
 * and voucher removal.
 */
public class VoucherModal {
    private final Page page;

    // Locators - Trigger & Dialog
    private final Locator openVoucherTrigger;
    private final Locator modalDialog;
    private final Locator modalTitle;
    private final Locator closeButton;

    // Locators - Input & Apply
    private final Locator voucherInput;
    private final Locator applyButton;
    private final Locator confirmButton;

    // Locators - Voucher list & Errors
    private final Locator voucherCards;
    private final Locator emptyVoucherText;
    private final Locator errorMessage;
    private final Locator removeVoucherButton;

    public VoucherModal(Page page) {
        this.page = page;

        // Triggers
        this.openVoucherTrigger = page.locator("p:has-text('Dùng mã khuyến mãi'), button:has-text('Dùng mã khuyến mãi'), div:has-text('Dùng mã khuyến mãi')").last();

        // Modal Dialog
        this.modalDialog = page.locator("div.fixed:has(p:has-text('Chọn khuyến mãi'))").last();
        this.modalTitle = modalDialog.locator("p:has-text('Chọn khuyến mãi')").first();
        this.closeButton = modalDialog.locator("button[aria-label='Close dialog']").first();

        // Voucher Input & Action buttons
        this.voucherInput = modalDialog.locator("input[placeholder='Tìm kiếm'], input").first();
        this.applyButton = modalDialog.locator("button:has-text('Tìm kiếm')").first();
        this.confirmButton = modalDialog.locator("button[aria-label='confirm voucher'], button:has-text('Xác nhận')").first();

        // Cards, Empty text, Error, Remove
        this.voucherCards = modalDialog.locator("div:has(> p:has-text('Giảm')), [class*='voucher-item'], [class*='coupon']");
        this.emptyVoucherText = modalDialog.locator("p:has-text('Hiện chưa có mã khuyến mãi nào')").first();
        this.errorMessage = modalDialog.locator("p.text-red-500, p.text-negative-text, [data-error='true'], p:has-text('không hợp lệ'), p:has-text('hết hạn'), p:has-text('chưa đủ điều kiện'), p:has-text('không tồn tại')").first();
        this.removeVoucherButton = page.locator("button:has-text('Gỡ bỏ'), button:has-text('Bỏ chọn'), button:has-text('Xóa mã'), button:has-text('Gỡ')").first();
    }

    // --- Action Methods ---

    /**
     * Opens the Voucher dialog by clicking "Dùng mã khuyến mãi".
     */
    public void openVoucherModal() {
        if (!isModalVisible()) {
            openVoucherTrigger.click();
            page.waitForTimeout(600);
        }
    }

    /**
     * Closes the Voucher dialog.
     */
    public void closeVoucherModal() {
        if (closeButton.isVisible()) {
            closeButton.click();
            page.waitForTimeout(400);
        } else {
            page.keyboard().press("Escape");
        }
    }

    /**
     * Checks if the Voucher modal dialog is currently visible.
     */
    public boolean isModalVisible() {
        return modalTitle.count() > 0 && modalTitle.isVisible();
    }

    /**
     * Enters a voucher code and clicks the Apply ("Tìm kiếm") button.
     */
    public void applyVoucherCode(String code) {
        openVoucherModal();
        voucherInput.fill(code);
        page.waitForTimeout(200);
        applyButton.click();
        page.waitForTimeout(800);
    }

    /**
     * Confirms the selected voucher by clicking "Xác nhận".
     */
    public void confirmSelection() {
        if (confirmButton.isVisible() && confirmButton.isEnabled()) {
            confirmButton.click();
            page.waitForTimeout(600);
        }
    }

    /**
     * Removes the currently applied voucher.
     */
    public void removeAppliedVoucher() {
        if (removeVoucherButton.count() > 0 && removeVoucherButton.isVisible()) {
            removeVoucherButton.click();
            page.waitForTimeout(600);
        }
    }

    /**
     * Gets the validation error message shown in the modal (or empty string if none).
     */
    public String getVoucherErrorMessage() {
        if (errorMessage.count() > 0 && errorMessage.isVisible()) {
            return errorMessage.innerText().trim();
        }
        Locator generalError = modalDialog.locator("p:has-text('không hợp lệ'), p:has-text('hết hạn'), p:has-text('chưa đủ điều kiện'), p:has-text('không tồn tại')").last();
        if (generalError.count() > 0 && generalError.isVisible()) {
            return generalError.innerText().trim();
        }
        return "";
    }

    /**
     * Checks if there are available voucher cards displayed in the modal.
     */
    public boolean hasVouchersAvailable() {
        return voucherCards.count() > 0;
    }

    /**
     * Checks if empty state ("Hiện chưa có mã khuyến mãi nào") is displayed.
     */
    public boolean isEmptyVoucherListDisplayed() {
        return emptyVoucherText.count() > 0 && emptyVoucherText.isVisible();
    }

    // --- Locators Getters ---

    public Locator getModalDialog() {
        return modalDialog;
    }

    public Locator getVoucherInput() {
        return voucherInput;
    }

    public Locator getApplyButton() {
        return applyButton;
    }

    public Locator getErrorMessage() {
        return errorMessage;
    }
}
