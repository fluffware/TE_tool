	package se.fluffware.grayhill.te.project;
	
	
	public class Color {
		public static class ParseException extends Exception
		{
			private static final long serialVersionUID = 1L;
	
			ParseException(String msg) 
			{
				super(msg);
			}
		}
		
		// 0 - 255
		public int red;
		public int green;
		public int blue;
		
		public String toString() {
			return String.format("#%02X%02X%02X", red, green, blue);
		}
	
		public static Color parseColor(String str) throws ParseException{
			if (str.length() != 7) {
				throw new ParseException("Color values must be exactly 7 characters long");
			}
			if (!str.substring(0, 1).equals("#")) {
				throw new ParseException("Color hex triple must start with #");
			}
			try {
				Color color = new Color();
				color.red = Integer.parseUnsignedInt(str.substring(1, 3), 16);
				color.green = Integer.parseUnsignedInt(str.substring(3, 5), 16);
				color.blue = Integer.parseUnsignedInt(str.substring(5, 7), 16);
				return color;
			} catch (NumberFormatException e) {
				throw new ParseException("Illegal hex color value");
			}
		}
		
		
	}
	
