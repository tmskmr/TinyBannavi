package tainavi;

import java.util.Date;
import java.util.GregorianCalendar;

public class TatCount {

	private GregorianCalendar ca = new GregorianCalendar();
	private GregorianCalendar cz = new GregorianCalendar();
	
	public void restart() {
		ca.setTime(new Date());
	}
	
	public double end() {
		cz.setTime(new Date());
		return (cz.getTimeInMillis() - ca.getTimeInMillis())/1000.0D;
	}
	
	public TatCount() {
		restart();
	}
}
