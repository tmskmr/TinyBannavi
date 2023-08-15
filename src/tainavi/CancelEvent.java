package tainavi;

import java.util.EventObject;

public class CancelEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	
	public static enum Cause { TOOLBAR_SEARCH };
	
	// この引数の群れはいいのか？
	public CancelEvent(Object source, Cause cause) {
		super(source);
		this.cause = cause;
	}
	
	private Cause cause;
	
	public Cause getCause() { return cause; }

}
