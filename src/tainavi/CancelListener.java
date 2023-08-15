package tainavi;

import java.util.EventListener;

public interface CancelListener extends EventListener {

	public void cancelRised(CancelEvent e);

}
