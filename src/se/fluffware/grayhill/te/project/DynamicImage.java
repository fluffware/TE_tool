package se.fluffware.grayhill.te.project;
import java.util.List;
import java.util.ArrayList;

public class DynamicImage extends WidgetXY {
	
	static public class SubImage {
		public int x;
		public int y;
		public int width;
		public int height;
	}
	
	static public class State {
		public int index;
		public SubImage selected;
		public SubImage unselected;
	}
	
	public DynamicImage()
	{
		states = new ArrayList<State>();
	}
	public int valueID; 
	public List<State> states;
	public String filename;
}
