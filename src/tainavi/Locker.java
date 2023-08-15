package tainavi;

/**
 * ロック処理について、swingの利用を隠ぺいできないかなーと思って作ったinterface
 * @sinc 3.15.4β
 */
abstract class Locker {

	public abstract void unlock(); 
	public abstract boolean waitfor();
	
}
