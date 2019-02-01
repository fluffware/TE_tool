package se.fluffware.grayhill.te.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
				
				File entry_file = dir.toPath().resolve(entry.getName()).toFile();
				if (entry.isDirectory()) {
					if (!entry_file.exists() && !entry_file.mkdir()) {
						throw new IOException("Failed to create directory: " + entry_file.toString());
					}
				} else {
					File parent = entry_file.getParentFile();

					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("Failed to create directory: " + parent.toString());
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
			}
		} finally {
			zip.close();
		}

	}

	static void pack_dir(ZipOutputStream zip, File dir, String entry_prefix) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		Path dir_path = dir.toPath();
		for (String name : dir.list()) {
			Path file_path = dir_path.resolve(name);
			File file = file_path.toFile();
			if (file.isDirectory()) {
				pack_dir(zip, file, entry_prefix+name+"/");
			} else {
				ZipEntry entry = new ZipEntry(entry_prefix+name);
				FileTime lastModified = Files.getLastModifiedTime(file_path);
				entry.setLastModifiedTime(lastModified);
				zip.putNextEntry(entry);
				InputStream inp = new FileInputStream(file);
				try {
					while (true) {
						int r = inp.read(buffer);
						if (r < 0) {
							break;
						}
						zip.write(buffer, 0, r);
					}

				} finally {
					inp.close();
					zip.closeEntry();
				}
			}
		}
	}

	static public void packFromDir(File zip_file, File dir) throws IOException {
		OutputStream outp = new FileOutputStream(zip_file);
		ZipOutputStream zip = new ZipOutputStream(outp);
		try {
			pack_dir(zip, dir, "");
		} finally {
			zip.close();
		}
	}
}
