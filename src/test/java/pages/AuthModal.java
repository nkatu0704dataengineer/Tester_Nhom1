package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * AuthModal represents Screen 1 of Authentication: Entering Email for Passwordless Login / Register.
 */
public class AuthModal {
    private final Page page;

    // Locators
    private final Locator modalContainer;
    private final Locator title;
    private final Locator emailInput;
    private final Locator continueButton;
    private final Locator googleLoginButton;
    private final Locator closeButton;
    private final Locator errorMessage;

    public AuthModal(Page page) {
        this.page = page;

        // Container of the Auth modal dialog
        this.modalContainer = page.locator("div.fixed.inset-0.z-\\[70\\], div[role='dialog']").first();

        // User-centric standard Playwright locators
        this.title = modalContainer.getByText("Đăng nhập / Đăng ký");
        this.emailInput = modalContainer.locator("input[name='email'], input[type='email']").first();
        this.continueButton = modalContainer.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Tiếp tục").setExact(true));
        this.googleLoginButton = modalContainer.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Tiếp tục với Google"));
        this.closeButton = modalContainer.locator("button[aria-label='Close dialog']");
        this.errorMessage = modalContainer.locator("div.text-field-wrapper p, .text-negative, div[data-error='true'] + p, p.text-red-500").first();
    }

    /**
     * Enters email address into the email input field.
     *
     * @param email Email string to enter
     */
    public AuthModal enterEmail(String email) {
        emailInput.fill(email);
        return this;
    }

    /**
     * Clears the email input field.
     */
    public AuthModal clearEmail() {
        emailInput.clear();
        return this;
    }

    /**
     * Clicks the "Tiếp tục" button.
     *
     * @return OtpModal instance
     */
    public OtpModal clickContinue() {
        continueButton.click();
        return new OtpModal(page);
    }

    /**
     * Closes the Auth modal by clicking the 'X' (Close dialog) button.
     */
    public void closeModal() {
        closeButton.click();
    }

    /**
     * Checks if the "Tiếp tục" button is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isContinueButtonEnabled() {
        return continueButton.isEnabled();
    }

    // Getters for assertions
    public Locator getModalContainerLoc() {
        return modalContainer;
    }

    public Locator getTitleLoc() {
        return title;
    }

    public Locator getEmailInputLoc() {
        return emailInput;
    }

    public Locator getContinueBtnLoc() {
        return continueButton;
    }

    public Locator getGoogleLoginBtnLoc() {
        return googleLoginButton;
    }

    public Locator getCloseBtnLoc() {
        return closeButton;
    }

    public Locator getErrorMessageLoc() {
        return errorMessage;
    }
}
