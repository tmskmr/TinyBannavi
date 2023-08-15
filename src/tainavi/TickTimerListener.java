package tainavi;

import java.util.EventListener;

public interface TickTimerListener extends EventListener {
	
	public void timerRised(TickTimerRiseEvent e);
	
}
