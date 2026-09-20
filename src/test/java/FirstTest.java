import com.microsoft.playwright.*;
import java.nio.file.Paths;

public class FirstTest {
    public static void main(String[] args) {
        // 1. Khởi tạo Playwright
        try (Playwright playwright = Playwright.create()) {

            // 2. Mở trình duyệt Chromium (Google Chrome engine) ở chế độ có giao diện (headless = false)
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(500)
            );

            // 3. Tạo một trang mới
            Page page = browser.newPage();

            // 4. Truy cập website Yody
            System.out.println("Đang mở trang web Yody.vn...");
            page.navigate("https://yody.vn");

            // 5. In tiêu đề trang web ra Console
            System.out.println("Tiêu đề trang: " + page.title());

            // 6. Chụp ảnh màn hình lưu vào thư mục dự án
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("yody_homepage.png")));
            System.out.println("Đã chụp ảnh màn hình thành công!");

            // 7. Đóng trình duyệt
            browser.close();
        }
    }
}