package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * AccountProfilePage represents the Profile and Personal Information management screen.
 * Encapsulates view elements and the "Sửa thông tin cá nhân" (Edit Profile) modal.
 */
public class AccountProfilePage {

    private final Page page;

    // View Locators
    private final Locator titleHeading;
    private final Locator editProfileButton;
    private final Locator orderHistoryLink;
    private final Locator addressBookLink;
    private final Locator logoutButton;

    private final Locator displayedFullName;
    private final Locator displayedPhone;
    private final Locator displayedEmail;
    private final Locator displayedGender;

    // Modal Locators
    private final Locator editModal;
    private final Locator genderMaleRadio;
    private final Locator genderFemaleRadio;
    private final Locator fullNameInput;
    private final Locator phoneInput;
    private final Locator emailInput;
    private final Locator confirmSaveButton;
    private final Locator closeModalButton;

    private final Locator fullNameError;
    private final Locator phoneError;
    private final Locator successToast;

    public AccountProfilePage(Page page) {
        this.page = page;

        // View elements
        this.titleHeading = page.locator("h1:has-text('Thông tin cá nhân')").first();
        this.editProfileButton = page.locator("button:has-text('Sửa thông tin cá nhân'), #btn-open-edit-modal").first();
        this.orderHistoryLink = page.locator("a:has-text('Lịch sử mua hàng'), #nav-orders").first();
        this.addressBookLink = page.locator("a:has-text('Sổ địa chỉ nhận hàng'), #nav-addresses").first();
        this.logoutButton = page.locator("button:has-text('Đăng xuất'), #btn-logout").first();

        this.displayedFullName = page.locator("#displayed-fullname").first();
        this.displayedPhone = page.locator("#displayed-phone").first();
        this.displayedEmail = page.locator("#displayed-email").first();
        this.displayedGender = page.locator("#displayed-gender").first();

        // Modal elements
        this.editModal = page.locator("#edit-profile-modal, div[role='dialog']:has-text('Sửa thông tin cá nhân')").first();
        this.genderMaleRadio = page.locator("input[type='radio'][value='Nam'], #radio-male, label:has-text('Nam') input").first();
        this.genderFemaleRadio = page.locator("input[type='radio'][value='Nữ'], #radio-female, label:has-text('Nữ') input").first();
        this.fullNameInput = page.locator("#input-fullname, input[placeholder*='Họ và tên'], input[name='fullName']").first();
        this.phoneInput = page.locator("#input-phone, input[placeholder*='Số điện thoại'], input[name='phone']").first();
        this.emailInput = page.locator("#input-email, input[placeholder*='Email'], input[name='email']").first();
        this.confirmSaveButton = page.locator("#btn-confirm-save, button:has-text('Xác nhận')").first();
        this.closeModalButton = page.locator("#btn-close-modal, button[aria-label='Close'], .modal-close").first();

        this.fullNameError = page.locator("#error-fullname, .error-msg:has-text('Họ và tên')").first();
        this.phoneError = page.locator("#error-phone, .error-msg:has-text('Số điện thoại')").first();
        this.successToast = page.locator("#toast-success, div[role='alert']:has-text('Cập nhật thông tin thành công')").first();
    }

    /**
     * Navigates directly to the Account Profile page.
     */
    public AccountProfilePage navigateToProfilePage() {
        page.navigate("https://yody.vn/account");
        page.waitForLoadState();
        return this;
    }

    /**
     * Opens the Edit Profile modal by clicking "Sửa thông tin cá nhân".
     */
    public AccountProfilePage openEditProfileModal() {
        editProfileButton.click();
        editModal.waitFor();
        return this;
    }

    /**
     * Selects gender radio option ("Nam" or "Nữ").
     */
    public AccountProfilePage selectGender(String gender) {
        if ("Nữ".equalsIgnoreCase(gender)) {
            genderFemaleRadio.check();
        } else {
            genderMaleRadio.check();
        }
        return this;
    }

    /**
     * Enters full name into the modal input field.
     */
    public AccountProfilePage enterFullName(String fullName) {
        fullNameInput.fill(fullName);
        return this;
    }

    /**
     * Enters phone number into the modal input field.
     */
    public AccountProfilePage enterPhoneNumber(String phone) {
        phoneInput.fill(phone);
        return this;
    }

    /**
     * Clicks "Xác nhận" to save changes.
     */
    public AccountProfilePage clickConfirmSave() {
        confirmSaveButton.click();
        return this;
    }

    /**
     * Closes the edit modal without saving.
     */
    public AccountProfilePage closeEditModal() {
        closeModalButton.click();
        return this;
    }

    /**
     * Performs logout action.
     */
    public void logout() {
        logoutButton.click();
        page.waitForTimeout(500);
    }

    /**
     * Returns the currently displayed Full Name on the page.
     */
    public String getDisplayedFullName() {
        return displayedFullName.innerText().trim();
    }

    /**
     * Returns the currently displayed Phone number on the page.
     */
    public String getDisplayedPhone() {
        return displayedPhone.innerText().trim();
    }

    /**
     * Returns the validation error text displayed under either full name or phone field.
     */
    public String getValidationErrorText() {
        if (fullNameError.isVisible()) {
            return fullNameError.innerText().trim();
        }
        if (phoneError.isVisible()) {
            return phoneError.innerText().trim();
        }
        return "";
    }

    /**
     * Navigates to the Order History page.
     */
    public OrderHistoryPage clickOrderHistory() {
        orderHistoryLink.click();
        return new OrderHistoryPage(page);
    }

    // Getters for Assertions
    public Locator getTitleHeading() { return titleHeading; }
    public Locator getEditModal() { return editModal; }
    public Locator getSuccessToast() { return successToast; }
    public Locator getFullNameError() { return fullNameError; }
    public Locator getPhoneError() { return phoneError; }
    public Locator getFullNameInput() { return fullNameInput; }
    public Locator getPhoneInput() { return phoneInput; }
    public Locator getEmailInput() { return emailInput; }
    public Locator getDisplayedFullNameLoc() { return displayedFullName; }
    public Locator getDisplayedPhoneLoc() { return displayedPhone; }
    public Locator getLogoutButton() { return logoutButton; }
}
