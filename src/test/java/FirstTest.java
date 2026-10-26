import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FirstTest extends BaseTest {

    @Test
    public void testHomePageLoadsSuccessfully() {
        Assert.assertNotNull(page.url());
        Assert.assertTrue(page.url().contains("yody.vn"));
    }
}