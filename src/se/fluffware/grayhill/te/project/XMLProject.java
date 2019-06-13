package se.fluffware.grayhill.te.project;

import java.util.Arrays;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import se.fluffware.grayhill.te.project.TapEvent.Action;

public class XMLProject {
	static public final String NS = "http://www.fluffware.se/ns/TE-project";

	
	static class Writer {
		XMLStreamWriter xml;
		int indent = 0;
		int indentStep = 4;

		public Writer(XMLStreamWriter xml) {
			this.xml = xml;
		}

		void writeIndent() throws XMLStreamException {
			char[] array = new char[indent];
			Arrays.fill(array, ' ');

			xml.writeCharacters(array, 0, indent);
		}

		void writeStartElement(String localName) throws XMLStreamException {
			xml.writeCharacters("\n");
			writeIndent();
			xml.writeStartElement(localName);
			indent += indentStep;
		}

		void writeEndElement() throws XMLStreamException {
			indent -= indentStep;
			xml.writeCharacters("\n");
			writeIndent();
			xml.writeEndElement();
		}

		void writeEmptyElement(String localName) throws XMLStreamException {
			xml.writeCharacters("\n");
			writeIndent();
			xml.writeEmptyElement(localName);
		}

		void writeAttribute(String localName, String value) throws XMLStreamException {
			xml.writeAttribute(localName, value);
		}

		void writeProject(Project proj) throws XMLStreamException {
			String iface_name;
			switch (proj.iface) {

			case USB:
				iface_name = "USB";
				break;
			case CAN:
				iface_name = "CAN";
				break;

			default:
				iface_name = "Unknown";
				break;
			}

			writeStartElement("project");
			xml.writeDefaultNamespace(NS);

			xml.writeAttribute("interface", iface_name);
			xml.writeAttribute("default-screen", Integer.toString(proj.defaultScreen));
			for (Screen s : proj.screens) {
				writeScreen(s);
			}
			writeEndElement();
		}

		void writeScreen(Screen screen) throws XMLStreamException {

			writeStartElement("screen");
			writeAttribute("index", Screen.indexToString(screen.index));
			for (Variable v : screen.vars) {
				writeVariable(v);
			}
			for (Widget w : screen.widgets) {

				if (w instanceof Image) {
					writeImage((Image) w);
				} else if (w instanceof Text) {
					writeText((Text) w);
				} else if (w instanceof Ring) {
					writeRing((Ring) w);
				} else if (w instanceof Sector) {
					writeSector((Sector) w);
				} else if (w instanceof Cursor) {
					writeCursor((Cursor) w);
				} else if (w instanceof GenericWidget) {
					writeGenericWidget((GenericWidget)w);
				} else {
					throw new XMLStreamException("Can't serialize unknown widget");
				}
			}
			for (Event e : screen.events) {

				if (e instanceof TapEvent) {
					writeTapEvent((TapEvent) e);
				} else if (e instanceof SwipeEvent) {
					writeSwipeEvent((SwipeEvent) e);
				} else if (e instanceof RotateEvent) {
					writeRotateEvent((RotateEvent) e);
				} else {
					throw new XMLStreamException("Can't serialize unknown event");
				}
			}
			writeEndElement();
		}

		void writeVariable(Variable variable) throws XMLStreamException {
			writeEmptyElement("variable");
			writeAttribute("id", Integer.toString(variable.id));
			writeAttribute("min", Integer.toString(variable.minValue));
			writeAttribute("max", Integer.toString(variable.maxValue));
			writeAttribute("start", Integer.toString(variable.startValue));
			writeAttribute("step", Integer.toString(variable.valueStep));
			writeAttribute("control", (((variable.control & Variable.CONTROL_ENCODER) != 0) ? "encoder" : "")
					+" "+ (((variable.control & Variable.CONTROL_HOST) != 0) ? "host" : ""));
			writeAttribute("exp", Integer.toString(((int)(byte)(variable.displayCode & 0xf0)) >> 4));
			writeAttribute("limits", (((variable.flags & Variable.FLAGS_WRAP) != 0) ? "wrap" : "limit"));
		}

