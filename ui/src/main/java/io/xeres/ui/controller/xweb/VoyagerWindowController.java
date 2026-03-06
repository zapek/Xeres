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

package io.xeres.ui.controller.xweb;

import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/xweb/voyager.fxml")
public class VoyagerWindowController implements WindowController
{
	@FXML
	private Button backButton;

	@FXML
	private Button forwardButton;

	@FXML
	private Button refreshButton;

	@FXML
	private TextField urlField;

	@FXML
	private WebView webView;

	private final BuildProperties buildProperties;

	public VoyagerWindowController(BuildProperties buildProperties)
	{
		this.buildProperties = buildProperties;
	}

	@Override
	public void initialize() throws IOException
	{
		urlField.setOnAction(_ -> webView.getEngine().load(urlField.getText()));

		var engine = webView.getEngine();
		var history = webView.getEngine().getHistory();

		backButton.setOnAction(_ -> {
			if (history.getCurrentIndex() > 0)
			{
				history.go(-1);
			}
		});
		forwardButton.setOnAction(_ -> {
			if (history.getCurrentIndex() < history.getEntries().size() - 1)
			{
				history.go(1);
			}
		});
		refreshButton.setOnAction(_ -> engine.reload());

		history.currentIndexProperty().addListener((_, _, newValue) -> {
			backButton.setDisable(newValue.intValue() == 0);
			forwardButton.setDisable(history.getEntries().isEmpty() || newValue.intValue() + 1 == history.getEntries().size());
		});

		engine.setUserAgent("XeresVoyager/" + buildProperties.getVersion() + " (" + System.getProperty("os.name") + "/" + System.getProperty("os.arch") + ")");

		engine.titleProperty().addListener((_, _, newTitle) ->
				((Stage) UiUtils.getWindow(urlField)).setTitle(newTitle)
		);

		urlField.setText("xweb:test");
		urlField.fireEvent(new ActionEvent());
	}
}
