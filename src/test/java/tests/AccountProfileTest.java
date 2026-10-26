package tests;

import base.AccountTestHelper;
import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.AccountProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test Suite: Module 5 - Cập nhật thông tin cá nhân (Profile Management Tests)
 * Platform: Yody.vn
 *
 * Implements 9 test cases (TC_CN_01 -> TC_CN_09) covering:
 * - Valid full name & phone updates with persistence on reload
 * - Mandatory field validations and format validations
 * - Boundary Value Analysis (BVA) on full name length (1, 2, 50, 51 chars)
 * - Invalid phone formats & duplicate phone conflict (409 Conflict)
 * - Auto-trimming whitespace
 * - Discarding unsaved changes on modal dismissal
 */
public class AccountProfileTest extends BaseTest {

    private AccountProfilePage profilePage;

    @Override
    protected boolean shouldNavigateHomeOnInit() {
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public void setupProfileTest() {
        // Setup authenticated profile session with default user data
        AccountTestHelper.setupAuthenticatedProfileSession(page, "Nguyen Van Test", "0987654321", "yody.test01@gmail.com", "Nam");
        profilePage = new AccountProfilePage(page);
        profilePage.navigateToProfilePage();
    }

    /**
     * Test Case ID: TC_CN_01
     * Tiêu đề: Cập nhật họ tên hợp lệ
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Hiển thị thông báo lưu thành công; họ tên mới được giữ nguyên sau khi tải lại trang (page.reload()).
     */
    @Test(description = "TC_CN_01 - Cập nhật họ tên hợp lệ")
    public void testUpdateValidFullName() {
        String newName = "Tran Thi Thu Trang";

        profilePage.openEditProfileModal();
        profilePage.enterFullName(newName);
        profilePage.clickConfirmSave();

        // Kiểm tra Toast thành công và họ tên mới được hiển thị
        assertThat(profilePage.getSuccessToast()).isVisible();
        Assert.assertEquals(profilePage.getDisplayedFullName(), newName, "Họ tên hiển thị chưa đúng sau khi lưu");

        // Kiểm tra tính bền vững sau khi tải lại trang (reload)
        page.reload();
        Assert.assertEquals(profilePage.getDisplayedFullName(), newName, "Họ tên mới không được giữ nguyên sau khi reload");
    }

    /**
     * Test Case ID: TC_CN_02
     * Tiêu đề: Cập nhật số điện thoại hợp lệ
     * Mức độ ưu tiên: Cao (P1)
     *
     * Kỳ vọng: Lưu thành công; số điện thoại mới hiển thị đúng sau khi tải lại trang.
     */
    @Test(description = "TC_CN_02 - Cập nhật số điện thoại hợp lệ")
    public void testUpdateValidPhoneNumber() {
        String newPhone = "0912345678";

        profilePage.openEditProfileModal();
        profilePage.enterPhoneNumber(newPhone);
        profilePage.clickConfirmSave();

        // Kiểm tra thông báo thành công và số điện thoại mới
        assertThat(profilePage.getSuccessToast()).isVisible();
        Assert.assertEquals(profilePage.getDisplayedPhone(), newPhone, "Số điện thoại hiển thị chưa đúng");

        // Kiểm tra sau khi reload
        page.reload();
        Assert.assertEquals(profilePage.getDisplayedPhone(), newPhone, "Số điện thoại mới không được giữ nguyên sau khi reload");
    }

    /**
     * Test Case ID: TC_CN_03
     * Tiêu đề: Để trống họ tên
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hiển thị thông báo bắt buộc nhập họ tên; không lưu dữ liệu.
     */
    @Test(description = "TC_CN_03 - Để trống họ tên")
    public void testEmptyFullName() {
        String oldName = profilePage.getDisplayedFullName();

        profilePage.openEditProfileModal();
        profilePage.enterFullName("");
        profilePage.clickConfirmSave();

        // Kiểm tra thông báo bắt buộc nhập
        assertThat(profilePage.getFullNameError()).isVisible();
        Assert.assertTrue(profilePage.getValidationErrorText().contains("không được để trống"),
                "Thông báo lỗi để trống họ tên chưa chính xác: " + profilePage.getValidationErrorText());

        // Đóng modal và kiểm tra giá trị cũ được bảo toàn
        profilePage.closeEditModal();
        Assert.assertEquals(profilePage.getDisplayedFullName(), oldName, "Dữ liệu cũ bị thay đổi khi nhập trống");
    }

    @DataProvider(name = "invalidFullNameData")
    public Object[][] invalidFullNameData() {
        return new Object[][]{
                {"   ", "toàn khoảng trắng"},
                {"Nguyen123", "chứa chữ số"},
                {"@#$%", "ký tự đặc biệt"}
        };
    }

    /**
     * Test Case ID: TC_CN_04
     * Tiêu đề: Họ tên không hợp lệ
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hiển thị thông báo họ tên không hợp lệ cho từng giá trị; chặn lưu dữ liệu.
     */
    @Test(dataProvider = "invalidFullNameData", description = "TC_CN_04 - Họ tên không hợp lệ")
    public void testInvalidFullName(String invalidName, String scenario) {
        profilePage.openEditProfileModal();
        profilePage.enterFullName(invalidName);
        profilePage.clickConfirmSave();

        assertThat(profilePage.getFullNameError()).isVisible();
        Assert.assertTrue(profilePage.getValidationErrorText().toLowerCase().contains("không hợp lệ")
                        || profilePage.getValidationErrorText().toLowerCase().contains("trống"),
                "Chưa báo lỗi họ tên không hợp lệ cho kịch bản: " + scenario);

        profilePage.closeEditModal();
    }

    /**
     * Test Case ID: TC_CN_05
     * Tiêu đề: Kiểm tra độ dài biên của họ tên (Boundary Value Analysis)
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Độ dài 1 và 51 ký tự báo lỗi hoặc bị chặn không lưu; độ dài biên 2 và 50 ký tự được chấp nhận hợp lệ.
     */
    @Test(description = "TC_CN_05 - Kiểm tra độ dài biên của họ tên")
    public void testFullNameBoundaryValues() {
        // 1. Kiểm tra biên dưới không hợp lệ: 1 ký tự ("A")
        profilePage.openEditProfileModal();
        profilePage.enterFullName("A");
        profilePage.clickConfirmSave();
        assertThat(profilePage.getFullNameError()).isVisible();
        Assert.assertTrue(profilePage.getValidationErrorText().contains("độ dài từ 2 đến 50 ký tự"));

        // 2. Kiểm tra biên dưới hợp lệ: 2 ký tự ("An")
        profilePage.enterFullName("An");
        profilePage.clickConfirmSave();
        assertThat(profilePage.getSuccessToast()).isVisible();
        Assert.assertEquals(profilePage.getDisplayedFullName(), "An");

        // 3. Kiểm tra biên trên hợp lệ: 50 ký tự
        String name50 = "A".repeat(50);
        profilePage.openEditProfileModal();
        profilePage.enterFullName(name50);
        profilePage.clickConfirmSave();
        assertThat(profilePage.getSuccessToast()).isVisible();
        Assert.assertEquals(profilePage.getDisplayedFullName(), name50);

        // 4. Kiểm tra biên trên không hợp lệ: 51 ký tự
        String name51 = "A".repeat(51);
        profilePage.openEditProfileModal();
        profilePage.enterFullName(name51);
        profilePage.clickConfirmSave();
        assertThat(profilePage.getFullNameError()).isVisible();
        Assert.assertTrue(profilePage.getValidationErrorText().contains("độ dài từ 2 đến 50 ký tự"));
        profilePage.closeEditModal();
    }

    @DataProvider(name = "invalidPhoneData")
    public Object[][] invalidPhoneData() {
        return new Object[][]{
                {"123", "quá ngắn"},
                {"098765432", "9 chữ số"},
                {"09876543210", "11 chữ số"},
                {"09876abc21", "chứa ký tự chữ"},
                {"0187654321", "sai đầu số nhà mạng"}
        };
    }

    /**
     * Test Case ID: TC_CN_06
     * Tiêu đề: Số điện thoại sai định dạng
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hiển thị thông báo số điện thoại không hợp lệ cho từng giá trị; không lưu dữ liệu.
     */
    @Test(dataProvider = "invalidPhoneData", description = "TC_CN_06 - Số điện thoại sai định dạng")
    public void testInvalidPhoneNumber(String invalidPhone, String scenario) {
        String oldPhone = profilePage.getDisplayedPhone();

        profilePage.openEditProfileModal();
        profilePage.enterPhoneNumber(invalidPhone);
        profilePage.clickConfirmSave();

        // Kiểm tra lỗi hiển thị ở trường SĐT
        assertThat(profilePage.getPhoneError()).isVisible();
        Assert.assertTrue(profilePage.getValidationErrorText().contains("không hợp lệ"),
                "Chưa báo lỗi SĐT không hợp lệ cho kịch bản: " + scenario);

        profilePage.closeEditModal();
        Assert.assertEquals(profilePage.getDisplayedPhone(), oldPhone, "SĐT không hợp lệ vẫn bị lưu");
    }

    /**
     * Test Case ID: TC_CN_07
     * Tiêu đề: Số điện thoại đã được tài khoản khác sử dụng
     * Mức độ ưu tiên: Trung bình (P2)
     *
     * Kỳ vọng: Hệ thống báo số điện thoại đã được sử dụng; không ghi đè dữ liệu.
     */
    @Test(description = "TC_CN_07 - Số điện thoại đã được tài khoản khác sử dụng")
    public void testDuplicatePhoneNumberConflict() {
        String duplicatePhone = "0911222333";
        String oldPhone = profilePage.getDisplayedPhone();

        profilePage.openEditProfileModal();
        profilePage.enterPhoneNumber(duplicatePhone);
        profilePage.clickConfirmSave();

        // Kiểm tra thông báo số điện thoại đã được sử dụng (409 Conflict)
        assertThat(profilePage.getPhoneError()).isVisible();
        Assert.assertTrue(profilePage.getValidationErrorText().contains("đã được tài khoản khác sử dụng"),
                "Chưa thông báo lỗi trùng số điện thoại: " + profilePage.getValidationErrorText());

        profilePage.closeEditModal();
        Assert.assertEquals(profilePage.getDisplayedPhone(), oldPhone, "Số điện thoại bị ghi đè dù đã tồn tại");
    }

    /**
     * Test Case ID: TC_CN_08
     * Tiêu đề: Họ tên có khoảng trắng ở đầu và cuối
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Hệ thống tự động cắt bỏ khoảng trắng thừa (trim) và lưu thành "Nguyen Van Test".
     */
    @Test(description = "TC_CN_08 - Họ tên có khoảng trắng ở đầu và cuối")
    public void testTrimWhitespaceInFullName() {
        String untrimmedName = "   Nguyen Van Test Trimmed   ";
        String expectedName = "Nguyen Van Test Trimmed";

        profilePage.openEditProfileModal();
        profilePage.enterFullName(untrimmedName);
        profilePage.clickConfirmSave();

        assertThat(profilePage.getSuccessToast()).isVisible();
        Assert.assertEquals(profilePage.getDisplayedFullName(), expectedName,
                "Hệ thống không tự động trim khoảng trắng thừa đầu và cuối họ tên");
    }

    /**
     * Test Case ID: TC_CN_09
     * Tiêu đề: Thoát khỏi trang/modal khi chưa lưu thay đổi
     * Mức độ ưu tiên: Thấp (P3)
     *
     * Kỳ vọng: Thông tin cũ vẫn được giữ nguyên; thay đổi chưa lưu không bị ghi nhận.
     */
    @Test(description = "TC_CN_09 - Thoát khỏi modal khi chưa lưu thay đổi")
    public void testDiscardUnsavedChangesOnClose() {
        String originalName = profilePage.getDisplayedFullName();

        // 1. Mở modal và sửa tên nhưng bấm Đóng (✕)
        profilePage.openEditProfileModal();
        profilePage.enterFullName("Ten Moi Chua Luu");
        profilePage.closeEditModal();

        // Kiểm tra thông tin cũ trên trang vẫn nguyên vẹn
        Assert.assertEquals(profilePage.getDisplayedFullName(), originalName, "Thông tin chưa lưu đã bị ghi nhận sau khi đóng modal");

        // 2. Mở lại modal -> dữ liệu trong input phải được reset về giá trị cũ
        profilePage.openEditProfileModal();
        Assert.assertEquals(profilePage.getFullNameInput().inputValue(), originalName, "Dữ liệu nháp chưa lưu vẫn còn lưu trong input");
        profilePage.closeEditModal();
    }
}
