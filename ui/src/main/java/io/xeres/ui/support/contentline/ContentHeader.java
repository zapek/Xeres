package io.xeres.ui.support.contentline;

import javafx.scene.Node;
import javafx.scene.text.Text;

public class ContentHeader implements Content
{
	public enum HeaderStyle {
		H1,
		H2,
		H3,
		H4,
		H5,
		H6
	}
	private final Text node;

	public ContentHeader(String text, HeaderStyle style)
	{
		node = new Text(text);
		node.setStyle("-fx-font-size: " + getHeaderFontSize(style) + "px;");
	}

	@Override
	public Node getNode()
	{
		return node;
	}

	private static int getHeaderFontSize(HeaderStyle headerStyle)
	{
		return switch (headerStyle)
		{
			case H1 -> 32;
			case H2 -> 24;
			case H3 -> 18;
			case H4 -> 16;
			case H5 -> 13;
			case H6 -> 10;
		};
	}
}
