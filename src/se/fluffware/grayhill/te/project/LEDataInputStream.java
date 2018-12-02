package se.fluffware.grayhill.te.project;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LEDataInputStream extends FilterInputStream {
	long pos = 0;
	public LEDataInputStream(InputStream in) {
		super(in);
	}
	
	public long getPosition()
	{
		return pos;
	}
	
	public int readUnsignedByte() throws IOException
	{
		int v = read();
		if (v < 0) throw new EOFException();
		pos += 1;
		return v;
	}
	
	public int readSignedByte() throws IOException
	{
		int v = readUnsignedByte();
		if (v >= 128) v -= 256;
		return v;
	}
	
	public int readUnsignedShort() throws IOException
	{
		int v0 = read();
		if (v0 < 0) throw new EOFException();
		int v1 = read();
		if (v1 < 0) throw new EOFException();
		pos += 2;
		return v0 | (v1 << 8);
	}
	
	public int readSignedShort() throws IOException
	{
		int v = readUnsignedShort();
		if (v >= 32768) v -= 65536;
		return v;
	}
	
	public String readString() throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		while (true) {
			int v = read();
			pos += 1;
			if (v == 0)
				break;
			if (v < 0)
				throw new EOFException();
			buf.write(v);
		}
		if ((pos & 1) != 0) {
			// Pad to even byte
			int v = read();
			if (v < 0)
				throw new EOFException();
			pos += 1;
		}
		return buf.toString("ISO8859-1");
	}
}
