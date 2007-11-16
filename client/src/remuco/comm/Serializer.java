package remuco.comm;

import remuco.util.Log;

public final class Serializer {

	/** Internal byte array {@link BaOut}. */
	private final BaOut bos = new BaOut(1024);

	/**
	 * If the size of data to write is not known when the size value must be
	 * written, this attribute stores the position where to write the data size
	 * once it is known.
	 */
	private int markerDataSizePos;

	/**
	 * If the size of data is only known once the data has been written, this
	 * attribute stores the position where the acutal written data started so
	 * that the size can be calculated. once it is known.
	 */
	private int markerDataStart;

	protected Serializer() {
	}

	/**
	 * Converts a message's binary data ({@link Message#bd}) into structured
	 * data ({@link Message#sd}) according the message ID.
	 * <p>
	 * On return {@link Message#bd} is guaranteed to be <code>null</code> and
	 * {@link Message#sd} to be not <code>null</code>.
	 * 
	 * @param m
	 *            the message (for the message's ID must be a non-<code>null</code>
	 *            data structure format vector in {@link Message#DSFVAin} !)
	 * @throws BinaryDataExecption
	 *             if the binary data is malformed (in this case
	 *             {@link Message#sd} is guaranteed to be <code>null</code>)
	 *             and {@link Message#bd} to be not <code>null</code>
	 */
	protected synchronized void bd2sd(Message m) throws BinaryDataExecption {

		Log.asssert(this, m != null && m.sd == null);

		if (m.bd == null)
			return;

		try {
			m.sd = unserialize(Message.DSFVAin[m.id], m.bd);
		} catch (BinaryDataExecption e) {
			m.sd = null;
			throw e;
		}

		m.bd = null;

	}

	/**
	 * Converts the message's structured data ({@link Message#sd}) into binary
	 * data ({@link Message#bd}) according the message ID.
	 * <p>
	 * On return {@link Message#bd} is guaranteed to be not <code>null</code>
	 * and {@link Message#sd} to be <code>null</code>.
	 * 
	 * @param m
	 *            the message (for the message's ID must be a non-<code>null</code>
	 *            data structure format vector in {@link Message#DSFVAout} !)
	 */
	protected synchronized void sd2bd(Message m) {

		Log.asssert(this, m != null && m.bd == null);

		if (m.sd == null)

			m.bd = new byte[] {};

		else

			m.bd = serialize(m.sd, Message.DSFVAout[m.id]);

	}

	/**
	 * Reads type, count and size of next subdata and checks if there is enough
	 * data in <code>bis</code> to read the actual subdata.
	 * 
	 * @param bis
	 *            the {@link BaIn} to read from
	 * @param dt
	 *            the expected data type
	 * @param dc
	 *            the expected data count
	 * @return the size of the following subdata
	 * @throws BinaryDataExecption
	 *             if type, count or size could not be read because the are not
	 *             enough bytes availbale in <code>bis</code> or if there are
	 *             less bytes in <code>bis</code> available than the read size
	 * 
	 */
	private int readAndCheckTypeCountSize(BaIn bis, int dt, int dc)
			throws BinaryDataExecption {

		int size;

		if (bis.read() != dt)
			throw new BinaryDataExecption("data type mismatch");

		if (bis.read() != dc)
			throw new BinaryDataExecption("data count mismatch");

		size = bis.readInt();

		Log.debug("[SR] " + size + "B (" + bis.available() + "B remaining)");

		if (size > bis.available())
			throw new BinaryDataExecption("not enough data");

		return size;

	}

