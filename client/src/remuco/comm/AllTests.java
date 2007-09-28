package remuco.comm;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for remuco.comm");
		//$JUnit-BEGIN$
		suite.addTestSuite(BaInOutTest.class);
		suite.addTestSuite(SerializerTest.class);
		//$JUnit-END$
		return suite;
	}

}
