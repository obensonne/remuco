package remuco;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		int val, res, x;
		float f;
		
		res = 80;
		val = 40;
		
		x = (int) ((float) res / 100 * val);
		f = (((float) (res / 100)) * val);
		
		System.out.println(x);
		System.out.println(f);

	}

}
