package test.apps;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class test.apps.GToDoActivityTest \
 * test.apps.tests/android.test.InstrumentationTestRunner
 */
public class GToDoActivityTest extends ActivityInstrumentationTestCase2<GToDoActivity> {

    public GToDoActivityTest() {
        super("test.apps", GToDoActivity.class);
    }

}
