package tainavi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VersionInfo {
	private static final String Version = "タイニー番組ナビゲータ for DBR-T2007　3.22.18β+1.13.7";

	private static final String OSname = System.getProperty("os.name");
	private static final String OSvers = System.getProperty("os.version");
	//private static final String OSarch = System.getProperty("sun.arch.data.model", "?");
	private static final String VMvers = System.getProperty("java.version");
	// XXX なんと！System.getProperty("os.arch")ではOS.archでなくJRE.archが返る！
	private static final String VMarch = System.getProperty("os.arch");
	private static final String VMvend = System.getProperty("java.vendor");


	public static String getVersion() {
		return(Version);
	}

	public static String getEnvironment() {
		String osarch = "";
		if ( CommonUtils.isWindows() ) {
			osarch = (System.getenv("ProgramFiles(x86)") != null) ? "x64" : "x86";
		}
		else {
			CommonUtils.executeCommand("arch");
			String oa = CommonUtils.getCommandResult();
			if ( oa != null ) {
				osarch = Pattern.compile("[\r\n]+$",Pattern.DOTALL).matcher(oa).replaceFirst("");
			}
		}
		return(String.format("%s %s (%s) & Java %s (%s) [%s]",OSname,OSvers,osarch,VMvers,VMarch,VMvend));
	}

	public static String getVersionNumber() {

		Matcher ma = Pattern.compile("([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(.)?").matcher(Version);
		if (ma.find()) {
			if (ma.group(4) == null) {
				return String.format("%03d_%03d", Integer.valueOf(ma.group(1)), Integer.valueOf(ma.group(2)));
			}
			else {
				String ab = "b";
				if (ma.group(5) != null && ma.group(5).equals("α")) {
					ab = "a";
				}
				return String.format("%03d_%03d_%03d%s", Integer.valueOf(ma.group(1)), Integer.valueOf(ma.group(2)), Integer.valueOf(ma.group(4)),ab);
			}
		}

		return null;
	}
}
