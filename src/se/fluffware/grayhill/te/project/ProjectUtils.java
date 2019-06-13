package se.fluffware.grayhill.te.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ProjectUtils {
	
	static public class CheckException extends Exception
	{
		private static final long serialVersionUID = 1L;

		CheckException(String msg) {
			super(msg);
		}
	}

	static void checkScreen(Screen screen, Set<Integer> screens) throws CheckException {
		TreeSet<Integer> var_indices = new TreeSet<Integer>();
		TreeSet<Integer> widget_indices = new TreeSet<Integer>();
		try {
			for (Variable v : screen.vars) {
				try {
					if (!var_indices.add(v.id)) {
						throw new CheckException("There are more than one variable with the same index ");
					}
					if (v.minValue > v.maxValue) {
						throw new CheckException("Min value is greater than max value");

					}
					if (v.startValue < v.minValue || v.startValue > v.maxValue) {
						throw new CheckException("Start value is not between limits");

					}
				} catch (CheckException e) {
					throw new CheckException("Variable " + v.id + ": " + e.getMessage());
				}
			}

			for (Widget w : screen.widgets) {
				try {
					if (!widget_indices.add(w.index)) {
						throw new CheckException("There are more than one widget with the same index ");
					}
					if (w instanceof Text) {
						Text text = (Text) w;
						if (!var_indices.contains(text.valueID)) {
							throw new CheckException("Unknown variable index " + text.valueID);
						}
					} else if (w instanceof Ring) {
						Ring ring = (Ring) w;
						if (!var_indices.contains(ring.valueIndex)) {
							throw new CheckException("Unknown variable index " + ring.valueIndex);
						}
					}
				} catch (CheckException e) {
					throw new CheckException("Widget " + w.index + ": " + e.getMessage());
				}
			}
			for (Event ev : screen.events) {
				if (ev instanceof SwipeEvent) {
					SwipeEvent swipe = (SwipeEvent) ev;
					if (swipe.up != 0 && !screens.contains(swipe.up)) {
						throw new CheckException(
								"Unknown screen " + Screen.indexToString(swipe.up) + " for swipe up event ");
					}
					if (swipe.down != 0 && !screens.contains(swipe.down)) {
						throw new CheckException(
								"Unknown screen " + Screen.indexToString(swipe.up) + " for swipe down event ");
					}
					if (swipe.left != 0 && !screens.contains(swipe.left)) {
						throw new CheckException(
								"Unknown screen " + Screen.indexToString(swipe.left) + " for swipe left event ");
					}
					if (swipe.right != 0 && !screens.contains(swipe.right)) {
						throw new CheckException(
								"Unknown screen " + Screen.indexToString(swipe.right) + " for swipe right event ");
					}
				} else if (ev instanceof RotateEvent) {
					RotateEvent rotate = (RotateEvent) ev;
					if (rotate.CCW != 0 && !screens.contains(rotate.CCW)) {
						throw new CheckException(
								"Unknown screen " + Screen.indexToString(rotate.CCW) + " for rotate CCW event ");
					}
					if (rotate.CW != 0 && !screens.contains(rotate.CW)) {
						throw new CheckException(
								"Unknown screen " + Screen.indexToString(rotate.CW) + " for rotate CW event ");
					}
				} else if (ev instanceof RotateEvent) {
					TapEvent tap = (TapEvent) ev;
					switch (tap.action) {
					case GotoScreen:
						if (!screens.contains(tap.arg)) {
							throw new CheckException(
									"Unknown screen " + Screen.indexToString(tap.arg) + " for tap event ");
						}
						break;
					case SetValue:
						if (!var_indices.contains(tap.valueIndex)) {
							throw new CheckException("Unknown variable index " + tap.valueIndex + " for tap event ");
						}
					}
				}
			}
		} catch (CheckException e) {
			throw new CheckException("Screen " + Screen.indexToString(screen.index) + ": " + e.getMessage());
		}

	}

	static public void checkProject(Project proj) throws CheckException {
		TreeSet<Integer> indices = new TreeSet<Integer>();
		for (Screen s: proj.screens) {
			if (!indices.add(s.index)) {
				throw new CheckException("There are more than one screen with index "+Screen.indexToString(s.index));
			}
			
		}
		for (Screen s: proj.screens) {
			checkScreen(s, indices);
		}
		if (!indices.contains(proj.defaultScreen)) {
			throw new CheckException("The default screen does not exist");
		}
		if (proj.iface == Project.InterfaceType.Invalid) {
			throw new CheckException("Invalid hardware interface");
		}
	}
	
	static String remap(Map<String,String> map, String old_value, String new_value)
	{
		if (map.containsKey(old_value)) {
			return map.get(old_value);
		} else {
			
			map.put(old_value, new_value);
			return new_value;
		}
	}
	
	static public void remapImages(Project proj, Map<String,String> map) {
		
		for (Screen s: proj.screens) {
			for (Widget w : s.widgets) {
			
					if (w instanceof Image) {
						Image image = (Image)w;
						int dot = image.filename.lastIndexOf('.');
						String ext;
						if (dot > 0) {
							ext = image.filename.substring(dot + 1);
						} else {
							ext = "png";
						}
						image.filename = remap(map, image.filename,"image_"+s.index+"_"+w.index+"."+ext);
						
					} else if (w instanceof Ring) {
						Ring ring = (Ring) w;
						String prefix = "ring_"+s.index+"_"+w.index+"_";
						
						ring.fullRingImage = remap(map, ring.fullRingImage, prefix+"full.png");
						
						ring.emptyRingImage = remap(map, ring.emptyRingImage, prefix+"empty.png");
						
					ring.cursorImage = remap(map, ring.cursorImage, prefix + "cursor.png");
				}
			}
		}
	}

	static public void remapFonts(Project proj, Map<String, String> map) {

		for (Screen s : proj.screens) {
			for (Widget w : s.widgets) {

				if (w instanceof Text) {
					Text text = (Text) w;
					String font_name = "font_" + text.fontIndex + ".ttf";
					remap(map, font_name, font_name);
				}
			}
		}
	}

	static public void copyMappedFiles(Map<String, String> map, Path from_dir, Path to_dir) throws IOException {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			File from = from_dir.resolve(entry.getKey()).toFile();
			File to = to_dir.resolve(entry.getValue()).toFile();

			FileInputStream sourceStream = new FileInputStream(from);

			try {
				FileChannel sourceChannel = sourceStream.getChannel();
				FileOutputStream destStream = new FileOutputStream(to);
				try {
					FileChannel destChannel = destStream.getChannel();
					destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
					destChannel.close();
				} finally {
					destStream.close();
				}
				sourceChannel.close();
			} finally {
				sourceStream.close();

			}
		}
	}
}