		void widgetAttrs(Widget w) throws XMLStreamException {
			writeAttribute("index", Integer.toString(w.index));
			writeAttribute("x", Integer.toString(w.x));
			writeAttribute("y", Integer.toString(w.y));
		}

		void writeImage(Image w) throws XMLStreamException {
			writeEmptyElement("image");
			widgetAttrs(w);
			writeAttribute("filename", w.filename);
		}

		

		void writeText(Text w) throws XMLStreamException {
			writeEmptyElement("text");
			widgetAttrs(w);
			writeAttribute("font-index", Integer.toString(w.fontIndex));
			writeAttribute("font-size", Integer.toString(w.fontSize));
			writeAttribute("color", w.color.toString());
			writeAttribute("value-id", Integer.toString(w.valueID));
			if (w.prefix != null) {
				writeAttribute("prefix", w.prefix);
			}
			if (w.suffix != null) {
				writeAttribute("suffix", w.suffix);
			}

		}

		void writeRing(Ring w) throws XMLStreamException {
			writeEmptyElement("ring");
			widgetAttrs(w);
			writeAttribute("radius", Integer.toString(w.radius));
			writeAttribute("start-angle", Integer.toString(w.startAngle));
			writeAttribute("end-angle", Integer.toString(w.endAngle));
			writeAttribute("value-id", Integer.toString(w.valueIndex));
			writeAttribute("empty", w.emptyRingImage);
			writeAttribute("full", w.fullRingImage);
			writeAttribute("cursor", w.cursorImage);
		}
		void writeSector(Sector w) throws XMLStreamException {
			writeEmptyElement("sector");
			widgetAttrs(w);
			writeAttribute("radius", Integer.toString(w.radius1 + w.radius2));
			writeAttribute("cursor-radius", Integer.toString(w.radius1));
			writeAttribute("start-angle", Integer.toString(w.startAngle));
			writeAttribute("end-angle", Integer.toString(w.endAngle));
			writeAttribute("background", w.background.toString());
			writeAttribute("foreground", w.foreground.toString());
			writeAttribute("value-id", Integer.toString(w.valueID));
		}
		
		void writeCursor(Cursor w) throws XMLStreamException {
			writeEmptyElement("cursor");
			writeAttribute("index", Integer.toString(w.index));
			writeAttribute("outer-radius", Integer.toString(w.outer_radius));
			writeAttribute("inner-radius", Integer.toString(w.inner_radius));
			writeAttribute("outer-color", w.outer_color.toString());
			writeAttribute("inner-color", w.inner_color.toString());
			
		}
		
		void writeGenericWidget(GenericWidget w) throws XMLStreamException {
			
			writeStartElement("generic-widget");
			writeAttribute("index", Integer.toString(w.index));
			writeAttribute("type", Integer.toString(w.type));
			xml.writeCharacters("\n");
			writeIndent();
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < w.data.length; i++) {
				byte c = w.data[i];
				buffer.append(String.format("%02x ", c));
			}
			xml.writeCharacters(buffer.toString());
			
			writeEndElement();
			
		}
			

		void writeGoto(int screen) throws XMLStreamException {
			writeEmptyElement("goto");
			writeAttribute("screen", Screen.indexToString(screen));
		}

		void writeSet(int value_index, int value) throws XMLStreamException {
			writeEmptyElement("set");
			writeAttribute("value-id", Integer.toString(value_index));
			writeAttribute("value", Integer.toString(value));

		}

		void writeTapEvent(TapEvent event) throws XMLStreamException {
			writeStartElement("tap");
			writeAttribute("x", Integer.toString(event.x));
			writeAttribute("y", Integer.toString(event.y));
			writeAttribute("width", Integer.toString(event.width));
			writeAttribute("height", Integer.toString(event.height));

			switch (event.action) {
			case GotoScreen:
				writeGoto(event.arg);
				break;
			case SetValue:
				writeSet(event.valueIndex, event.arg);
				break;
			}
			writeEndElement();
		}

