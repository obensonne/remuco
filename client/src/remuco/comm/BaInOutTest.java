package remuco.comm;

import junit.framework.TestCase;

public class BaInOutTest extends TestCase {

	public void testWriteStringV() {
		
		String[] sv, sv2 = null;
		

		BaOut bo;
		BaIn bi;

		bo = new BaOut();
		sv = new String[] { "hey", "du", null, "da" };
		
		bo.write(sv, false);
		
		dump(bo.toByteArray());

		bi = new BaIn(bo.toByteArray());

		try {
			sv2 = bi.readStringV(false);
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}
		
		assertEquals(sv.length, sv2.length);
		
		for (int i = 0; i < sv2.length; i++) {
			assertEquals(sv[i], sv2[i]);
		}
		
		
		
	}
	
	public void testWriteInt() {

		int i = 0;

		BaOut bo;
		BaIn bi;

		bo = new BaOut();

		bo.writeInt(0x12345);
		bo.writeInt(0x32345);

		dump(bo.toByteArray());

		bi = new BaIn(bo.toByteArray());

		try {
			i = bi.readInt();
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}
		assertEquals(0x12345, i);

		try {
			i = bi.readInt();
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}
		assertEquals(0x32345, i);
		
	}

	public void testWriteString() {

		String s;

		BaOut bo;
		BaIn bi;

		bo = new BaOut();

		bo.write("hallo");
		bo.write((String) null);
		bo.write("du");
		bo.write((String) null);

		dump(bo.toByteArray());
		
		bi = new BaIn(bo.toByteArray());

		try {
			s = bi.readString();
			assertEquals("hallo", s);
			s = bi.readString();
			assertEquals(null, s);
			s = bi.readString();
			assertEquals("du", s);
			s = bi.readString();
			assertEquals(null, s);
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}

	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	private void dump(byte[] ba) {
		Message m = new Message();
		m.bd = ba;
		m.dumpBin();
	}
	
}
