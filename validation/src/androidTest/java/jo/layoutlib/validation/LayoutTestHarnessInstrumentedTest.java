package jo.layoutlib.validation;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests instrumentés du {@link LayoutTestHarness}.
 *
 * <p>Ces tests exécutent réellement les 50 layouts du catalogue à travers
 * le pipeline complet du mini-layoutlib (inflation + measure + layout) sur
 * un device Android. Ils valident que :</p>
 *
 * <ul>
 *   <li>Au moins 80% des layouts s'inflent sans erreur</li>
 *   <li>Le temps moyen par layout est &lt; 200 ms</li>
 *   <li>Les layouts complexes (écran de login, dashboard) fonctionnent</li>
 *   <li>Le rapport HTML peut être généré et écrit sur disque</li>
 * </ul>
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class LayoutTestHarnessInstrumentedTest {

    private Context context;
    private LayoutTestHarness harness;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        harness = new LayoutTestHarness(context);
        harness.setDefaultDimensions(1080, 1920);
    }

    @Test
    public void runTest_simpleTextView_succeeds() {
        LayoutTestResult result = harness.runTest("simple",
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:text=\"Hello\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>");
        assertTrue("L'inflation devrait réussir : " + result.getErrorMessage(),
                result.isSuccess());
        assertEquals(1, result.getViewCount());
        assertTrue(result.getMeasuredWidth() > 0);
        assertTrue(result.getMeasuredHeight() > 0);
    }

    @Test
    public void runTest_invalidXml_fails() {
        LayoutTestResult result = harness.runTest("invalid", "<TextView><unclosed");
        assertTrue("Le test devrait échouer", !result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    public void runTest_emptyXml_fails() {
        LayoutTestResult result = harness.runTest("empty", "");
        assertTrue(!result.isSuccess());
    }

    @Test
    public void runTest_nullXml_fails() {
        LayoutTestResult result = harness.runTest("null", null);
        assertTrue(!result.isSuccess());
    }

    @Test
    public void runTest_nestedLinearLayout_succeeds() {
        LayoutTestResult result = harness.runTest("nested",
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:orientation=\"vertical\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"match_parent\">"
                        + "<TextView android:text=\"A\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"B\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>");
        assertTrue(result.isSuccess());
        assertEquals(3, result.getViewCount());  // LinearLayout + 2 TextView
        assertEquals(2, result.getMaxDepth());
    }

    @Test
    public void runTest_deeplyNested_succeeds() {
        LayoutTestResult result = harness.runTest("deep",
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:orientation=\"vertical\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Deep\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout></LinearLayout></LinearLayout>");
        assertTrue(result.isSuccess());
        assertEquals(4, result.getViewCount());
        assertEquals(4, result.getMaxDepth());
    }

    @Test
    public void runTest_loginScreen_succeeds() {
        List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
        LayoutTestCase loginTest = tests.stream()
                .filter(t -> t.getName().contains("login"))
                .findFirst()
                .orElse(null);
        assertNotNull("Test de login devrait exister", loginTest);
        LayoutTestResult result = harness.runTest(loginTest.getName(), loginTest.getXml());
        assertTrue("Login devrait réussir : " + result.getErrorMessage(),
                result.isSuccess());
        assertTrue("Devrait avoir > 5 vues", result.getViewCount() > 5);
    }

    @Test
    public void runTest_dashboardScreen_succeeds() {
        List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
        LayoutTestCase dashboardTest = tests.stream()
                .filter(t -> t.getName().contains("dashboard"))
                .findFirst()
                .orElse(null);
        assertNotNull(dashboardTest);
        LayoutTestResult result = harness.runTest(
                dashboardTest.getName(), dashboardTest.getXml());
        assertTrue("Dashboard devrait réussir : " + result.getErrorMessage(),
                result.isSuccess());
    }

    @Test
    public void runAllTests_atLeast80PercentSuccess() {
        List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
        List<LayoutTestResult> results = harness.runTests(tests);
        long success = results.stream().filter(LayoutTestResult::isSuccess).count();
        double rate = (double) success / results.size() * 100;
        assertTrue(
                "Taux de succès " + String.format("%.1f%%", rate) + " < 80%",
                rate >= 80);
    }

    @Test
    public void runAllTests_averageTimeUnder200ms() {
        List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
        List<LayoutTestResult> results = harness.runTests(tests);
        double avgTime = results.stream()
                .filter(LayoutTestResult::isSuccess)
                .mapToLong(LayoutTestResult::getTotalTimeMs)
                .average()
                .orElse(0);
        assertTrue(
                "Temps moyen " + String.format("%.1f ms", avgTime) + " >= 200ms",
                avgTime < 200);
    }

    @Test
    public void generateHtmlReport_canBeWrittenToFile() throws IOException {
        List<LayoutTestCase> tests = LayoutTestCatalog.getAllTests();
        List<LayoutTestResult> results = harness.runTests(tests);
        String html = ValidationReportGenerator.generateHtml(results);

        File reportFile = new File(context.getCacheDir(),
                "validation-report-" + System.currentTimeMillis() + ".html");
        try (FileOutputStream fos = new FileOutputStream(reportFile)) {
            fos.write(html.getBytes("UTF-8"));
        }
        assertTrue("Le rapport devrait être écrit", reportFile.exists());
        assertTrue("Le rapport devrait être non vide", reportFile.length() > 0);
    }
}
