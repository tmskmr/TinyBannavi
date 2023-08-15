package taiSync;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.Timer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import tainavi.DebugPrintStream;
import tainavi.LogViewer;



public class Viewer extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private static Env env = null;
	
	private static String iconf = "icon/taisync.png";
	private static String iconf_alert = "icon/taisync-alert.png";
	private static String iconf_alarm = "icon/taisync-alarm.png";
	
	private JPanel jContentPane = null;
	private JScrollPane jScrollPane = null;
	private JTable jTable = null;
	private JCheckBoxPanel jCBPanel_DebugMode = null;
	private JCheckBoxPanel jCBPanel_AppendDT = null;
	private JSliderPanel jSPanel_Period = null;
	private JSliderPanel jSPanel_Keeping = null;
	private JSliderPanel jSPanel_KeepingCheckInterval = null;
	private JSliderPanel jSPanel_PopPort = null;
	private JSliderPanel jSPanel_SmtpPort = null;
	private JButton jButton = null;

	/**
	 * 	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			jScrollPane.setViewportView(getJTable());
			
			Dimension dh = getJTable().getTableHeader().getPreferredSize();
			Dimension db = getJTable().getPreferredSize();
			jScrollPane.setPreferredSize(new Dimension(dh.width,dh.height+db.height));
		}
		return(jScrollPane);
	}
	
	private JTable getJTable() {
		if (jTable == null) {
			String[] colname = {"IP", "PORT", "RDPASS", "ID", "PASS", "WAIT"};
			int[] colwidth = {100, 50, 100, 100, 100, 50};
			
			DefaultTableModel model = new DefaultTableModel(colname, 5);
			jTable = new JTable(model);
			jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
	        DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable.getColumnModel();
	        for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
	        	TableColumn column = columnModel.getColumn(i);
	        	column.setPreferredWidth(colwidth[i]);
	        }
	        
	        JPasswordField password = new JPasswordField();
	        TableCellEditor editor = new DefaultCellEditor( password );
	        jTable.getColumn("PASS").setCellEditor( editor );
	        TableCellRenderer renderer = new DefaultTableCellRenderer() {

				private static final long serialVersionUID = 1L;

				@Override
	    		protected void setValue(Object value) {
	    			if (value != null && ((String)value).length() > 0) {
	    				setText( "●●●●●●●●" );
	    			}
	    			else {
	    				setText( "" );
	    			}
	    		}
	    	};
	        jTable.getColumn("PASS").setCellRenderer( renderer );
	        
	        ArrayList<RecorderInfo> tmpInfo = RecorderInfo.load();	// まあ、いいか…
	        int row = 0;
	        for (RecorderInfo rec : tmpInfo) {
	        	jTable.setValueAt(rec.getRecorderIPAddr(), row, 0);
	        	jTable.setValueAt(rec.getRecorderPortNo(), row, 1);
	        	jTable.setValueAt(rec.getRecorderBroadcast(), row, 2);
	        	jTable.setValueAt(rec.getRecorderUser(), row, 3);
	        	jTable.setValueAt(rec.getRecorderPasswd(), row, 4);
	        	jTable.setValueAt(String.valueOf(rec.getLocalPort()), row, 5);
	        	++row;
	        }
		}
		return jTable;
	}

	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton("設定");
			
			jButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					jButton.setEnabled(false);
					
					ArrayList<RecorderInfo> rl = new ArrayList<RecorderInfo>();
					for (int row=0; row<jTable.getRowCount(); row++) {
						RecorderInfo rec = new RecorderInfo();
						int cnt = 0;
						for (int col=0; col<jTable.getColumnCount(); col++) {
							if ( jTable.getValueAt(row, col) != null && ! jTable.getValueAt(row, col).equals("")) {
								++cnt;
							}
						}
						if (cnt == 0) {
							continue;
						}
						if (cnt != jTable.getColumnCount()) {
							JOptionPane.showMessageDialog(null, "枠は全部埋めてください");
							jButton.setEnabled(true);
							return;
						}
						try {
							rec.setRecorderIPAddr((String) jTable.getValueAt(row, 0));
							InetAddress.getByName(rec.getRecorderIPAddr());
							
							rec.setRecorderPortNo((String) jTable.getValueAt(row, 1));
							Integer.valueOf(rec.getRecorderPortNo());
							
							rec.setRecorderBroadcast((String) jTable.getValueAt(row, 2));
							
							rec.setRecorderUser((String) jTable.getValueAt(row, 3));
							
							rec.setRecorderPasswd((String) jTable.getValueAt(row, 4));
							
							rec.setLocalPort(Integer.valueOf((String) jTable.getValueAt(row, 5)));
							
							rl.add(rec);
							
						} catch (UnknownHostException e1) {
							JOptionPane.showMessageDialog(null, "IPが不正です");
							jButton.setEnabled(true);
							return;
						} catch (NumberFormatException e1) {
							JOptionPane.showMessageDialog(null, "数値の形式が不正です");
							jButton.setEnabled(true);
							return;
						}
					}
					
					RecorderInfo.save(rl);
					
					//
					Env n = new Env();
					n.setDebug(jCBPanel_DebugMode.isSelected());
					n.setAppendDT(jCBPanel_AppendDT.isSelected());
					n.setPeriod(jSPanel_Period.getValue());
					n.setKeeping(jSPanel_Keeping.getValue());
					n.setKeepingCheckInterval(jSPanel_KeepingCheckInterval.getValue());
					n.setPopPort(jSPanel_PopPort.getValue());
					n.setSmtpPort(jSPanel_SmtpPort.getValue());
					n.save();
					
					//
					JOptionPane.showMessageDialog(null, "設定を保存したので再起動して下さい");
					return;
				}
			});
		}
		return jButton;
	}

	/**
	 * @param args
	 */
	
	private static final String logfile = "log_taiSync.txt";
	
	public static void main(String[] args) {
		
		// 
		String serveraddr = null;
		
		// コマンドラインオプションを処理する
		int flag = 0;
		for (String arg : args) {
			switch (flag) {
			case 0:
				if (arg.compareTo("-serveraddr") == 0) {
					flag = 1;
				}
				break;
			case 1:
				serveraddr = arg;
				flag = 0;
				break;
			}
		}
		
		// 環境設定をする
		env = Env.load();
		
		// 標準出力・エラーをリダイレクトする
		System.setOut(new DebugPrintStream(System.out,logfile,true));
		System.setErr(new DebugPrintStream(System.err,logfile,env.getDebug()));
		
		// 起動ログ
		System.out.println("タイニーシンクが起動しました。(VersionInfo:"+VersionInfo.getVersion()+")");
		
		// アイコンファイルはあるかな？
		if ( ! new File(iconf).exists() || ! new File(iconf_alert).exists() || ! new File(iconf_alarm).exists()) {
			String msg = "アイコンファイルがみつかりません。タイニーシンクを終了します。";
			System.out.println(msg);
			JOptionPane.showConfirmDialog(null, msg, "タイニーシンク", JOptionPane.CLOSED_OPTION);
			return;
		}
		
		// サーバースレッドを起動する
		final ArrayList<ReserveInfo> rsvInfo = ReserveInfo.load();	// スレッド間同期用オブジェクトにするよ
		final ArrayList<RecorderInfo> recInfo = RecorderInfo.load();
		final ReserveCtrl rCtrl = new ReserveCtrl();
		
		ServerSocket sock = null;

		// お知らせ
		if (serveraddr != null) {
			System.out.println("POP/SMTP接続待ち受けポートをアドレス"+serveraddr+"にバインドします");
		}
		
		// POPサーバを立ち上げる
		try {
			System.out.println("RDからのPOP接続をポート"+env.getPopPort()+"で待ちます");
			if (serveraddr == null) {
				sock = new ServerSocket(env.getPopPort());
			}
			else {
				sock = new ServerSocket(env.getPopPort(), 0, InetAddress.getByName(serveraddr));
			}
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showConfirmDialog(null, "POPポートが開けません。タイニーシンクを終了します。", "タイニーシンク", JOptionPane.CLOSED_OPTION);
			return;
		}
		final POPServer popserver = new POPServer(sock, recInfo, rsvInfo, env);
		
		// SMTPサーバを立ち上げる
		try {
			System.out.println("RDからのSMTP接続をポート"+env.getSmtpPort()+"で待ちます");
			if (serveraddr == null) {
				sock = new ServerSocket(env.getSmtpPort());
			}
			else {
				sock = new ServerSocket(env.getSmtpPort(), 0, InetAddress.getByName(serveraddr));
			}
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showConfirmDialog(null, "SMTPポートが開けません。タイニーシンクを終了します。", "タイニーシンク", JOptionPane.CLOSED_OPTION);
			return;
		}
		final SMTPServer smtpserver = new SMTPServer(sock, recInfo, rsvInfo);
		
		// HTTPサーバを立ち上げる
		for (RecorderInfo rec : recInfo) {
			try {
				System.out.println("鯛ナビからのHTTP接続をポート"+rec.getLocalPort()+"で待ちます");
				sock = new ServerSocket(rec.getLocalPort());
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showConfirmDialog(null, "HTTPポートが開けません。タイニーシンクを終了します。", "タイニーシンク", JOptionPane.CLOSED_OPTION);
				return;
			}
			new HTTPServer(sock, rec, rsvInfo);
		}
		
		// GUIを起動する
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final Viewer thisClass = new Viewer();
				
				try {
					final Image image = ImageIO.read(new File(iconf));
					final Image image2 = ImageIO.read(new File(iconf_alert));
					final Image image3 = ImageIO.read(new File(iconf_alarm));
					thisClass.setIconImage(image);
	
					final boolean isSupportedSystemTray = SystemTray.isSupported();
					final SystemTray tray = (isSupportedSystemTray)?(SystemTray.getSystemTray()):(null);
					final TrayIcon icon = new TrayIcon(image,"taiSync");
					
					if (isSupportedSystemTray) {
						// システムトレイが有効なら「閉じる」ボタンで隠れる
						thisClass.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
							
						PopupMenu popup = new PopupMenu();
						{
							MenuItem item = new MenuItem("設定ウィンドウを開く");
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									thisClass.setVisible(true);
								}
							});
							popup.add(item);
						}
						if (Desktop.isDesktopSupported()) {
							MenuItem item = new MenuItem("ログファイルを開く");
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									LogViewer lv = new LogViewer(logfile);
									lv.setVisible(true);
									/*
									Desktop desktop = Desktop.getDesktop();
									try {
										desktop.browse(new URI(logfile));
									} catch (UnsupportedOperationException e1) {
										e1.printStackTrace();
									} catch (URISyntaxException e1) {
										e1.printStackTrace();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
									*/
								}
							});
							popup.add(item);
						}
						{
							MenuItem item = new MenuItem("終了する");
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									System.out.println("タイニーシンクを終了します");
									// スレッド同期待ち
									synchronized(rsvInfo) {
										tray.remove(icon);
										System.exit(0);
									}
								}
							});
							popup.add(item);
						}
						icon.setPopupMenu(popup);
						tray.add(icon);
					}
					else {
						// システムトレイが無効なら「閉じる」ボタンで終了
						thisClass.addWindowListener(new WindowAdapter() {
							@Override
							public void windowClosing(WindowEvent e) {
								System.out.println("タイニーシンクを終了します");
								// スレッド同期待ち
								synchronized(rsvInfo) {
									System.exit(0);
								}
							}
						});
					}
					
					/*
					 *  定期的にＲＤとの同期状況を確認する
					 */
					Timer timer = new Timer(1*1000, new ActionListener() {
						private int iconMode = 1;
						private boolean isTriggered = false;
						private boolean isPopFaulted = false;
						private Date prevRefreshed = null;
						private Date prevChecked = null;
						private Date curDate = null;
						
						@Override
						public void actionPerformed(ActionEvent e) {
							
							// 定期的な予約リストのリフレッシュ（ｎ時間に一回）
							if (curDate == null || (curDate.getTime()-prevRefreshed.getTime()) >= env.getKeepingCheckInterval()*3600*1000) {
								curDate = new Date();
								synchronized(rsvInfo) {
									System.out.println("予約リストをリフレッシュします("+curDate.toString()+")");
									rCtrl.refreshReserveStartEnd(null,rsvInfo);
									isTriggered = rCtrl.isThereUnprocessing(null,rsvInfo,env.getKeeping());
									if (isTriggered) {
										String msg = "予約の滞留が発生しています";
										System.out.println(msg);
										if (isSupportedSystemTray) icon.displayMessage(null, msg, MessageType.WARNING);
									}
								}
								prevRefreshed = (Date) curDate.clone();
								prevChecked = (Date) curDate.clone();
							}
							else {
								curDate = new Date();
							}
							
							//
							if (popserver.isFatal()) {
								// bindエラー
								System.out.println("【警告】タイニーシンクを強制終了します");
								if (isSupportedSystemTray) tray.remove(icon);
								synchronized(rsvInfo) {
									System.exit(0);
								}
							}
							else if (popserver.isFault() || smtpserver.isFault()) {
								// RDがリクエストを受け付けない
								if (iconMode == 1) {
									thisClass.setIconImage(image2);
									if (isSupportedSystemTray) icon.setImage(image2);
									iconMode = 2;
								}
								else {
									thisClass.setIconImage(image);
									if (isSupportedSystemTray) icon.setImage(image);
									iconMode = 1;
								}
								//
								if ( ! isPopFaulted && ! popserver.isFault()) {
									isPopFaulted = true;
									if (isSupportedSystemTray) icon.displayMessage(null, "予約が受け付けられませんでした", MessageType.WARNING);
								}
							}
							else if (isTriggered){
								// RDと情報の同期がとれていない
								if (iconMode == 1) {
									thisClass.setIconImage(image3);
									if (isSupportedSystemTray) icon.setImage(image3);
									iconMode = 2;
								}
								else {
									thisClass.setIconImage(image);
									if (isSupportedSystemTray) icon.setImage(image);
									iconMode = 1;
								}

								// 予約エントリが全部はけたかどうか定期的に確認（３０秒に一回）
								if ((curDate.getTime()-prevChecked.getTime()) >= 30*1000) {
									synchronized(rsvInfo) {
										//System.out.println("予約が滞留していないか確認します");
										isTriggered = rCtrl.isThereUnprocessing(null,rsvInfo,env.getKeeping());
										if ( ! isTriggered) {
											String msg = "予約の滞留が解消しました";
											System.out.println(msg);
											//if (isSupportedSystemTray) icon.displayMessage(null, msg, MessageType.INFO);
										}
									}
									prevChecked = (Date) curDate.clone();
								}
							}
							else {
								// 普通の状態にもどった
								if (iconMode != 1) {
									thisClass.setIconImage(image);
									if (isSupportedSystemTray) icon.setImage(image);
									iconMode = 1;
								}
								//
								isPopFaulted = true;
							}
						}
					});
					
					timer.start();
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (AWTException e) {
					e.printStackTrace();
				}
				
				thisClass.setResizable(false);
				thisClass.setVisible(true);
				thisClass.pack();
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
		this.setTitle(VersionInfo.getVersion());
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();

			jContentPane.setLayout(new BoxLayout(jContentPane,BoxLayout.Y_AXIS));
			jContentPane.add(getJScrollPane());
			jContentPane.add(jSPanel_PopPort = new JSliderPanel("番のポートでRDからのPOP接続を待ちます", 1, 1024));
			jContentPane.add(jSPanel_SmtpPort = new JSliderPanel("番のポートでRDからのSMTP接続を待ちます", 1, 1024));
			jContentPane.add(jSPanel_KeepingCheckInterval = new JSliderPanel("時間ごとに未同期予約の滞留チェックを行います", 1, 24));
			jContentPane.add(jSPanel_Keeping = new JSliderPanel("日以内に開始する未同期の予約があると警告します", 1, 8));
			jContentPane.add(jSPanel_Period = new JSliderPanel("日先の予約まで自動登録します", 1, 8));
			jContentPane.add(jCBPanel_AppendDT = new JCheckBoxPanel("繰り返し予約の予約名の末尾に日付を付加します"));
			jContentPane.add(jCBPanel_DebugMode = new JCheckBoxPanel("デバッグログを出力します"));
			jContentPane.add(getJButton());

			// 配置の設定
			for (Component comp : jContentPane.getComponents()) {
				((JComponent)comp).setAlignmentX(JComponent.LEFT_ALIGNMENT);
			}
			
			// 初期値の設定
			jCBPanel_DebugMode.setSelected(env.getDebug());
			jCBPanel_AppendDT.setSelected(env.getAppendDT());
			jSPanel_Period.setValue(env.getPeriod());
			jSPanel_Keeping.setValue(env.getKeeping());
			jSPanel_KeepingCheckInterval.setValue(env.getKeepingCheckInterval());
			jSPanel_PopPort.setValue(env.getPopPort());
			jSPanel_SmtpPort.setValue(env.getSmtpPort());
		}
		return jContentPane;
	}
	
	
	
	// 独自のクラス
	private class JCheckBoxPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private JCheckBox jcheckbox = null;
		
		public JCheckBoxPanel(String s) {
			this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
			this.add(jcheckbox = new JCheckBox());
			this.add(new JLabel(s));
		}
		
		public boolean isSelected() {
			return jcheckbox.isSelected();
		}
		public void setSelected(boolean b) {
			jcheckbox.setSelected(b);
		}
	}
	
	private class JSliderPanel extends JPanel {
		private JSlider jslider = null;
		private JLabel jlabel = null;
		private String lstr = null;
		
		public JSliderPanel(String s, int min, int max) {
			this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
			this.add(jslider = new JSlider(min,max));
			this.add(jlabel = new JLabel(lstr = s));
			
			// スライダーを短くする
			Dimension d = jslider.getPreferredSize();
			d.width = 100;
			jslider.setMaximumSize(d);
			
			jslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					jlabel.setText(String.format("%d %s",jslider.getValue(),lstr));
				}
			});
		}
		
		public int getValue() {
			return jslider.getValue();
		}
		public void setValue(int n) {
			jslider.setValue(n);
		}
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
