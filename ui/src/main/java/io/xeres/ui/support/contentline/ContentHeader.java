package io.xeres.ui.support.contentline;

import javafx.scene.Node;
import javafx.scene.text.Text;

public class ContentHeader implements Content
{
	private final Text node;

	public ContentHeader(String text, int size)
	{
		node = new Text(text);
		node.setStyle("-fx-font-size: " + getHeaderFontSize(size) + "px;");
	}

	@Override
	public Node getNode()
	{
		return node;
	}

	private static int getHeaderFontSize(int size)
	{
		return switch (size)
		{
			case 1 -> 32;
			case 2 -> 24;
			case 3 -> 18;
			case 4 -> 16;
			case 5 -> 13;
			case 6 -> 10;
			default -> throw new IllegalStateException("Header size " + size + " is bigger than the maximum of 6");
		};
	}
}
