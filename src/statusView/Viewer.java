
package statusView;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Authenticator;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.JComboBox;

import tainavi.RecorderInfo;
import tainavi.RecorderInfoList;

public class Viewer extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JPanel jPanel1 = null;
	private JComboBox jComboBox = null;
	private JPanel jPanel2 = null;
	private JTable jTable = null;
	private JPanel jPanel3 = null;
	private JSlider jSlider = null;
	private Timer jTimer = null;
	
	private final String defFile = "env/StatusView.def";
	
	private RecorderInfo recorder = null;
	
	/**
	 * 鯛ナビのレコーダ一覧の中で"RD-"を含むもののみ抽出したリスト. 
	 */
	private RecorderInfoList recorderList = new RecorderInfoList();
	
	private tainavi.GetRDStatus gs = new tainavi.GetRDStatus();
	
	
	
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BorderLayout());
			jPanel1.add(jComboBox = new JComboBox(), BorderLayout.CENTER);
			
			jComboBox.addItem("");
			for (RecorderInfo re : recorderList) {
				jComboBox.addItem(re.getRecorderIPAddr());
			}
			
			jComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (jTimer != null) {
						jTimer.stop();
					}
					
					jTable.setValueAt("", 0, 1);
					jTable.setValueAt("", 1, 1);
					jTable.setValueAt("", 2, 1);
					jTable.setValueAt("", 3, 1);
					jTable.setValueAt("", 4, 1);
					jTable.setValueAt("", 5, 1);
					jTable.setValueAt("", 6, 1);
					jTable.setValueAt("", 7, 1);
					jTable.setValueAt("", 8, 1);
					jTable.setValueAt("", 9, 1);
					jTable.setValueAt("", 10, 1);
					jTable.setValueAt("", 11, 1);
					jTable.setValueAt("", 12, 1);
					jTable.setValueAt("", 13, 1);
					jTable.setValueAt("", 14, 1);
					jTable.setValueAt("", 15, 1);
					jTable.setValueAt("", 16, 1);
					jTable.setValueAt("", 17, 1);
					jTable.setValueAt("", 18, 1);
					jTable.setValueAt("", 19, 1);
					jTable.setValueAt("", 20, 1);
					
					if (jComboBox.getSelectedItem().equals("")) {
						recorder = null;
						return;
					}
					recorder = recorderList.get(jComboBox.getSelectedIndex()-1);
					
					jTimer = new Timer(1*1000, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String s = gs.getCurChannel(recorder.getRecorderIPAddr());
							if (s != null) {
								jTable.setValueAt(gs.mod, 0, 1);
								jTable.setValueAt(gs.enc, 1, 1);
								jTable.setValueAt(gs.ch, 2, 1);
								jTable.setValueAt(gs.typ, 3, 1);
								jTable.setValueAt(gs.unk, 4, 1);
								jTable.setValueAt(gs.dvd, 5, 1);
								jTable.setValueAt(gs.opn, 6, 1);
								jTable.setValueAt(gs.title_no, 7, 1);
								jTable.setValueAt(gs.title, 8, 1);
								jTable.setValueAt(gs.title_len_s, 9, 1);
								jTable.setValueAt(gs.chapter, 10, 1);
								jTable.setValueAt(gs.chapter_name, 11, 1);
								jTable.setValueAt(gs.time_all_s, 12, 1);
								jTable.setValueAt(gs.time_chap_s, 13, 1);
								jTable.setValueAt(gs.title_chno, 14, 1);
								jTable.setValueAt(gs.title_chname, 15, 1);
								jTable.setValueAt(gs.title_date + " " + gs.title_time, 16, 1);
								jTable.setValueAt(gs.title_gnr, 17, 1);
								jTable.setValueAt(gs.title_chcode, 18, 1);
								jTable.setValueAt(gs.okk, 19, 1);
								jTable.setValueAt(gs.ply, 20, 1);
								
								if (jSlider.getMaximum() != gs.title_len) {
									jSlider.setMaximum(gs.title_len); 
								}
								jSlider.setValue((gs.time_all>=0)?(gs.time_all):(gs.title_len+gs.time_all)); 
								jSlider.updateUI();
							}
							else {
								jTable.setValueAt("切断", 20, 1);
							}
						}
					});
					
					jTimer.start();
				}
			});
		}
		return jPanel1;
	}

	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			
			int row = 21;
			int col = 2;
			
			jPanel2.setLayout(new BorderLayout());
			jPanel2.add(jTable = new JTable(row,col), BorderLayout.CENTER);

			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable.getColumnModel();
			TableColumn column = null;
			column = columnModel.getColumn(0);
			column.setPreferredWidth(100);
			column.setMinWidth(100);
			column.setMaxWidth(100);
			column = columnModel.getColumn(1);
			column.setPreferredWidth(200);

			jTable.setValueAt("表示メニュー", 0, 0);
			jTable.setValueAt("Ｗ録", 1, 0);
			jTable.setValueAt("チャンネル", 2, 0);
			jTable.setValueAt("HDD/DVD", 3, 0);
			jTable.setValueAt("HDD", 4, 0);
			jTable.setValueAt("DVD", 5, 0);
			jTable.setValueAt("トレイ", 6, 0);
			jTable.setValueAt("タイトル番号", 7, 0);
			jTable.setValueAt("タイトル名", 8, 0);
			jTable.setValueAt("長さ", 9, 0);
			jTable.setValueAt("チャプタ番号", 10, 0);
			jTable.setValueAt("チャプタ名", 11, 0);
			jTable.setValueAt("現在位置", 12, 0);
			jTable.setValueAt("チャプタ内位置", 13, 0);
			jTable.setValueAt("CH", 14, 0);
			jTable.setValueAt("チャンネル名", 15, 0);
			jTable.setValueAt("録画日時", 16, 0);
			jTable.setValueAt("ジャンル", 17, 0);
			jTable.setValueAt("放送局コード", 18, 0);
			jTable.setValueAt("追っかけ", 19, 0);
			jTable.setValueAt("動作", 20, 0);
		}
		return jPanel2;
	}
	
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			jPanel3 = new JPanel();
			
			jPanel3.setLayout(new BorderLayout());
			jPanel3.add(jSlider = new JSlider(0,0,0), BorderLayout.CENTER);
			
			jSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (jSlider.getValueIsAdjusting()) {
						jTimer.stop();
						jTable.setValueAt(gs.getHMS(jSlider.getValue()), 12, 1);
					}
					else {
						if ( ! jTimer.isRunning() && ! gs.ply.equals("") && ! gs.ply.equals("録画") && gs.mod.equals("なし")) {
							String HMS = gs.getHMS(jSlider.getValue());
							
							tainavi.HDDRecorderUtils ru = new tainavi.HDDRecorderUtils();
							Authenticator.setDefault(ru.new MyAuthenticator(recorder.getRecorderUser(),recorder.getRecorderPasswd()));
							
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/ts.htm?"+jSlider.getValue(), null);
							/*
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0E", null);
							tainavi.CommonUtils.milSleep(500);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0E", null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0"+HMS.substring(0,1), null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0"+HMS.substring(1,2), null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=C4", null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0"+HMS.substring(3,4), null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0"+HMS.substring(4,5), null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=C4", null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0"+HMS.substring(6,7), null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=0"+HMS.substring(7,8), null);
							tainavi.CommonUtils.milSleep(100);
							ru.reqGET("http://"+recorder.getRecorderIPAddr()+":"+recorder.getRecorderPortNo()+"/remote/remote.htm?key=44", null);
							*/
						}
						jTimer.start();
					}
				}
			});
		}
		return jPanel3;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Viewer thisClass = new Viewer();
				thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.setVisible(true);
			}
		});
	}

	/**
	 * This is the default constructor
	 */
	public Viewer() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		//this.setSize(300, 200);
		
		try {
			final Image image = ImageIO.read(new File("icon/remostat.png"));
			this.setIconImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 設定ファイルを読み込む
		String recip = "";
		Rectangle ra = new Rectangle();
		File f = new File(defFile);
		if ( f.exists() ) {
			try {
				String str = null;
				int i = 0;
				BufferedReader reader = new BufferedReader(new FileReader(f));
				while ((str = reader.readLine()) != null) {
					switch (i++) {
					case 0:
						recip = str;
						break;
					case 1:
						ra.x = Integer.valueOf(str);
						break;
					case 2:
						ra.y = Integer.valueOf(str);
						break;
					case 3:
						ra.width = Integer.valueOf(str);
						break;
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// 鯛ナビと タイニーシンクの設定から
		//tainavi.Env env = tainavi.Env.load();
		RecorderInfoList recInfos = new RecorderInfoList();
		recInfos.load();
		
		ArrayList<taiSync.RecorderInfo> syncrl = taiSync.RecorderInfo.load();
		
		// 必要なレコーダのみ抽出する
		for (RecorderInfo r : recInfos) {
			if ( r.getRecorderIPAddr().equals("") ) {
				continue;
			}
	    	if ( ! r.getRecorderId().contains("RD-") && ! r.getRecorderId().contains("DBR-Z") ) {
	    		continue;
	    	}
	    	
	    	RecorderInfo rn = new RecorderInfo();
	    	if (r.getRecorderIPAddr().equals("127.0.0.1") || r.getRecorderIPAddr().equals("localhost")) {
		    	for (taiSync.RecorderInfo sr : syncrl) {
		    		if (sr.getLocalPort() == Integer.valueOf(r.getRecorderPortNo())) {
		    	    	rn.setRecorderIPAddr(sr.getRecorderIPAddr());
		    	    	rn.setRecorderPortNo(sr.getRecorderPortNo());
				    	rn.setRecorderUser(sr.getRecorderUser());
				    	rn.setRecorderPasswd(sr.getRecorderPasswd());
		    		}
		    	}
	    	}
	    	if (rn.getRecorderIPAddr().equals("")) {
		    	rn.setRecorderIPAddr(r.getRecorderIPAddr());
		    	rn.setRecorderPortNo(r.getRecorderPortNo());
		    	rn.setRecorderUser(r.getRecorderUser());
		    	rn.setRecorderPasswd(r.getRecorderPasswd());
	    	}
	    	rn.setRecorderMacAddr(r.getRecorderMacAddr());
	    	rn.setRecorderBroadcast(r.getRecorderBroadcast());
	    	rn.setRecorderId(r.getRecorderId());
	    	
	    	recorderList.add(rn);
		}
		
		//this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("RDリモートステータスビューア");
		this.pack();
		
		Rectangle ws = this.getBounds();
		ws.x = ra.x;
		ws.y = ra.y;
		if (ra.width > ws.width) {
			ws.width = ra.width;
		}
		this.setBounds(ws);
		
		// 前回選択していたレコーダーを再選択
		if ( ! recip.equals("")) {
			jComboBox.setSelectedItem(recip);
		}
		
		// ウィンドウを閉じたときの処理
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowListener() {

			public void windowActivated(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				final Viewer thisClass = (Viewer) e.getSource();
				Rectangle r = thisClass.getBounds();
				
				File f = new File(defFile);
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(f));
					if (recorder != null) {
						writer.write(recorder.getRecorderIPAddr()+"\n");
					}
					else {
						writer.write("\n");
					}
					writer.write(r.x+"\n");
					writer.write(r.y+"\n");
					writer.write(r.width+"\n");
					writer.write(r.height+"\n");
					writer.close();
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getJPanel1(), BorderLayout.NORTH);
			jContentPane.add(getJPanel2(), BorderLayout.CENTER);
			jContentPane.add(getJPanel3(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}
}
