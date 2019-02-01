package se.fluffware.te_tool;

import java.io.File;
import java.util.prefs.Preferences;

public class UserPrefs {
	static final public String BASE = "/se/fluffware/te_tool";
	static final public String CurrentProjDir = BASE + "/CurrentProjDir";
	static final public String ImportDir = BASE + "/ImportDir";
	static final public String ArchiveDir = BASE + "/ArchiveDir";

	static public File getFile(Preferences prefs, String key) {
		String file_name = prefs.get(key, null);
		if (file_name == null)
			return null;

		return new File(file_name);
	}
}
