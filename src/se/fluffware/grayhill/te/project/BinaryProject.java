package se.fluffware.grayhill.te.project;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BinaryProject {
	static public class Exception extends java.lang.Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Exception(String msg) {
			super(msg);
		}
	}

	static final int WIDGET_TYPE_IMAGE_JPEG = 1;
	static final int WIDGET_TYPE_IMAGE_PNG = 0x80;
	
	static final int WIDGET_TYPE_TEXT = 0xc0;
	static final int WIDGET_TYPE_RING = 5;
	static final int WIDGET_TYPE_SECTOR = 0x20;
	static final int WIDGET_TYPE_CURSOR = 0xe1;

	static final int EVENT_TYPE_TAP = 1;
	static final int EVENT_TYPE_SWIPE = 2;
	static final int EVENT_TYPE_ROTATE = 3;

	static final int IFACE_TYPE_USB = 1;
	static final int IFACE_TYPE_CAN = 2;

	static final int EVENT_ACTION_GOTO_SCREEN = 1;
	static final int EVENT_ACTION_SET_VALUE = 2;
	
	static final int TEXT_PREFIX = 0x01;
	static final int TEXT_SUFFIX = 0x04;
	static final int TEXT_VALUE = 0x02;

	static public Color readColor(LEDataInputStream in) throws IOException 
	{
		Color color = new Color();
		color.blue = in.readUnsignedByte();
		color.green = in.readUnsignedByte();
		color.red = in.readUnsignedByte();
		return color;
	}
	
	static public Project readProject(LEDataInputStream in) throws IOException, Exception {
		int blockLength = in.readUnsignedShort();
		if (blockLength != 16)
			throw new Exception("Wrong length for first block");
		long endPos = in.getPosition();
		endPos += in.readUnsignedShort() - 2;
		int nScreens = in.readUnsignedShort();
		Project proj = new Project();
		proj.defaultScreen = in.readUnsignedShort();
		if (in.readUnsignedShort() != 1)
			throw new Exception("Unexpected value at offset 8 in first block");
		if (in.readUnsignedShort() != 1)
			throw new Exception("Unexpected value at offset 10 in first block");
		int iface = in.readUnsignedShort();
		if (iface == IFACE_TYPE_USB)
			proj.iface = Project.InterfaceType.USB;
		else if (iface == IFACE_TYPE_CAN)
			proj.iface = Project.InterfaceType.CAN;
		else
			throw new Exception("Unexpected value for interface type");
		if (in.readUnsignedShort() != 0)
			throw new Exception("Unexpected value at offset 14 in first block");
		for (int s = 0; s < nScreens; s++) {
			Screen screen = readScreen(in);
			proj.screens.add(screen);
		}
		if (endPos != in.getPosition())
			throw new Exception("Parsed file length doesn't match length in header");
		return proj;
	}

	static public Screen readScreen(LEDataInputStream in) throws IOException, Exception {
		int blockLength = in.readUnsignedShort();
		if (blockLength != 10)
			throw new Exception("Wrong length for screen block");
		Screen screen = new Screen();
		screen.index = in.readUnsignedShort();
		int nVars = in.readUnsignedShort();
		int nWidgets = in.readUnsignedShort();
		int nEvents = in.readUnsignedShort();
		for (int v = 0; v < nVars; v++) {
			Variable var = readVariable(in);
			screen.vars.add(var);
		}
		for (int w = 0; w < nWidgets; w++) {
			Widget widget = readWidget(in);
			screen.widgets.add(widget);
		}
		for (int e = 0; e < nEvents; e++) {
			Event event = readEvent(in);
			screen.events.add(event);
		}
		return screen;
	}

	static public Variable readVariable(LEDataInputStream in) throws IOException, Exception {
		Variable var = new Variable();
		int blockLength = in.readUnsignedShort();
		if (blockLength != 14)
			throw new Exception("Wrong length for variable block");
		var.id = in.readUnsignedByte();
		var.control = in.readUnsignedByte();
		if ((var.control & ~Variable.CONTROL_MASK) != 0) {

			throw new Exception(
					"Unexpected value at offset 3 (flags) in variable block, got 0x" + Integer.toHexString(var.control));
		}
		var.startValue = in.readSignedShort();
		var.minValue = in.readSignedShort();
		var.maxValue = in.readSignedShort();
		var.valueStep = in.readSignedShort();
		var.displayCode = in.readSignedByte();
		var.flags = in.readSignedByte();
		if ((var.flags & ~Variable.FLAGS_MASK) != 0)
			throw new Exception("Unexpected flags in variable block");
		return var;
	}

	static public Event readEvent(LEDataInputStream in) throws IOException, Exception {
		long blockEnd = in.getPosition();
		blockEnd += in.readUnsignedShort();
		int type = in.readUnsignedShort();
		Event event;
		switch (type) {
		case 1:
			event = readTapEvent(in);
			break;
		case 2:
			event = readSwipeEvent(in);
			break;
		case 3:
			event = readRotateEvent(in);
			break;
		default:
			throw new Exception("Unknown event type " + type);
		}
		if (blockEnd != in.getPosition())
			throw new Exception("Parsed event length doesn't match length in header");
		return event;
	}

	static Event readTapEvent(LEDataInputStream in) throws IOException, Exception {
		TapEvent event = new TapEvent();
		event.x = in.readUnsignedShort();
		event.y = in.readUnsignedShort();
		event.width = in.readUnsignedShort();
		event.height = in.readUnsignedShort();
		int action = in.readUnsignedByte();
		switch (action) {
		case 1:
			event.action = TapEvent.Action.GotoScreen;
			break;
		case 2:
			event.action = TapEvent.Action.SetValue;
			break;
		default:
			throw new Exception("Unknown event action " + action);
		}
		event.valueIndex = in.readUnsignedByte();
		event.arg = in.readUnsignedShort();
		return event;
	}

	static Event readSwipeEvent(LEDataInputStream in) throws IOException, Exception {
		SwipeEvent event = new SwipeEvent();
		event.up = in.readUnsignedShort();
		event.down = in.readUnsignedShort();
		event.right = in.readUnsignedShort();
		event.left = in.readUnsignedShort();
		return event;
	}

	static Event readRotateEvent(LEDataInputStream in) throws IOException, Exception {
		RotateEvent event = new RotateEvent();
		event.CW = in.readUnsignedShort();
		event.CCW = in.readUnsignedShort();
		return event;
	}

	static public Widget readWidget(LEDataInputStream in) throws IOException, Exception {
		long blockEnd = in.getPosition();
		int blockLength = in.readUnsignedShort();
		blockEnd += blockLength;
		int index = in.readUnsignedShort();
		int type = in.readUnsignedShort();
		Widget w;
		switch (type) {
		case WIDGET_TYPE_IMAGE_PNG:
		case WIDGET_TYPE_IMAGE_JPEG:	
			w = readImage(in,type);
			break;
		case WIDGET_TYPE_TEXT:
			w = readText(in);
			break;
		//case WIDGET_TYPE_RING:
		//	w = readRing(in);
		//	break;
		case WIDGET_TYPE_SECTOR:
			w = readSector(in);
			break;
		case WIDGET_TYPE_CURSOR:
			w = readCursor(in);
			break;
		default:
			w = readGenericWidget(in, type, blockEnd);
		}
		w.index = index;
		if (blockEnd != in.getPosition())
			throw new Exception("Parsed widget length doesn't match length in header");
		return w;
	}
	
	static public Widget readGenericWidget(LEDataInputStream in, int type, long end) throws IOException, Exception {
		GenericWidget w = new GenericWidget();
		w.type = type;
		int count = (int) (end - in.getPosition());
		w.data = in.readBytes(count);
		return w;
	}
	
	static public Widget readImage(LEDataInputStream in, int type) throws IOException, Exception {
		Image w = new Image();
		w.x = in.readUnsignedShort();
		w.y = in.readUnsignedShort();
		w.filename = in.readString();
		if (type == WIDGET_TYPE_IMAGE_JPEG && !w.filename.endsWith(".jpg")) {
			throw new Exception("Image filename does not end in .jpg");
		}
		if (type == WIDGET_TYPE_IMAGE_PNG && !w.filename.endsWith(".png")) {
			throw new Exception("Image filename does not end in .png");
		}
		return w;
	}

	

	static public Widget readSector(LEDataInputStream in) throws IOException, Exception {
		Sector w = new Sector();
		w.startAngle = in.readUnsignedShort();
		w.endAngle = in.readUnsignedShort();
		w.radius1 = in.readUnsignedShort();
		w.radius2 = in.readUnsignedShort();
		w.x = in.readUnsignedShort();
		w.y = in.readUnsignedShort();
		if (in.readUnsignedByte() != 0xff) {
			new Exception("Unexpected value at offset 18 in sector widget");
		}
		w.background = readColor(in);
		if (in.readUnsignedByte() != 0xff) {
			new Exception("Unexpected value at offset 22 in sector widget");
		}
		w.foreground = readColor(in);
		if (in.readUnsignedByte() != 0x01) {
			new Exception("Unexpected value at offset 26 in sector widget");
		}
		if (in.readUnsignedByte() != 0x01) {
			new Exception("Unexpected value at offset 27 in sector widget");
		}
		
		w.valueID = in.readUnsignedByte();
		return w;
	}
	
	static public Widget readCursor(LEDataInputStream in) throws IOException, Exception {
		Cursor w = new Cursor();
		w.inner_radius = in.readUnsignedShort();
		if (in.readUnsignedShort() != 0x0000) {
			new Exception("Unexpected value at offset 8 in cursor widget");
		}
		w.outer_radius = in.readUnsignedShort();
		if (in.readUnsignedShort() != 0x0000) {
			new Exception("Unexpected value at offset 12 in cursor widget");
		}
		if (in.readUnsignedByte() != 0xff) {
			new Exception("Unexpected value at offset 14 in cursor widget");
		}
		w.inner_color = readColor(in);
		if (in.readUnsignedByte() != 0xff) {
			new Exception("Unexpected value at offset 18 in cursor widget");
		}
		w.outer_color = readColor(in);
		if (in.readUnsignedByte() != 0x01) {
			new Exception("Unexpected value at offset 22 in cursor widget");
		}
		return w;
	}
	
	static public Widget readText(LEDataInputStream in) throws IOException, Exception {
		Text w = new Text();
		w.x = in.readUnsignedShort();
		w.y = in.readUnsignedShort();
		if (in.readUnsignedByte() == 0xff) {
			new Exception("Unexpected value at offset 10 in text widget");
		}
		w.color = readColor(in);

		w.fontIndex = in.readUnsignedByte();
		w.fontSize = in.readUnsignedByte();
		int flags = in.readUnsignedByte();
		if ((flags & 0xfa) != TEXT_VALUE)
			throw new Exception("Unexpected text flags");
		w.valueID = in.readUnsignedByte();
		long str_start = in.getPosition();
		if ((flags & TEXT_PREFIX) != 0) {
			w.prefix = in.readString();
		}
		if ((flags & TEXT_SUFFIX) != 0) {
			w.suffix = in.readString();
		}
		in.readPadEven(in.getPosition() - str_start);
		return w;
	}

	static public Project loadProject(File file) throws IOException, Exception {
		FileInputStream file_in = new FileInputStream(file);
		LEDataInputStream data_in = new LEDataInputStream(file_in);
		Project proj = readProject(data_in);
		data_in.close();
		return proj;
	}

	static public void writeColor(LEDataOutputStream out, Color color) throws IOException {
		out.writeUnsignedByte(color.blue);
		out.writeUnsignedByte(color.green);
		out.writeUnsignedByte(color.red);
	}
	
	static public void writeVariable(LEDataOutputStream out, Variable var) throws IOException {
		out.writeUnsignedShort(14); // Block length
		out.writeUnsignedByte(var.id);
		out.writeUnsignedByte(var.control);
		out.writeUnsignedShort(var.startValue);
		out.writeUnsignedShort(var.minValue);
		out.writeUnsignedShort(var.maxValue);
		out.writeUnsignedShort(var.valueStep);
		out.writeUnsignedByte(var.displayCode);
		out.writeUnsignedByte(var.flags);
	}

	static public void writeImage(LEDataOutputStream out, Image image) throws IOException {
		ByteArrayOutputStream filename_bytes = new ByteArrayOutputStream();
		LEDataOutputStream filename_out = new LEDataOutputStream(filename_bytes);
		filename_out.writeString(image.filename);
		filename_out.close();
		out.writeUnsignedShort(10 + filename_bytes.size()); // Block length
		out.writeUnsignedShort(image.index);
		if (image.filename.endsWith(".jpg")) {
			out.writeUnsignedShort(WIDGET_TYPE_IMAGE_PNG);
		} else {
			out.writeUnsignedShort(WIDGET_TYPE_IMAGE_PNG);
		}
		out.writeUnsignedShort(image.x);
		out.writeUnsignedShort(image.y);
		out.write(filename_bytes.toByteArray());
	}

	static public void writeText(LEDataOutputStream out, Text text) throws IOException {
		ByteArrayOutputStream text_bytes = new ByteArrayOutputStream();
		LEDataOutputStream text_out = new LEDataOutputStream(text_bytes);
		if (text.prefix != null) {
			text_out.writeString(text.prefix);
		}
		if (text.suffix != null) {
			text_out.writeString(text.suffix);
		}
		text_out.close();
		out.writeUnsignedShort(18 + text_bytes.size()); // Block length
		out.writeUnsignedShort(text.index);
		out.writeUnsignedShort(WIDGET_TYPE_TEXT);
		out.writeUnsignedShort(text.x);
		out.writeUnsignedShort(text.y);
		out.writeUnsignedByte(0xff); // Pad
		writeColor(out, text.color);
		out.writeUnsignedByte(text.fontIndex);
		out.writeUnsignedByte(text.fontSize);
		int flags = TEXT_VALUE;
		if (text.prefix != null) {
			flags |= TEXT_PREFIX;
		}
		if (text.suffix != null) {
			flags |= TEXT_SUFFIX;
		}
		out.writeUnsignedByte(flags);
		out.writeUnsignedByte(text.valueID);
		out.write(text_bytes.toByteArray());
		out.writePadEven(text_bytes.size());
	}

	
	
	static public void writeSector(LEDataOutputStream out, Sector sector) throws IOException {
		out.writeUnsignedShort(29); // Block length
		out.writeUnsignedShort(sector.index);
		out.writeUnsignedShort(WIDGET_TYPE_SECTOR);
		out.writeUnsignedShort(sector.startAngle);
		out.writeUnsignedShort(sector.endAngle);

		out.writeUnsignedShort(sector.radius1);
		out.writeUnsignedShort(sector.radius2);
		out.writeUnsignedShort(sector.x);
		out.writeUnsignedShort(sector.y);
		out.writeUnsignedByte(0xff);
		writeColor(out, sector.background);
		out.writeUnsignedByte(0xff);
		writeColor(out, sector.foreground);
		out.writeUnsignedByte(0x01);
		out.writeUnsignedByte(0x01);
		out.writeUnsignedByte(sector.valueID);
		
	}
	static public void writeCursor(LEDataOutputStream out, Cursor cursor) throws IOException {
		out.writeUnsignedShort(23); // Block length
		out.writeUnsignedShort(cursor.index);
		out.writeUnsignedShort(WIDGET_TYPE_CURSOR);
		out.writeUnsignedShort(cursor.inner_radius);
		out.writeUnsignedShort(0);
		out.writeUnsignedShort(cursor.outer_radius);
		out.writeUnsignedShort(0);
		out.writeUnsignedByte(0xff);
		writeColor(out, cursor.inner_color);
		out.writeUnsignedByte(0xff);
		writeColor(out, cursor.outer_color);
		out.writeUnsignedByte(0x01);
	}
	
	static public void writeGenericWidget(LEDataOutputStream out, GenericWidget widget) throws IOException {
		out.writeUnsignedShort(6 + widget.data.length); // Block length
		out.writeUnsignedShort(widget.index);
		out.writeUnsignedShort(widget.type);
		out.writeBytes(widget.data);
	}		

	static public void writeTap(LEDataOutputStream out, TapEvent event) throws IOException {
		out.writeUnsignedShort(16); // Block length
		out.writeUnsignedShort(EVENT_TYPE_TAP);
		out.writeUnsignedShort(event.x);
		out.writeUnsignedShort(event.y);
		out.writeUnsignedShort(event.width);
		out.writeUnsignedShort(event.height);
		if (event.action == TapEvent.Action.GotoScreen) {
			out.writeUnsignedByte(EVENT_ACTION_GOTO_SCREEN);
		} else {
			out.writeUnsignedByte(EVENT_ACTION_SET_VALUE);
		}
		out.writeUnsignedByte(event.valueIndex);
		out.writeUnsignedShort(event.arg);

	}

	static public void writeSwipe(LEDataOutputStream out, SwipeEvent event) throws IOException {
		out.writeUnsignedShort(12); // Block length
		out.writeUnsignedShort(EVENT_TYPE_SWIPE);
		out.writeUnsignedShort(event.up);
		out.writeUnsignedShort(event.down);
		out.writeUnsignedShort(event.right);
		out.writeUnsignedShort(event.left);
	}

	static public void writeRotate(LEDataOutputStream out, RotateEvent event) throws IOException {
		out.writeUnsignedShort(8); // Block length
		out.writeUnsignedShort(EVENT_TYPE_ROTATE);
		out.writeUnsignedShort(event.CW);
		out.writeUnsignedShort(event.CCW);
	}

	static public void writeScreen(LEDataOutputStream out, Screen screen) throws IOException {
		out.writeUnsignedShort(10); // Block length
		out.writeUnsignedShort(screen.index);
		out.writeUnsignedShort(screen.vars.size());
		out.writeUnsignedShort(screen.widgets.size());
		out.writeUnsignedShort(screen.events.size());
		for (Variable v : screen.vars) {
			writeVariable(out, v);
		}
		for (Widget w : screen.widgets) {
			if (w instanceof Image) {
				writeImage(out, (Image) w);
			} else if (w instanceof Text) {
				writeText(out, (Text) w);
			//} else if (w instanceof Ring) {
			//	writeRing(out, (Ring) w);
			} else if (w instanceof Sector) {
				writeSector(out, (Sector) w);						
			} else if (w instanceof Cursor) {
				writeCursor(out, (Cursor) w);						
			} else if (w instanceof GenericWidget) {
				writeGenericWidget(out, (GenericWidget)w);
			}
		}
		for (Event e : screen.events) {
			if (e instanceof TapEvent) {
				writeTap(out, (TapEvent) e);
			} else if (e instanceof SwipeEvent) {
				writeSwipe(out, (SwipeEvent) e);
			} else if (e instanceof RotateEvent) {
				writeRotate(out, (RotateEvent) e);
			}
		}
	}

	static public void writeProject(LEDataOutputStream out, Project proj) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		LEDataOutputStream screen_out = new LEDataOutputStream(bytes);
		for (Screen s : proj.screens) {
			writeScreen(screen_out, s);
		}
		screen_out.close();
		out.writeUnsignedShort(16);// Block length
		out.writeUnsignedShort(bytes.size() + 16);
		out.writeUnsignedShort(proj.screens.size());
		out.writeUnsignedShort(proj.defaultScreen);
		out.writeUnsignedShort(1);
		out.writeUnsignedShort(1);
		if (proj.iface == Project.InterfaceType.USB) {
			out.writeUnsignedShort(1); // USB
		} else {
			out.writeUnsignedShort(2); // CAN
		}
		out.writeUnsignedShort(0);

		out.write(bytes.toByteArray());

	}

	static public Project saveProject(File file, Project proj) throws IOException {
		FileOutputStream file_out = new FileOutputStream(file);
		LEDataOutputStream data_out = new LEDataOutputStream(file_out);
		writeProject(data_out, proj);
		data_out.close();
		return proj;
	}
}
