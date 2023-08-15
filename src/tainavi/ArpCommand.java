package tainavi;


public class ArpCommand {

	// キャッシュに乗るまで時間がかかる場合があるので３秒待つ
	// InetAddress#isReachable()は使えない
	
	private static final String PINGWIN = "PING.EXE -n 3 -w 1000 #HOST#";
	private static final String ARPWIN = "ARP.EXE -a #HOST#";
	
	private static final String PINGX = "ping -c 3 -i 1 #HOST#";
	private static final String ARPX = "arp -n #HOST#";
	
	/**
	 * @return 
	 */
	public static String getMac(String host) {
		
		String pingcmd = null;
		String arpcmd = null;
		
		if ( CommonUtils.isWindows() ) {
			pingcmd = PINGWIN;
			arpcmd = ARPWIN;
		}
		else if ( CommonUtils.isLinux() || CommonUtils.isMac() ) {
			pingcmd = PINGX;
			arpcmd = ARPX;
		}
		else {
			return null;
		}
		
		{
			int retval = CommonUtils.executeCommand(pingcmd.replaceFirst("#HOST#", host));
			if ( retval != 0 ) {
				return null;
			}
		}
		{
			int retval = CommonUtils.executeCommand(arpcmd.replaceFirst("#HOST#", host));
			if ( retval != 0 ) {
				return null;
			}
		}
		
		String mac = CommonUtils.getCommandResult();
		if ( mac == null ) {
			return null;
		}
		
		return mac.replaceFirst("^[\\s\\S]*(([0-9a-fA-F][0-9a-fA-F][\\-:]){5}[0-9a-fA-F][0-9a-fA-F])[\\s\\S]*$", "$1").replaceAll("[\\-:]", "").toUpperCase();
	}

}
