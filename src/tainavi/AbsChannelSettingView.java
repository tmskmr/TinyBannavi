package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;

/**
 * CH設定のタブ
 * @since 3.15.4β　{@link Viewer}から分離
 */
public abstract class AbsChannelSettingView extends JScrollPane {

	private static final long serialVersionUID = 1L;

	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;

	
	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/
	
	protected abstract Env getEnv();
	protected abstract TVProgramList getProgPlugins();
	
	protected abstract StatusWindow getStWin(); 
	protected abstract StatusTextArea getMWin();
	
	protected abstract Component getParentComponent();
	protected abstract VWColorChooserDialog getCcWin(); 
	
	protected abstract void ringBeep();
	
	/**
	 * 放送局の選択を変更したので反映してほしい
	 */
	protected abstract void updateProgPlugin();

	
	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/
	
	private final Env env = getEnv();
	private final TVProgramList progPlugins = getProgPlugins();
	
	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト
	
	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト
	private final VWColorChooserDialog ccwin = getCcWin();	// これは起動時に作成されたまま変更されないオブジェクト
	
	// メソッド
	//private void StdAppendMessage(String message) { System.out.println(message); }
	//private void StdAppendError(String message) { System.err.println(message); }
	private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }

	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	//private static final int BLOCK_SEP_HEIGHT = 75;

	private static final int UPDATE_WIDTH = 250;
	private static final int HINT_WIDTH = 750;
	
	private static final int PANEL_WIDTH = PARTS_WIDTH+100;

	private static final String TEXT_HINT =
			"[CHソート設定] 放送局の並べ替えはあちらを利用してください。ここでも多少できなくはないですが、番組表をまたげません。\n"+
			"[CHコンバート設定] 番組表を切り替えても検索条件の放送局名を変えたくない方、しょぼかる連携したい方は変換規則を設定してください。";
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント
	
	private JPanel jPanel_chSetting = null;
	
	private JPanel jPanel_update = null;
	private JButton jButton_update = null;
	
	private ChannelSettingPanel tvp = null;
	private ChannelSettingPanel csp = null;
	private ChannelSettingPanel cs2p = null;
	//private ChannelSettingPanel radp = null;
	//private ChannelSettingPanel syobo = null;

	private JTextAreaWithPopup jta_help = null;

	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	public AbsChannelSettingView() {
		
		super();
		
		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.setColumnHeaderView(getJPanel_update());
		this.setViewportView(getJPanel_chSetting());
		
	}
	
	private JPanel getJPanel_update() {
		if (jPanel_update == null)
		{
			jPanel_update = new JPanel();
			jPanel_update.setLayout(new SpringLayout());
			
			jPanel_update.setBorder(new LineBorder(Color.GRAY));
			
			int y = SEP_HEIGHT;
			CommonSwingUtils.putComponentOn(jPanel_update, getJButton_update("更新を確定する"), UPDATE_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			
			int yz = SEP_HEIGHT/2;
			int x = UPDATE_WIDTH+50;
			CommonSwingUtils.putComponentOn(jPanel_update, getJta_help(), HINT_WIDTH, PARTS_HEIGHT+SEP_HEIGHT, x, yz);
			
			y += (PARTS_HEIGHT + SEP_HEIGHT);
			
			// 画面の全体サイズを決める
			Dimension d = new Dimension(PANEL_WIDTH,y);
			jPanel_update.setPreferredSize(d);
		}
		return jPanel_update;
	}
	
	private JPanel getJPanel_chSetting() {
		if (jPanel_chSetting == null) {
			
			jPanel_chSetting = new JPanel();
			jPanel_chSetting.setLayout(new SpringLayout());
			
			//
			Dimension pd;
			
			int y = SEP_HEIGHT;
			
			tvp = new ChannelSettingPanel("地上波＆ＢＳ番組表",progPlugins.getTvProgPlugins(),env.getTVProgramSite(),true,ccwin,StWin,parent);
			pd = tvp.getPreferredSize();
			CommonSwingUtils.putComponentOn(jPanel_chSetting, tvp, pd.width, pd.height, SEP_WIDTH, y);
			y+=(pd.height+SEP_HEIGHT);
			
			csp = new ChannelSettingPanel("ＣＳ[プライマリ]番組表",progPlugins.getCsProgPlugins(),env.getCSProgramSite(),false,ccwin,StWin,parent);
			pd = csp.getPreferredSize();
			CommonSwingUtils.putComponentOn(jPanel_chSetting, csp, pd.width, pd.height, SEP_WIDTH, y);
			y+=(pd.height+SEP_HEIGHT);
			
			cs2p = new ChannelSettingPanel("ＣＳ[セカンダリ]番組表",progPlugins.getCs2ProgPlugins(),env.getCS2ProgramSite(),false,ccwin,StWin,parent);
			pd = cs2p.getPreferredSize();
			CommonSwingUtils.putComponentOn(jPanel_chSetting, cs2p, pd.width, pd.height, SEP_WIDTH, y);
			y+=(pd.height+SEP_HEIGHT);

			/*
			if ( progPlugins.getRadioProgPlugins().size() > 0 ) {
				y+=(pd.height+SEP_HEIGHT);
				radp = new ChannelSettingPanel("ラジオ番組表",progPlugins.getRadioProgPlugins(),env.getRadioProgramSite(),"",true,ccwin,StWin);
				pd = radp.getPreferredSize();
				CommonSwingUtils.putComponentOn(jPanel_chSetting, radp, pd.width, pd.height, SEP_WIDTH, y);
				y+=(pd.height+SEP_HEIGHT);
			}
			*/
			
			y += SEP_HEIGHT;
			
			Dimension d = tvp.getPreferredSize();
			d.width += SEP_WIDTH*2;
			d.height = y;
			
			jPanel_chSetting.setPreferredSize(d);
		}
			
		return jPanel_chSetting;
	}

	
	/*******************************************************************************
	 * アクション
	 ******************************************************************************/
	
	private void updateChSetting() {
		
		TatCount tc = new TatCount();
		
		MWin.appendMessage("【CH設定】設定を保存します");
		StWin.clear();
		
		new SwingBackgroundWorker(false) {
			
			@Override
			protected Object doWorks() throws Exception {
				env.setTVProgramSite((String) tvp.getSelectedCenter());
				env.setCSProgramSite((String) csp.getSelectedCenter());
				env.setCS2ProgramSite((String) cs2p.getSelectedCenter());
				//if (radp!=null) env.setRadioProgramSite((String) radp.getSelectedCenter());

				tvp.saveChannelSetting();
				csp.saveChannelSetting();
				cs2p.saveChannelSetting();

				updateProgPlugin();

				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
			
		}.execute();
			
		StWinSetLocationCenter(parent);
		StWinSetVisible(true);
		
		MWin.appendMessage(String.format("【CH設定】更新が完了しました。所要時間： %.2f秒",tc.end()));
	}

	
	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
	
	private JButton getJButton_update(String s) {
		if (jButton_update == null) {
			
			jButton_update = new JButton(s);
			
			jButton_update.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateChSetting();
				}
			});
		}
		return(jButton_update);
	}
	
	//
	private JTextAreaWithPopup getJta_help() {
		if ( jta_help == null ) {
			jta_help = CommonSwingUtils.getJta(this,2,0);
			jta_help.append(TEXT_HINT);
		}
		return jta_help;
	}
}
