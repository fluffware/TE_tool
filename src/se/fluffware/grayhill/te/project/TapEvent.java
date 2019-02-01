package se.fluffware.grayhill.te.project;

public class TapEvent extends Event
{
	static enum Action
	{
		GotoScreen,
		SetValue
	}
	int x;
	int y;
	int width;
	int height;
	Action action;
	int valueIndex;
	int arg; // screen or value
}