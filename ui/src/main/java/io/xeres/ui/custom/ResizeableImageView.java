package io.xeres.ui.custom;

import javafx.scene.image.ImageView;

/**
 * Modified image class that allows unlimited resizing.
 */
public class ResizeableImageView extends ImageView
{
	public ResizeableImageView()
	{
		setPreserveRatio(false);
	}

	@Override
	public double minWidth(double height)
	{
		return 32;
	}

	@Override
	public double prefWidth(double height)
	{
		var image = getImage();
		if (image == null)
		{
			return minWidth(height);
		}
		return image.getWidth();
	}

	@Override
	public double maxWidth(double height)
	{
		return Double.MAX_VALUE;
	}

	@Override
	public double minHeight(double width)
	{
		return 32;
	}

	@Override
	public double prefHeight(double width)
	{
		var image = getImage();
		if (image == null)
		{
			return minHeight(width);
		}
		return image.getHeight();
	}

	@Override
	public double maxHeight(double width)
	{
		return Double.MAX_VALUE;
	}

	@Override
	public boolean isResizable()
	{
		return true;
	}

	@Override
	public void resize(double width, double height)
	{
		setFitWidth(width);
		setFitHeight(height);
	}
}
