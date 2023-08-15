package tainavi;

import java.util.concurrent.ExecutionException;

/**
 * スレッドを使ったバックグラウンド処理について、swingの利用を隠ぺいできないかなーと思って作ったinterface
 * @sinc 3.15.4β
 */
public abstract class BackgroundWorker {

	// 抽象メソッド
	protected abstract Object doWorks() throws Exception;	// 主な処理（SwingWorker#doInBackground()）
	protected abstract void doFinally();					// 終了時に１回だけ実行する処理（SwingWorker#done()）
	
	// 必要なメソッド（SwingWorkerのパクリ）
	public abstract void execute();
	public abstract Object get() throws InterruptedException, ExecutionException;
	public abstract boolean cancel(boolean mayInterruptIfRunning);
		
}
