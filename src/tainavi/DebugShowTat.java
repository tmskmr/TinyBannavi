package tainavi;

public class DebugShowTat {
	private long start,stop;
	
	public void start() {
		start = System.nanoTime();
	}
	public void stop(String s) {
		stop = System.nanoTime();
		System.err.println(s+" [DEBUG] tat="+(stop-start));
	}
}
