package tainavi;

import java.util.EventObject;
import java.util.GregorianCalendar;

public class TickTimerRiseEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	
	private final GregorianCalendar calendar;
	
	public GregorianCalendar getCalendar() { return calendar; }
	
	public TickTimerRiseEvent(Object source) {
		super(source);
		this.calendar = new GregorianCalendar();
	}

}
