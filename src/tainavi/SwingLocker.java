package tainavi;

import java.awt.Dialog.ModalityType;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.SwingWorker;

/**
 * {@link Locker}を{@link SwingWorker}をつかって実装
 * @since 3.15.4β
 */
public class SwingLocker extends Locker {

	// クラス共有設定
	private static Frame ownerframe = null;
	public static void setOwner(Frame owner) { ownerframe = owner; }

	private JDialog dialog;
	
	@Override
	public void unlock() {
		dialog.setVisible(false);
	}
	
	@Override
	public boolean waitfor() {
		dialog.setVisible(true);
		return true;
	}

	public SwingLocker() {
		
		super();
		
		if ( ownerframe != null ) {
			dialog = new JDialog(ownerframe);	// フォーカスが移動してしまうのが難
		}
		else {
			dialog = new JDialog();
		}
		//dialog.setModal(true);			// モーダル
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);	// 親しかブロックしないモーダル
		dialog.setUndecorated(true);		// 見えないダイアログ
		dialog.setBounds(0,0,0,0);
		dialog.setEnabled(false);
	}
}
