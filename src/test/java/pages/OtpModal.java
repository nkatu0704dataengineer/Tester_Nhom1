package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;

/**
 * OtpModal represents Screen 2 of Authentication: Verifying 4-digit OTP.
 */
public class OtpModal {
    private final Page page;

    // Locators
    private final Locator modalContainer;
    private final Locator title;
    private final Locator emailNotificationText;
    private final Locator otpHiddenInput;
    private final Locator otpSlots;
    private final Locator countdownTimerText;
    private final Locator remainingSendsText;
    private final Locator verifyButton;
    private final Locator resendButton;
    private final Locator backButton;
    private final Locator closeButton;
    private final Locator errorMessage;

    public OtpModal(Page page) {
        this.page = page;

        this.modalContainer = page.locator("div.fixed.inset-0.z-\\[70\\], div[role='dialog']").first();
        this.title = modalContainer.getByText("Nhập mã OTP");
        this.emailNotificationText = modalContainer.locator("text=/Vui lòng kiểm tra email/i");

        // The unified input element handling the 4-digit code
        this.otpHiddenInput = modalContainer.locator("input[data-slot='input-otp'], input[autocomplete='one-time-code']").first();

        // The 4 visible digit slots
        this.otpSlots = modalContainer.locator("[data-slot='input-otp-slot']");

        // Timer and remaining counts
        this.countdownTimerText = modalContainer.locator("text=/sau \\d{2}:\\d{2}/i");
        this.remainingSendsText = modalContainer.locator("text=/Số lần gửi OTP còn lại/i");

        // Action buttons
        this.verifyButton = modalContainer.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Tiếp tục").setExact(true));
        this.resendButton = modalContainer.locator("button:has-text('Gửi lại mã'), a:has-text('Gửi lại mã'), text=Gửi lại mã").first();
        this.backButton = modalContainer.locator("button:has(svg path[d*='M17 10']), div.flex-col > button:first-child").first();
        this.closeButton = modalContainer.locator("button[aria-label='Close dialog']");

        // Error alerts and validation notifications triggered upon verification failure
        this.errorMessage = modalContainer.locator(".text-negative-text, [data-slot='input-otp-group'] + div, [data-slot='input-otp-slot'].border-border-negative, [data-sonner-toast], [role='status'], [role='alert']").first();
    }

    /**
     * Enters OTP digits into the OTP input field.
     *
     * @param otp OTP string (e.g. "1234")
     */
    public OtpModal enterOtp(String otp) {
        otpHiddenInput.fill(otp);
        return this;
    }

    /**
     * Clears all entered OTP digits.
     */
    public OtpModal clearOtp() {
        otpHiddenInput.clear();
        return this;
    }

    /**
     * Clicks the "Tiếp tục" / Verify button.
     */
    public void clickVerify() {
        verifyButton.click();
    }

    /**
     * Clicks "Gửi lại mã" button.
     */
    public void clickResendOtp() {
        resendButton.click();
    }

    /**
     * Clicks the Back arrow button to return to the Email screen.
     *
     * @return AuthModal instance
     */
    public AuthModal clickBack() {
        backButton.click();
        return new AuthModal(page);
    }

    /**
     * Closes the OTP modal dialog.
     */
    public void closeModal() {
        closeButton.click();
    }

    /**
     * Checks if the "Tiếp tục" button is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isVerifyButtonEnabled() {
        return verifyButton.isEnabled();
    }

    // Getters for assertions
    public Locator getModalContainerLoc() {
        return modalContainer;
    }

    public Locator getTitleLoc() {
        return title;
    }

    public Locator getEmailNotificationLoc() {
        return emailNotificationText;
    }

    public Locator getOtpHiddenInputLoc() {
        return otpHiddenInput;
    }

    public Locator getOtpSlotsLoc() {
        return otpSlots;
    }

    public Locator getCountdownTimerLoc() {
        return countdownTimerText;
    }

    public Locator getRemainingSendsLoc() {
        return remainingSendsText;
    }

    public Locator getVerifyBtnLoc() {
        return verifyButton;
    }

    public Locator getResendBtnLoc() {
        return resendButton;
    }

    public Locator getBackBtnLoc() {
        return backButton;
    }

    public Locator getCloseBtnLoc() {
        return closeButton;
    }

    public Locator getErrorMessageLoc() {
        return errorMessage;
    }
}
