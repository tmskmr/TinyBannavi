package tainavi;

import java.util.concurrent.Semaphore;

/**
 * {@link Locker}を{@link Semaphore}をつかって実装。しかし使えなかった。
 * @since 3.15.4β
 */
public class SemLocker extends Locker {

	private Semaphore sem;
	
	public void unlock() {
		sem.release(1);
	}
	
	public boolean waitfor() {
		try {
			sem.acquire();
			return true;
		}
		catch (InterruptedException e) {
			System.err.println("[ERROR] Semaphore: "+e.toString());
		}
		return false;
	}

	public SemLocker() {
		super();
		sem = new Semaphore(0);
	}

}
