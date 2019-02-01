package se.fluffware.grayhill.te.project;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BinaryProject {
	static public class Exception extends java.lang.Exception
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Exception(String msg)
		{
			super(msg);
		}
	}
	
	static final int WIDGET_TYPE_IMAGE = 1;
	static final int WIDGET_TYPE_TEXT = 4;
	static final int WIDGET_TYPE_RING = 5;
	
	static final int EVENT_TYPE_TAP = 1;
	static final int EVENT_TYPE_SWIPE = 2;
	static final int EVENT_TYPE_ROTATE = 3;
	
	static final int IFACE_TYPE_USB = 1;
	static final int IFACE_TYPE_CAN = 2;
	
	static final int EVENT_ACTION_GOTO_SCREEN = 1;
	static final int EVENT_ACTION_SET_VALUE = 2;
	
	
	static public Project readProject(LEDataInputStream in) throws IOException, Exception {
		int blockLength = in.readUnsignedShort();
		if (blockLength != 16) throw new Exception("Wrong length for first block");
		long endPos = in.getPosition();
		endPos += in.readUnsignedShort() - 2;
		int nScreens = in.readUnsignedShort();
		Project proj = new Project();
		proj.defaultScreen = in.readUnsignedShort();
		if (in.readUnsignedShort() != 1) throw new Exception("Unexpected value at offset 8 in first block");
		if (in.readUnsignedShort() != 1) throw new Exception("Unexpected value at offset 10 in first block");
		int iface = in.readUnsignedShort();
		if (iface == IFACE_TYPE_USB) proj.iface = Project.InterfaceType.USB;
		else if (iface == IFACE_TYPE_CAN) proj.iface = Project.InterfaceType.CAN;
		else throw new Exception("Unexpected value for interface type");
		if (in.readUnsignedShort() != 0) throw new Exception("Unexpected value at offset 14 in first block");
		for (int s = 0; s < nScreens; s++) {
			Screen screen = readScreen(in);
			proj.screens.add(screen);
		}
		if (endPos != in.getPosition()) throw new Exception("Parsed file length doesn't match length in header");
		return proj;
	}

	static public Screen readScreen(LEDataInputStream in) throws IOException, Exception {
		int blockLength = in.readUnsignedShort();
		if (blockLength != 10) throw new Exception("Wrong length for screen block");
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
			Widget widget= readWidget(in);
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
		if (blockLength != 14) throw new Exception("Wrong length for variable block");
		var.index = in.readUnsignedByte();
		if ((in.readUnsignedByte() & 0xfffe)!= 2) throw new Exception("Unexpected value at offset 3 in variable block");
		var.startValue = in.readSignedShort();
		var.minValue = in.readSignedShort();
		var.maxValue = in.readSignedShort();
		var.valueStep = in.readSignedShort();
		if (in.readUnsignedShort() != 0) throw new Exception("Unexpected value at offset 12 in variable block");
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
		if (blockEnd != in.getPosition()) throw new Exception("Parsed event length doesn't match length in header");
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
		event.left = in.readUnsignedShort();
		event.right = in.readUnsignedShort();
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
		blockEnd += in.readUnsignedShort();
		int index = in.readUnsignedShort();
		int type = in.readUnsignedShort();
		Widget w;
		switch (type) {
		case WIDGET_TYPE_IMAGE:
			w = readImage(in);
			break;
		case WIDGET_TYPE_TEXT:
			w = readText(in);
			break;
		case WIDGET_TYPE_RING:
			w = readRing(in);
			break;
		default:
			throw new Exception("Unknown widget type " + type);
		}
		w.index = index;
		if (blockEnd != in.getPosition()) throw new Exception("Parsed widget length doesn't match length in header");
		return w;
	}
	
	static public Widget readImage(LEDataInputStream in) throws IOException, Exception {
		Image w = new Image();
		w.x = in.readUnsignedShort();
		w.y = in.readUnsignedShort();
		w.filename = in.readString();
		return w;
	}
	
	static public Widget readRing(LEDataInputStream in) throws IOException, Exception {
		Ring w = new Ring();
		w.x = in.readUnsignedShort();
		w.y = in.readUnsignedShort();
		w.endAngle = in.readUnsignedShort();
		w.startAngle = in.readUnsignedShort();
		w.radius = in.readUnsignedShort();
		w.valueIndex = in.readUnsignedShort();
		w.emptyRingImage = in.readString();
		w.fullRingImage = in.readString();
		w.cursorImage = in.readString();
		return w;
	}
	
	static public Widget readText(LEDataInputStream in) throws IOException, Exception {
		Text w = new Text();
		w.x = in.readUnsignedShort();
		w.y = in.readUnsignedShort();
		w.blue = in.readUnsignedByte();
		w.green = in.readUnsignedByte();
		w.red = in.readUnsignedByte();
		in.readUnsignedByte(); // Pad
		w.fontIndex = in.readUnsignedByte();
		w.fontSize = in.readUnsignedByte();
		int flags = in.readUnsignedByte();
		if ((flags & 0xfa) != 0x02)
			throw new Exception("Unexpected text flags");
		w.valueIndex = in.readUnsignedByte();
		if ((flags & 0x01) != 0) {
			w.prefix = in.readString();
		}
		if ((flags & 0x04) != 0) {
			w.suffix = in.readString();
		}

		return w;
	}
	static public Project loadProject(File file) throws IOException, Exception 
	{
		FileInputStream file_in = new FileInputStream(file);
		LEDataInputStream data_in = new LEDataInputStream(file_in);
		Project proj = readProject(data_in);
		data_in.close();
		return proj;
	}
	
	static public void writeVariable(LEDataOutputStream out, Variable var) throws IOException
	{
		out.writeUnsignedShort(14); // Block length
		out.writeUnsignedByte(var.index);
		out.writeUnsignedByte(var.flags);
		out.writeUnsignedShort(var.startValue);
		out.writeUnsignedShort(var.minValue);
		out.writeUnsignedShort(var.maxValue);
		out.writeUnsignedShort(var.valueStep);
		out.writeUnsignedShort(0);
	}
	
	static public void writeImage(LEDataOutputStream out, Image image) throws IOException {
		ByteArrayOutputStream filename_bytes = new ByteArrayOutputStream();
		LEDataOutputStream filename_out = new LEDataOutputStream(filename_bytes);
		filename_out.writeString(image.filename);
		filename_out.close();
		out.writeUnsignedShort(10 + filename_bytes.size()); // Block length
		out.writeUnsignedShort(image.index);
		out.writeUnsignedShort(WIDGET_TYPE_IMAGE);
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
		out.writeUnsignedByte(text.blue);
		out.writeUnsignedByte(text.green);
		out.writeUnsignedByte(text.red);
		out.writeUnsignedByte(0xff); // Pad
		out.writeUnsignedByte(text.fontIndex);
		out.writeUnsignedByte(text.fontSize);
		int flags = 0x02;
		if (text.prefix != null) {
			flags |= 0x01;
		}
		if (text.suffix != null) {
			flags |= 0x04;
		}
		out.writeUnsignedByte(flags);
		out.writeUnsignedByte(text.valueIndex);
		out.write(text_bytes.toByteArray());
	}
	
	static public void writeRing(LEDataOutputStream out, Ring ring) throws IOException {
		ByteArrayOutputStream filename_bytes = new ByteArrayOutputStream();
		LEDataOutputStream filename_out = new LEDataOutputStream(filename_bytes);
		filename_out.writeString(ring.emptyRingImage);
		filename_out.writeString(ring.fullRingImage);
		filename_out.writeString(ring.cursorImage);
		filename_out.close();
		
		out.writeUnsignedShort(18 + filename_bytes.size()); // Block length
		out.writeUnsignedShort(ring.index);
		out.writeUnsignedShort(WIDGET_TYPE_RING);
		out.writeUnsignedShort(ring.x);
		out.writeUnsignedShort(ring.y);
		out.writeUnsignedShort(ring.endAngle);
		out.writeUnsignedShort(ring.startAngle);
		out.writeUnsignedShort(ring.radius);
		out.writeUnsignedShort(ring.valueIndex);
		out.write(filename_bytes.toByteArray());
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
		out.writeUnsignedShort(event.left);
		out.writeUnsignedShort(event.right);	
	}
	static public void writeRotate(LEDataOutputStream out, RotateEvent event) throws IOException {
		out.writeUnsignedShort(8); // Block length
		out.writeUnsignedShort(EVENT_TYPE_ROTATE);
		out.writeUnsignedShort(event.CW);
		out.writeUnsignedShort(event.CCW);
	}
	
	static public void writeScreen(LEDataOutputStream out, Screen screen) throws IOException
	{
		out.writeUnsignedShort(10); // Block length
		out.writeUnsignedShort(screen.index);
		out.writeUnsignedShort(screen.vars.size());
		out.writeUnsignedShort(screen.widgets.size());
		out.writeUnsignedShort(screen.events.size());
		for (Variable v: screen.vars) {
			writeVariable(out, v);
		}
		for (Widget w : screen.widgets) {
			if (w instanceof Image) {
				writeImage(out, (Image) w);
			} else if (w instanceof Text) {
				writeText(out, (Text) w);
			} else if (w instanceof Ring) {
				writeRing(out, (Ring) w);
			} 
		}
		for (Event e: screen.events) {
			if (e instanceof TapEvent) {
				writeTap(out, (TapEvent)e);
			} else if (e instanceof SwipeEvent) {
				writeSwipe(out, (SwipeEvent)e);
			} else if (e instanceof RotateEvent) {
				writeRotate(out, (RotateEvent)e);
			} 
		}
	}
	
	static public void writeProject(LEDataOutputStream out, Project proj) throws IOException
	{
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
	
	static public Project saveProject(File file, Project proj) throws IOException
	{
		FileOutputStream file_out = new FileOutputStream(file);
		LEDataOutputStream data_out = new LEDataOutputStream(file_out);
		writeProject(data_out, proj);
		data_out.close();
		return proj;
	}
}
