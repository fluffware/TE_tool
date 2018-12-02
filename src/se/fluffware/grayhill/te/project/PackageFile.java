package se.fluffware.grayhill.te.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PackageFile {
	static final int BUFFER_SIZE = 64 * 1024;

	static public void unpackToDir(File zip_file, File dir) throws IOException {
		InputStream inp = new FileInputStream(zip_file);
		ZipInputStream zip = new ZipInputStream(inp);
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			while (true) {
				ZipEntry entry = zip.getNextEntry();
				if (entry == null)
					break;
				if (entry.isDirectory()) {
					throw new IOException("Directories are not allowed in project packages");
				}
				File entry_file = dir.toPath().resolve(entry.getName()).toFile();
				File parent = entry_file.getParentFile();
				
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory: "+parent.toString());
				}
				OutputStream outp = new FileOutputStream(entry_file);
				try {
					while (true) {
						int r = zip.read(buffer, 0, buffer.length);
						if (r < 0)
							break;
						outp.write(buffer, 0, r);
					}
				} finally {
					outp.close();
				}
			}
		} finally {
			zip.close();
		}

	}
}
