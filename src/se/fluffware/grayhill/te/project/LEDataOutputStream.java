package se.fluffware.grayhill.te.project;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LEDataOutputStream extends FilterOutputStream {
	public LEDataOutputStream(OutputStream out) {
		super(out);
	}
	
	public void writeUnsignedByte(int v) throws IOException
	{
		write(v);
	}
	
	public void writeSignedByte(int v) throws IOException
	{
		write(v);
	}
	
	public void writeUnsignedShort(int v) throws IOException
	{
		write(v);
		write(v >> 8);
	}
	
	public void writeSignedShort(int v) throws IOException
	{
		write(v);
		write(v >> 8);
	}
	
	public void writeString(String str) throws IOException {
		byte [] b = str.getBytes("ISO8859-1");
		write(b);
		write(0);
	}
	
	public void writePadEven(long len) throws IOException {
		if ((len & 1) != 0) {
			write(0xff);
		}
	}
	
	
	public void writeBytes(byte [] b) throws IOException {
		write(b);
	}
}
