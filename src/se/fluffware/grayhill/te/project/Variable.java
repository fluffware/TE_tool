package se.fluffware.grayhill.te.project;

public class Variable
{
	public int index;
	public int startValue;
	public int minValue;
	public int maxValue;
	public int valueStep;
	
	public static final int FLAGS_RING_ADJUST=0x01;
	public static final int FLAGS_UNUSED_VALUE = 0x02;
	public static final int FLAGS_MASK = FLAGS_RING_ADJUST;
	public int flags;
}