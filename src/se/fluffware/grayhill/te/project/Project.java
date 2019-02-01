package se.fluffware.grayhill.te.project;
import java.util.ArrayList;
import java.util.List;

public class Project {
	enum InterfaceType {
		Invalid,
		CAN,
		USB
	}
	public int defaultScreen = -1;
	public InterfaceType iface = InterfaceType.Invalid;
	public List<Screen> screens = new ArrayList<Screen>();
	
}
