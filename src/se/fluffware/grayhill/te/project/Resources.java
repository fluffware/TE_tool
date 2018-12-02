package se.fluffware.grayhill.te.project;

public class Resources {
	static public String imageName(int screen, int instance)
	{
		return "image_"+screen+"_"+instance+".png";
	}
	static public String fontName(int index)
	{
		return "font_"+index+".ttf";
	}
}
