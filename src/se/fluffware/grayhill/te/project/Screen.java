package se.fluffware.grayhill.te.project;

import java.util.ArrayList;
import java.util.List;

public class Screen {
	static public String indexToString(int index) {
		return (index & 0xff) + "." + (index >> 8);
	}

	public int index;
	public List<Variable> vars = new ArrayList<Variable>();
	public List<Widget> widgets = new ArrayList<Widget>();
	public List<Event> events = new ArrayList<Event>();
}