	private byte[] serialize(Object[] dataStruct, int[] dsfv) {

		int[] iv;
		int i, j, n, dt, dc;
		String[] sv;
		String[][] svv;
		byte[][] bav;

		Log.debug("--- SERIALIZE START ---");

		bos.reset();

		Log.asssert(this, dataStruct.length == dsfv.length / 2);

		n = dsfv.length / 2;
		for (i = 0; i < n; i++) {

			dt = dsfv[2 * i];
			dc = dsfv[2 * i + 1];

			Log.debug("[SR] serialize dt " + dt + " (" + dc + " times)");

			serializePrepare(dt, dc);

			switch (dt) {
			case IStructuredData.DT_BA:

				bav = (byte[][]) dataStruct[i];
				Log.asssert(this, bav.length == dc);

				for (j = 0; j < bav.length; j++) {
					bos.writeBa(bav[j]);
				}

				break;
			case IStructuredData.DT_INT:

				iv = (int[]) dataStruct[i];
				Log.asssert(this, iv.length == dc);

				for (j = 0; j < iv.length; j++) {
					bos.writeInt(iv[j]);
				}

				break;
			case IStructuredData.DT_IV:

				Log.ln("[SR] BUG: need IV serialization");
				// TODO: implement IV serialization

				break;
			case IStructuredData.DT_STR:

				sv = (String[]) dataStruct[i];
				Log.asssert(this, sv.length == dc);

				bos.write(sv, false);

				break;
			case IStructuredData.DT_SV:

				svv = (String[][]) dataStruct[i];
				Log.asssert(this, svv.length == dc);

				for (j = 0; j < svv.length; j++) {

					bos.write(svv[j], true);

				}

				break;

			default:

				Log.asssertNotReached(this);
				break;
			}

			serializeFinish();

			// serialize(bos)

		}

		Log.debug("--- SERIALIZE END ---");

		return bos.toByteArray();
	}

	/**
	 * Uses {@link #markerDataSizePos} and {@link #markerDataStart} to calculate
	 * the size of written data and write it at the correct position into
	 * {@link #bos}.
	 * 
	 */
	private void serializeFinish() {

		int size = bos.size() - markerDataStart;

		Log
				.debug("  [SR] finish -  write " + size + " at "
						+ markerDataSizePos);

		bos.writeIntAt(size, markerDataSizePos);

	}

	/**
	 * Writes kind of preamble into {@link #bos}. Once acutal data has been
	 * written into {@link #bos} call {@link #serializeFinish()} to complete
	 * writing.
	 * 
	 * @param dt
	 *            the data type
	 * @param dc
	 *            the number of data elements that will be written into
	 *            {@link #bos}
	 */
	private void serializePrepare(int dt, int dc) {

		Log.debug("  [SR] prepare - write at " + bos.size());

		bos.write(dt);
		bos.write(dc);
		markerDataSizePos = bos.size();
		bos.writeInt(0); // here we will later write the size of the
		// following data - if finishWrite() gets called
		markerDataStart = bos.size();

	}

	private Object[] unserialize(int[] dsfv, byte[] dataBin)
			throws BinaryDataExecption {

		Log.debug("--- UNSERIALIZE START ---");

		int i, j, n, dt, dc, ds;
		Object[] bdv;
		BaIn bis;

		bis = new BaIn(dataBin);

		Log.asssert(this, dsfv.length % 2 == 0);

		n = dsfv.length / 2;

		bdv = new Object[n];

		for (i = 0; i < n; i++) {

			dt = dsfv[2 * i];
			dc = dsfv[2 * i + 1];

			ds = readAndCheckTypeCountSize(bis, dt, dc);

			switch (dt) {
			case IStructuredData.DT_BA:

				byte[][] bav = new byte[dc][];

				for (j = 0; j < bav.length; j++) {

					bav[j] = bis.readBa();

				}

				bdv[i] = bav;

				break;
			case IStructuredData.DT_INT:

				if (ds != 4 * dc)
					throw new BinaryDataExecption("bad data size");

				int[] iv = new int[dc];

				for (j = 0; j < iv.length; j++) {

					iv[j] = bis.readInt();

				}

				bdv[i] = iv;

				break;
			case IStructuredData.DT_IV:

				int[][] ivv = new int[dc][];

				for (j = 0; j < ivv.length; j++) {
					ivv[j] = bis.readIntV();
				}

				bdv[i] = ivv;

				break;
			case IStructuredData.DT_STR:

				String[] sv = bis.readStringV(ds);

				bdv[i] = sv;

				break;
			case IStructuredData.DT_SV:

				String[][] svv = new String[dc][];

				for (j = 0; j < svv.length; j++) {

					svv[j] = bis.readStringV(0);

				}

				bdv[i] = svv;

				break;

			default:

				Log.asssertNotReached(this);
				break;
			}

			// serialize(bos)

		}

		Log.debug("--- UNSERIALIZE END ---");

		return bdv;

	}

}
