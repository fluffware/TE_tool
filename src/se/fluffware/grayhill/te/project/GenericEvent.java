package se.fluffware.grayhill.te.project;

public class GenericEvent extends Event
{
	int type;
	byte [] data; // Raw bytes following the event type
}
