package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * HomePage represents the Yody.vn main landing page.
 * Provides entry points to header navigation, authentication modal, and search overlay.
 */
public class HomePage {
    private final Page page;

    // Locators
    private final Locator headerLoginButton;
    private final Locator headerSearchInput;
    private final Locator searchOverlay;
    private final Locator closeOverlayButton;

    public HomePage(Page page) {
        this.page = page;

        // User-facing locator targeting the Login icon/button in the header
        this.headerLoginButton = page.locator("header").getByText("Đăng nhập").first();

        // Search elements
        this.headerSearchInput = page.locator("header input, input[placeholder*='Tìm']").first();
        this.searchOverlay = page.locator("div.fixed.inset-0:has(button:has-text('Đóng')), div.fixed:has-text('BST POLO')").first();
        this.closeOverlayButton = page.locator("button:has-text('Đóng')").first();
    }

    /**
     * Opens the Authentication Modal (Email input screen).
     *
     * @return AuthModal instance
     */
    public AuthModal openAuthModal() {
        headerLoginButton.click();
        AuthModal authModal = new AuthModal(page);
        authModal.getModalContainerLoc().waitFor();
        return authModal;
    }

    /**
     * Opens the search overlay by clicking on the header search bar.
     *
     * @return HomePage instance
     */
    public HomePage openSearchOverlay() {
        headerSearchInput.click();
        page.waitForTimeout(500);
        return this;
    }

    /**
     * Closes the search overlay.
     *
     * @return HomePage instance
     */
    public HomePage closeSearchOverlay() {
        if (closeOverlayButton.isVisible()) {
            closeOverlayButton.click();
        }
        return this;
    }

    /**
     * Enters a search keyword and presses Enter to navigate to the Search & Filter results page.
     *
     * @param keyword Keyword string to search for
     * @return SearchAndFilterPage instance
     */
    public SearchAndFilterPage searchByKeyword(String keyword) {
        headerSearchInput.click();
        page.waitForTimeout(500);

        // Find the active editable search input in the overlay (header input has readonly)
        Locator input = page.locator("input:not([readonly])").first();
        if (keyword != null && !keyword.isEmpty()) {
            input.fill(keyword);
            page.keyboard().press("Enter");
            try {
                page.waitForURL("**/search**", new Page.WaitForURLOptions().setTimeout(5000));
            } catch (Exception ignored) {
            }
        } else {
            page.keyboard().press("Enter");
            page.waitForTimeout(1000);
        }
        page.waitForLoadState();

        return new SearchAndFilterPage(page);
    }

    public Locator getHeaderLoginButton() {
        return headerLoginButton;
    }

    public Locator getHeaderSearchInput() {
        return headerSearchInput;
    }

    public Locator getSearchOverlay() {
        return searchOverlay;
    }

    public Locator getCloseOverlayButton() {
        return closeOverlayButton;
    }
}
