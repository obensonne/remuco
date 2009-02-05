package remuco.comm;

import remuco.UserException;

public interface IScanner {

	public void cancelScan();

	public void startScan(IScanListener listener)
			throws UserException;

}
