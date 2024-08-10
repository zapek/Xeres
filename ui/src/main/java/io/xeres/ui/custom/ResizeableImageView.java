package io.xeres.ui.custom;

/**
 * Modified image class that allows unlimited up-scaling.
 */
public class ResizeableImageView extends AsyncImageView
{
	private static final double MINIMUM_SIZE = 32.0;

	public ResizeableImageView()
	{
		super();
		setPreserveRatio(false);
	}

	@Override
	public double minWidth(double height)
	{
		return MINIMUM_SIZE;
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
		return MINIMUM_SIZE;
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