		void writeEventType(String type, int screen) throws XMLStreamException {
			if (screen > 0) {
				writeStartElement(type);
				writeGoto(screen);
				writeEndElement();
			}
		}

		void writeRotateEvent(RotateEvent event) throws XMLStreamException {
			writeEventType("rotate-cw", event.CW);
			writeEventType("rotate-ccw", event.CCW);
		}

		void writeSwipeEvent(SwipeEvent event) throws XMLStreamException {
			writeEventType("swipe-up", event.up);
			writeEventType("swipe-down", event.down);
			writeEventType("swipe-left", event.left);
			writeEventType("swipe-right", event.right);
		}

	}

	static public void writeProject(XMLStreamWriter xml_writer, Project proj) throws XMLStreamException {
		xml_writer.writeStartDocument("UTF-8", "1.0");
		Writer xml = new Writer(xml_writer);
		xml.writeProject(proj);
		xml_writer.writeEndDocument();
	}

	static class Reader {
		XMLStreamReader xml;

		public Reader(XMLStreamReader xml) {
			this.xml = xml;
		}

		void nextStartElement() throws XMLStreamException {
			if (xml.nextTag() != XMLStreamReader.START_ELEMENT) {
				throw new XMLStreamException("No start tag found");
			}

		}

		void nextEndElement() throws XMLStreamException {
			if (xml.nextTag() != XMLStreamReader.END_ELEMENT) {
				throw new XMLStreamException("No end tag found");
			}

		}

		void nextStartElement(String name) throws XMLStreamException {
			nextStartElement();
			if (!xml.getNamespaceURI().equals(NS)) {
				throw new XMLStreamException("Incorrect namespace");
			}
			if (!xml.getLocalName().equals(name)) {
				throw new XMLStreamException("Got tag " + xml.getLocalName() + ", expected " + name + " at line "
						+ xml.getLocation().getLineNumber());
			}
		}

		void nextEndElement(String name) throws XMLStreamException {
			nextEndElement();
			if (!xml.getNamespaceURI().equals(NS)) {
				throw new XMLStreamException("Incorrect namespace");
			}
			if (!xml.getLocalName().equals(name)) {
				throw new XMLStreamException("Got tag " + xml.getLocalName() + ", expected " + name + " at line "
						+ xml.getLocation().getLineNumber());
			}
		}

		int getIntValue(String value) throws XMLStreamException {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new XMLStreamException("Illegal integer attribute value ");
			}
		}

		static class AttributeLookup {
			static class AttributeNotFoundException extends XMLStreamException {

				private static final long serialVersionUID = 1L;

				public AttributeNotFoundException(String msg) {
					super(msg);
				}
			}

			TreeMap<String, String> attrs = new TreeMap<String, String>();
			String tagName;
			int line;

			public AttributeLookup(XMLStreamReader reader) {
				int attr_count = reader.getAttributeCount();

				for (int a = 0; a < attr_count; a++) {
					String attr = reader.getAttributeLocalName(a);
					String value = reader.getAttributeValue(a);
					attrs.put(attr, value);
				}
				tagName = reader.getLocalName();
				line = reader.getLocation().getLineNumber();
			}

			String get(String name) throws AttributeNotFoundException {

				if (!attrs.containsKey(name)) {
					throw new AttributeNotFoundException(
							"No attribute " + name + " found for element" + tagName + " at line " + line);
				}
				return attrs.get(name);
			}

			String get(String name, String def) {
				String value = attrs.get(name);
				return (value != null) ? value : def;

			}

			boolean has(String name) {
				return attrs.containsKey(name);

			}
			int getInteger(String name) throws XMLStreamException {
				try {
					return Integer.parseInt(get(name));
				} catch (NumberFormatException e) {
					throw new XMLStreamException("Illegal integer value for attribute " + name + " at line " + line);
				}
			}

			int getInteger(String name, int def) throws XMLStreamException {
				try {
					return getInteger(name);

				} catch (AttributeNotFoundException e) {
					return def;
				}
			}

			int getScreenIndex(String name) throws XMLStreamException {
				try {
					String value = get(name);

					int split = value.indexOf('.');
					if (split >= 0) {
						int screen = Integer.parseInt(value.substring(0, split));
						int sub = Integer.parseInt(value.substring(split + 1));
						return sub * 256 + screen;
					} else {
						return Integer.parseInt(value);
					}
				} catch (NumberFormatException e) {
					throw new XMLStreamException(
							"Illegal screen index value for attribute " + name + " at line " + line);
				}
			}

			Color getColor(String name) throws XMLStreamException {
				String s = get(name);
				try {
					return Color.parseColor(s);
				} catch (Color.ParseException e) {
					throw new XMLStreamException("Failed to parse color for attribute " 
							+ name + " at line " + line+": "+e.getMessage());
				}

			}
		}

		Variable readVariable() throws XMLStreamException {
			Variable var = new Variable();

			AttributeLookup attrs = new AttributeLookup(xml);

			var.id = attrs.getInteger("id");
			var.minValue = attrs.getInteger("min");
			var.maxValue = attrs.getInteger("max");

			var.startValue = attrs.getInteger("start");
			var.valueStep = attrs.getInteger("step", 1);
			var.control = 0;
			if (attrs.get("control").contains("encoder")) {
				var.control |= Variable.CONTROL_ENCODER;
			}
			if (attrs.get("control").contains("host")) {
				var.control |= Variable.CONTROL_HOST;
			}
			var.displayCode = (attrs.getInteger("exp", 0) & 0x0f)<<4;
			String limits = attrs.get("limits", "limit");
			var.flags = 0;
			if (limits.equals("wrap")) {
				var.flags |= Variable.FLAGS_WRAP;
			} else if (!limits.equals("limit")) {
				int line = xml.getLocation().getLineNumber();
			
				throw new XMLStreamException("Value of attribute limits must be either 'limit' or 'wrap' at line " + line);
			}
			
			nextEndElement("variable");
			return var;
		}

		Image readImage() throws XMLStreamException {
			Image image = new Image();
			AttributeLookup attrs = new AttributeLookup(xml);
			image.index = attrs.getInteger("index");
			image.x = attrs.getInteger("x");
			image.y = attrs.getInteger("y");
			image.filename = attrs.get("filename");
			nextEndElement("image");
			return image;
		}

		Ring readRing() throws XMLStreamException {
			Ring ring = new Ring();
			AttributeLookup attrs = new AttributeLookup(xml);
			ring.index = attrs.getInteger("index");
			ring.x = attrs.getInteger("x", 0);
			ring.y = attrs.getInteger("y", 0);

			ring.radius = attrs.getInteger("radius");

			ring.startAngle = attrs.getInteger("start-angle");

			ring.endAngle = attrs.getInteger("end-angle");

			ring.valueIndex = attrs.getInteger("value-id");

			ring.emptyRingImage = attrs.get("empty");

			ring.fullRingImage = attrs.get("full");

			ring.cursorImage = attrs.get("cursor");

			nextEndElement("ring");
			return ring;
		}
		
		Sector readSector() throws XMLStreamException {
			Sector sector = new Sector();
			AttributeLookup attrs = new AttributeLookup(xml);
			sector.index = attrs.getInteger("index");
			sector.x = attrs.getInteger("x", 0);
			sector.y = attrs.getInteger("y", 0);


			sector.radius2 = attrs.getInteger("radius");
			if (attrs.has("cursor-radius")) {
				sector.radius1 = attrs.getInteger("cursor-radius");
				sector.radius2 -= sector.radius1;
			} else {
				sector.radius1 = sector.radius2;
			}

			sector.startAngle = attrs.getInteger("start-angle");

			sector.endAngle = attrs.getInteger("end-angle");

			sector.valueID = attrs.getInteger("value-id");

			sector.background = attrs.getColor("background");
			sector.foreground = attrs.getColor("foreground");
			nextEndElement("sector");
			return sector;
		}
		
		Cursor readCursor() throws XMLStreamException {
			Cursor cursor = new Cursor();
			AttributeLookup attrs = new AttributeLookup(xml);
			cursor.index = attrs.getInteger("index");

			cursor.outer_radius = attrs.getInteger("outer-radius");
			cursor.inner_radius = attrs.getInteger("inner-radius", cursor.outer_radius);
			
			

			cursor.outer_color = attrs.getColor("outer-color");
			if (attrs.has("inner-color")) {
			cursor.inner_color = attrs.getColor("inner-color");
			} else {
				cursor.inner_color = cursor.outer_color;
			}
			nextEndElement("cursor");
			return cursor;
		}
		
		GenericWidget readGenericWidget() throws XMLStreamException {
			GenericWidget widget = new GenericWidget();
			AttributeLookup attrs = new AttributeLookup(xml);
			widget.index = attrs.getInteger("index");
			widget.type = attrs.getInteger("type");
			String text = xml.getElementText();
			String[] byte_str = text.trim().split("[ \\t\\n\\r]+");
			widget.data = new byte[byte_str.length];
			for (int i = 0; i < byte_str.length; i++) {
				try {
					widget.data[i] = (byte)Integer.parseInt(byte_str[i],16);
				} catch(NumberFormatException ex) {
					throw new XMLStreamException(
							"Illegal hex value ("+byte_str[i]+") at line " + xml.getLocation().getLineNumber());
			
				}
			}
			
			return widget;
		}
		
		Text readText() throws XMLStreamException {
			Text text = new Text();
			AttributeLookup attrs = new AttributeLookup(xml);
			text.index = attrs.getInteger("index");
			text.x = attrs.getInteger("x");
			text.y = attrs.getInteger("y");
			text.fontIndex = attrs.getInteger("font-index");
			text.fontSize = attrs.getInteger("font-size");
			text.color = attrs.getColor("color");
			System.err.println("Color: " + text.color);
			text.valueID = attrs.getInteger("value-id");
			text.prefix = attrs.get("prefix", null);
			text.suffix = attrs.get("suffix", null);
			nextEndElement("text");
			return text;
		}

		int readGoto() throws XMLStreamException {
			AttributeLookup attrs = new AttributeLookup(xml);
			int screen = attrs.getScreenIndex("screen");
			nextEndElement("goto");
			return screen;
		}

		int[] readSet() throws XMLStreamException {
			AttributeLookup attrs = new AttributeLookup(xml);
			int[] set = new int[2];
			set[0] = attrs.getScreenIndex("value-id");
			set[1] = attrs.getScreenIndex("value");
			nextEndElement("set");
			return set;
		}

		SwipeEvent readSwipe(SwipeEvent swipe) throws XMLStreamException {
			String startTag = xml.getLocalName();
			nextStartElement("goto");
			int screen = readGoto();
			if (swipe == null) {
				swipe = new SwipeEvent();
			}
			switch (startTag) {
			case "swipe-up":
				swipe.up = screen;
				break;
			case "swipe-down":
				swipe.down = screen;
				break;
			case "swipe-left":
				swipe.left = screen;
				break;
			case "swipe-right":
				swipe.right = screen;
				break;
			default:
				throw new XMLStreamException(
						"Unknown swipe-* element " + startTag + " at line " + xml.getLocation().getLineNumber());
			}
			nextEndElement(startTag);
			return swipe;
		}

		RotateEvent readRotate(RotateEvent rotate) throws XMLStreamException {
			String startTag = xml.getLocalName();
			nextStartElement("goto");
			int screen = readGoto();
			if (rotate == null) {
				rotate = new RotateEvent();
			}
			switch (startTag) {
			case "rotate-cw":
				rotate.CW = screen;
				break;
			case "rotate-ccw":
				rotate.CCW = screen;
				break;
			default:
				throw new XMLStreamException(
						"Unknown rotate-* element " + startTag + " at line " + xml.getLocation().getLineNumber());
			}
			nextEndElement(startTag);
			return rotate;
		}

		TapEvent readTap() throws XMLStreamException {
			TapEvent tap = new TapEvent();
			AttributeLookup attrs = new AttributeLookup(xml);

			tap.x = attrs.getInteger("x");
			tap.y = attrs.getInteger("y");
			tap.width = attrs.getInteger("width");
			tap.height = attrs.getInteger("height");
			if (xml.nextTag() != XMLStreamReader.START_ELEMENT) {
				throw new XMLStreamException("Expected start tag at line " + xml.getLocation().getLineNumber());
			}
			switch (xml.getLocalName()) {
			case "goto":
				int screen = readGoto();
				tap.action = Action.GotoScreen;
				tap.arg = screen;
				break;
			case "set":
				int[] set = readSet();
				tap.valueIndex = set[0];
				tap.arg = set[1];
				tap.action = Action.SetValue;
				break;
			}
			nextEndElement("tap");
			return tap;
		}

		Screen readScreen() throws XMLStreamException {

			Screen screen = new Screen();
			AttributeLookup attrs = new AttributeLookup(xml);
			screen.index = attrs.getScreenIndex("index");
			SwipeEvent swipe = null;
			RotateEvent rotate = null;
			while (true) {
				if (xml.nextTag() == XMLStreamReader.END_ELEMENT)
					break;
				if (xml.getLocalName().equals("variable")) {
					Variable var = readVariable();
					screen.vars.add(var);
				} else if (xml.getLocalName().equals("image")) {
					Image image = readImage();
					screen.widgets.add(image);
				} else if (xml.getLocalName().equals("text")) {
					Text text = readText();
					screen.widgets.add(text);
				} else if (xml.getLocalName().equals("cursor")) {
					Cursor cursor = readCursor();
					screen.widgets.add(cursor);
				} else if (xml.getLocalName().equals("sector")) {
					Sector sector = readSector();
					screen.widgets.add(sector);
				} else if (xml.getLocalName().equals("generic-widget")) {
					GenericWidget widget = readGenericWidget();
					screen.widgets.add(widget);
				} else if (xml.getLocalName().startsWith("swipe-")) {
					swipe = readSwipe(swipe);
				} else if (xml.getLocalName().startsWith("rotate-")) {
					rotate = readRotate(rotate);
				} else if (xml.getLocalName().equals("tap")) {
					TapEvent tap = readTap();
					screen.events.add(tap);
				} else {
					throw new XMLStreamException(
							"Expected variable, widget or event element, got " + xml.getLocalName());
				}
			}
			if (!xml.getLocalName().equals("screen")) {
				throw new XMLStreamException("Expected end of screen, got " + xml.getLocalName());
			}
			if (swipe != null) {
				screen.events.add(swipe);
			}
			if (rotate != null) {
				screen.events.add(rotate);
			}
			return screen;
		}

		public Project readProject() throws XMLStreamException {

			Project proj = new Project();
			nextStartElement("project");
			AttributeLookup attrs = new AttributeLookup(xml);
			String iface_str = attrs.get("interface");
			switch (iface_str) {
			case "USB":
				proj.iface = Project.InterfaceType.USB;
				break;
			case "CAN":
				proj.iface = Project.InterfaceType.CAN;
				break;
			default:
				throw new XMLStreamException("Unknown interface type " + iface_str);
			}
			proj.defaultScreen = attrs.getInteger("default-screen", 1);

			while (true) {
				if (xml.nextTag() == XMLStreamReader.END_ELEMENT)
					break;
				if (xml.getLocalName().equals("screen")) {
					Screen screen = readScreen();
					proj.screens.add(screen);
				} else {
					throw new XMLStreamException("Expected screen element, got " + xml.getLocalName());
				}
			}
			if (!xml.getLocalName().equals("project")) {
				throw new XMLStreamException("Expected end of project, got " + xml.getLocalName());
			}
			return proj;
		}

	}

	static public Project readProject(XMLStreamReader xml_reader) throws XMLStreamException {
		Reader xml = new Reader(xml_reader);

		return xml.readProject();

	}
}
