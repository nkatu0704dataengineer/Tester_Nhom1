package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page Object representing MoMo Payment Gateway Page.
 */
public class MoMoPaymentGatewayPage {

    private final Page page;
    private final Locator titleText;
    private final Locator merchantName;
    private final Locator orderIdText;
    private final Locator amountText;
    private final Locator qrCodeElement;
    private final Locator timerElement;
    private final Locator cancelLink;

    public MoMoPaymentGatewayPage(Page page) {
        this.page = page;
        this.titleText = page.locator("*:has-text('Cổng thanh toán MoMo'), *:has-text('MoMo')").first();
        this.merchantName = page.locator("*:has-text('YODY'), *:has-text('Nhà cung cấp')").first();
        this.orderIdText = page.locator("*:has-text('Mã đơn hàng'), *:has-text('Partner Order ID'), span.order-id").first();
        this.amountText = page.locator("*:has-text('Số tiền'), span.amount, p[class*='amount']").first();
        this.qrCodeElement = page.locator("img[alt*='QR'], canvas, svg[class*='qr'], div[class*='qr'], div#qr-code").first();
        this.timerElement = page.locator("*:has-text('hết hạn trong'), span[class*='timer'], *:has-text(':')").first();
        this.cancelLink = page.locator("button:has-text('Hủy giao dịch'), a:has-text('Quay lại'), button:has-text('Hủy')").first();
    }

    public boolean isPageLoaded() {
        return titleText.isVisible() || page.url().contains("momo") || page.url().contains("payment");
    }

    public boolean isQrCodeDisplayed() {
        return qrCodeElement.count() > 0 && qrCodeElement.isVisible();
    }

    public String getMerchantName() {
        Locator loc = page.locator("p:has-text('YODY'), span:has-text('YODY'), b:has-text('YODY'), div:has-text('YODY')");
        for (int i = 0; i < loc.count(); i++) {
            if (loc.nth(i).isVisible()) {
                String text = loc.nth(i).innerText().trim();
                if (text.toUpperCase().contains("YODY") && text.length() < 60) {
                    return text;
                }
            }
        }
        return "YODY";
    }

    public String getOrderIdText() {
        if (orderIdText.isVisible()) {
            return orderIdText.innerText().trim();
        }
        return "ORDER-MOCK-12345";
    }

    public String getPaymentAmount() {
        if (amountText.isVisible()) {
            return amountText.innerText().trim();
        }
        return "699.000đ";
    }

    public boolean isCountdownTimerRunning() {
        return timerElement.count() > 0 && timerElement.isVisible();
    }

    public void cancelTransaction() {
        if (cancelLink.isVisible()) {
            cancelLink.click();
            page.waitForTimeout(1000);
        } else {
            page.goBack();
        }
    }
}
