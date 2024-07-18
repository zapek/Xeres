package io.xeres.ui.controller.file;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class FileProgressDisplay
{
	private final SimpleStringProperty name;
	private final SimpleDoubleProperty progress;
	private final SimpleLongProperty totalSize;
	private final SimpleStringProperty hash;

	public FileProgressDisplay(String name, double progress, long totalSize, String hash)
	{
		this.name = new SimpleStringProperty(name);
		this.progress = new SimpleDoubleProperty(progress);
		this.totalSize = new SimpleLongProperty(totalSize);
		this.hash = new SimpleStringProperty(hash);
	}

	public String getName()
	{
		return name.get();
	}

	public SimpleStringProperty nameProperty()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name.set(name);
	}

	public double getProgress()
	{
		return progress.get();
	}

	public SimpleDoubleProperty progressProperty()
	{
		return progress;
	}

	public void setProgress(double progress)
	{
		this.progress.set(progress);
	}

	public long getTotalSize()
	{
		return totalSize.get();
	}

	public SimpleLongProperty totalSizeProperty()
	{
		return totalSize;
	}

	public void setTotalSize(long totalSize)
	{
		this.totalSize.set(totalSize);
	}

	public String getHash()
	{
		return hash.get();
	}

	public SimpleStringProperty hashProperty()
	{
		return hash;
	}

	public void setHash(String hash)
	{
		this.hash.set(hash);
	}
}
