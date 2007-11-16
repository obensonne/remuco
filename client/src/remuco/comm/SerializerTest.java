package remuco.comm;

import junit.framework.TestCase;
import remuco.controller.ClientInfo;
import remuco.player.Info;
import remuco.player.Plob;
import remuco.player.PlobList;

public class SerializerTest extends TestCase {

	private Serializer srz;
	
	public void testSerializeISerial() {

		Message m = new Message();
		Message m2 = new Message();

		ClientInfo ci, ci2;

		ci = new ClientInfo(100, 100);

		System.out.println(ci.toString());

		m.id = Message.ID_IFC_CINFO;
		m.sd = ci.sdGet();

		srz.sd2bd(m);

		// m.dumpBin();

		m.sd = null;

		try {
			srz.bd2sd(m);
		} catch (BinaryDataExecption e) {
			System.out.println(e.getMessage());
		}

		ci2 = new ClientInfo(0, 0);
		ci2.sdSet(m.sd);

		System.out.println(ci2.toString());

		assertEquals(ci, ci2);

		/* ---------------- */
		
		Info i = new Info();
		Info i2;
		
		
		m.sd = i.sdGet();
		m.id = Message.ID_IFS_PINFO;
		
		srz.sd2bd(m);
		
		m.dumpBin();
		
		m.sd = null;
		try {
			srz.bd2sd(m);
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}
		
		i2 = new Info();
		i2.sdSet(m.sd);
		
		assertEquals(i, i2);
		
		/* ---------------- */
		
		Plob p, p2;
		
		p = new Plob("1234");
		
		p.setMeta("hallo", "du");
		p.setMeta("ich", null);
		p.setRating(3);
		
		m.id = Message.ID_IFS_CAP;
		m.sd = p.sdGet();
		
		srz.sd2bd(m);
		
		m.dumpBin();
		
		m.sd = null;
		try {
			srz.bd2sd(m);
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}
		
		p2 = new Plob();
		p2.sdSet(m.sd);
		
		assertEquals(p, p2);
		
		/* ---------------- */
		
		PlobList pl, pl2;
		
		pl = new PlobList("1234", "chill");
		
		pl.addPlob("x", "namex", "descx");
		pl.addPlob("y", "namey", "descy");
		pl.addPlob("z", "namez", "descz");

		pl.removePlob(1);
		
		pl.addSubList("s1", "names1");
		pl.addSubList("s2", "names2");
		pl.addSubList("s3", "names3");

		pl.removeSubList("s2");
		
		m.id = Message.ID_IFS_PLAYLIST;
		m.sd = pl.sdGet();
		
		srz.sd2bd(m);
		
		m.dumpBin();
		
		m.sd = null;
		try {
			srz.bd2sd(m);
		} catch (BinaryDataExecption e) {
			fail(e.getMessage());
		}
		
		pl2 = new PlobList();
		pl2.sdSet(m.sd);
		
		assertEquals(pl, pl2);
		
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		srz = new Serializer();

	}

}
