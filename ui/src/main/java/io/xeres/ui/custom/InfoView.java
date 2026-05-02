/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.ui.custom;

import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.support.util.TextFlowDragSelection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.TextFlow;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class InfoView extends ScrollPane
{
	private static final Logger log = LoggerFactory.getLogger(InfoView.class);

	@FXML
	private AsyncImageView image;

	@FXML
	private TextFlow header;

	@FXML
	private TextFlow body;

	public InfoView()
	{
		var loader = new FXMLLoader(InfoView.class.getResource("/view/custom/info_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void initialize()
	{
		TextFlowDragSelection.enableSelection(header, this);
		TextFlowDragSelection.enableSelection(body, this);
	}

	public void setLoader(Function<String, byte[]> loader)
	{
		image.setLoader(loader);
	}

	public void setInfo(List<Node> header, List<Node> body)
	{
		setInfo(header, body, null, 0, 0);
	}

	public void setInfo(List<Node> header, List<Node> body, String imageUrl, int imageWidth, int imageHeight)
	{
		if (imageUrl != null && imageWidth > 0 && imageHeight > 0)
		{
			if (image.hasLoader())
			{
				image.setFitWidth(imageWidth);
				image.setFitHeight(imageHeight);
				image.setUrl(imageUrl);
			}
			else
			{
				log.warn("InfoView has no loader set, url {} cannot be loaded. use setLoader() first", imageUrl);
			}
		}
		else
		{
			if (imageUrl != null)
			{
				log.warn("image width and height not supplied for url {}, not loading image", imageUrl);
			}
			image.setUrl(null);
			image.setFitWidth(0);
			image.setFitHeight(0);
		}

		if (CollectionUtils.isNotEmpty(header))
		{
			this.header.getChildren().setAll(header);
		}
		else
		{
			this.header.getChildren().clear();
		}
		if (CollectionUtils.isNotEmpty(body))
		{
			this.body.getChildren().setAll(body);
		}
		else
		{
			this.body.getChildren().clear();
		}
		setVvalue(getVmin()); // Needed when the content is smaller than the height, I think
	}
}
