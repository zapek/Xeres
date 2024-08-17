/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.file;

import io.xeres.common.rest.file.AddDownloadRequest;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.ui.client.FileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.ReadOnlyTextField;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/file/add_download.fxml")
public class FileAddDownloadViewController implements WindowController
{
	@FXML
	private ReadOnlyTextField name;

	@FXML
	private ReadOnlyTextField size;

	@FXML
	private ReadOnlyTextField hash;

	@FXML
	private Button downloadButton;

	@FXML
	private Button cancelButton;

	private final FileClient fileClient;
	private final ResourceBundle bundle;

	public FileAddDownloadViewController(FileClient fileClient, ResourceBundle bundle)
	{
		this.fileClient = fileClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize() throws IOException
	{
		cancelButton.setOnAction(UiUtils::closeWindow);

		Platform.runLater(this::handleArgument);
	}

	private void handleArgument()
	{
		var args = (AddDownloadRequest) name.getScene().getRoot().getUserData();
		if (args == null)
		{
			throw new IllegalArgumentException("Missing user data");
		}

		name.setText(args.name());
		size.setText(ByteUnitUtils.fromBytes(args.size()));
		TooltipUtils.install(size, MessageFormat.format(bundle.getString("download.add.bytes"), args.size()));
		hash.setText(args.hash().toString());

		downloadButton.setOnAction(event -> fileClient.download(args.name(),
						args.hash(),
						args.size(),
						args.locationId())
				.doOnSuccess(aLong -> Platform.runLater(() -> UiUtils.closeWindow(name)))
				.subscribe());
	}
}
