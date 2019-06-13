package se.fluffware.grayhill.te.project;

public class Variable
{
	public int id;
	public int startValue;
	public int minValue;
	public int maxValue;
	public int valueStep;
	
	public static final int CONTROL_ENCODER =0x01; // The value can be changed by the encoder
	public static final int CONTROL_HOST = 0x02; // The value can be changed by the host
	public static final int CONTROL_MASK = CONTROL_ENCODER | CONTROL_HOST;	
	public int control;
	public int displayCode;
	
	public static final int FLAGS_WRAP = 0x01; // The value wraps between min and max
	public static final int FLAGS_MASK = FLAGS_WRAP;
	public int flags;
}