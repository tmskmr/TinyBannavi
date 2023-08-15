package tainavi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/**
 * {@link BackgroundWorker}を{@link SwingWorker}をつかって実装
 * @since 3.15.4β
 */
public abstract class SwingBackgroundWorker extends BackgroundWorker {

	public static void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;
	
	// 抽象メソッド
	protected abstract Object doWorks() throws Exception;	// 主な処理
	protected abstract void doFinally();					// 終了時に１回だけ実行する処理
	
	// 本体
	private boolean locking = true;	// falseならブロッキングしなくてもいいよ

	private SwingWorker<Object, Object> worker = null;
	
	/*
	 * コンストラクタ
	 */
	public SwingBackgroundWorker(final boolean locking) {
		this.locking = locking;
	}
	
	/*
	 * メソッド
	 */
	
	public void execute() {
		
		final Locker lock = new SwingLocker();
		
		worker = new SwingWorker<Object, Object>() {
			@Override
			protected Object doInBackground() throws Exception {
				Object retval = doWorks();
				//if (debug) System.out.println("DOWORK");
				return retval;
			}
			
			@Override
			protected void done() {
				
				// サブスレッドで発生するキャッチできない例外をキャッチしたい！うまくトラップできるといいな！
				try {
					get();
				}
				catch (final InterruptedException ex) {
	            	//throw new RuntimeException(ex);
	            	ex.printStackTrace();
	            }
				catch (final ExecutionException ex) {
					//throw new RuntimeException(ex.getCause());
	            	ex.printStackTrace();
	            }
				
				// トラップできるようになったのはいいが、例外発生時に処理を終了できなくなって止まってしまう。困った。
				
				//if (debug) System.out.println("DONE");
				
				doFinally();
				if (locking) lock.unlock();
			}
		};
		
		if (debug) {
			worker.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent e) {
					System.out.println("SwingBackgroundWorker CHANGE "+e.getPropertyName()+"="+e.getOldValue()+"->"+e.getNewValue());
				}
			});
		}
		
		worker.execute();
		
		if (locking) lock.waitfor();
		
		// Semaphoreは、親スレッドが止まってしまってdone()がディスパッチされなくなり動作が停止するので使えない
		// SwingWorker.get()は、Swingのディスパッチが止まってしって画面更新がなくなるのでバックグランド処理をしては使いづらい
		// JDialog.setVisible(true)以外ありえないのか…？
		//
		// JRE6では
		//  dialog.setModal(true);　ではなく
		//  dialog.setModalityType(ModalityType.DOCUMENT_MODAL);　を使うことで
		// 他のダイアログへのブロッキングが発生しない！これはいい！！
		
		// ↑ 今読んだら、なんかJava APIの説明にdialog使えやって書いてあった(^^;;;
	}
	
	public Object get() throws InterruptedException, ExecutionException {
		return worker.get();
	}
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		return worker.cancel(mayInterruptIfRunning);
	}
}
