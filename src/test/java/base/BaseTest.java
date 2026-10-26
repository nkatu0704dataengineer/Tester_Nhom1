package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BaseTest manages the lifecycle of Playwright, Browser, BrowserContext, and Page.
 * Implements Playwright Tracing on test failure and handles initial setup for Yody.vn.
 */
public abstract class BaseTest {
    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    protected static final String BASE_URL = "https://yody.vn";

    @BeforeClass(alwaysRun = true)
    public void setupClass() {
        if (playwright == null) {
            playwright = Playwright.create();
            boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "false"));
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadless)
                    .setSlowMo(100));
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setupTest(ITestResult result) {
        // Initialize isolated BrowserContext with 1920x1080 viewport
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));

        // Start Playwright Tracing with screenshots, snapshots, and sources
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();

        if (shouldNavigateHomeOnInit()) {
            // Navigate to Yody home page and dismiss initial promo banners if present
            page.navigate(BASE_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            closePromotionalPopupsIfAny();
        }
    }

    /**
     * Override this hook in child test classes to skip initial home page navigation if not needed.
     */
    protected boolean shouldNavigateHomeOnInit() {
        return true;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTest(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                // Ensure traces folder exists
                File tracesDir = new File("traces");
                if (!tracesDir.exists()) {
                    tracesDir.mkdirs();
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String traceFileName = String.format("traces/%s_%s.zip", result.getMethod().getMethodName(), timestamp);
                Path tracePath = Paths.get(traceFileName);

                context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
                System.out.println("[INFO] Test failed. Playwright trace recorded at: " + tracePath.toAbsolutePath());
            } else {
                // Stop tracing without saving to conserve disk space
                context.tracing().stop();
            }
        } catch (Exception e) {
            System.err.println("[WARN] Error stopping Playwright trace: " + e.getMessage());
        } finally {
            if (page != null) {
                page.close();
            }
            if (context != null) {
                context.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    /**
     * Checks and dismisses any promotional popup / marketing banner if it appears on the homepage.
     */
    protected void closePromotionalPopupsIfAny() {
        try {
            // Dismiss common promo/voucher dialog close buttons if displayed
            Locator promoCloseBtn = page.locator("button[aria-label='Close dialog'], .popup-close, .modal-close, button:has-text('Bỏ qua'), div.banner-popup button")
                    .first();
            if (promoCloseBtn.isVisible(new Locator.IsVisibleOptions().setTimeout(1500))) {
                promoCloseBtn.click();
            }
        } catch (Exception ignored) {
            // No promo popup displayed; proceed normally
        }
    }

    public Page getPage() {
        return page;
    }
}
