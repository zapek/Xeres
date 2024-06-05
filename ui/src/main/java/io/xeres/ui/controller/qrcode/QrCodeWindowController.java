/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.qrcode;

import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.LOCATIONS_PATH;

@Component
@FxmlView(value = "/view/qrcode/qrcode.fxml")
public class QrCodeWindowController implements WindowController
{
	public static final double PRINTER_DPI = 72.0; // JavaFX uses 72 DPI for all printers
	public static final double CREDIT_CARD_WIDTH = 3.37;
	public static final double CREDIT_CARD_HEIGHT = 2.125;

	@FXML
	private ImageView ownQrCode;

	@FXML
	private Button printButton;

	@FXML
	private Button saveButton;

	@FXML
	private Button closeButton;

	@FXML
	private Label status;

	private RSIdResponse rsIdResponse;

	private final ResourceBundle bundle;

	public QrCodeWindowController(ResourceBundle bundle)
	{
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		printButton.setOnAction(event -> showPrintSetupThenPrint(UiUtils.getWindow(event)));
		saveButton.setOnAction(event -> saveAsPng(UiUtils.getWindow(event)));
		closeButton.setOnAction(UiUtils::closeWindow);

		Platform.runLater(() -> closeButton.requestFocus());
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(ownQrCode);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing RsIdResponse");
		}

		rsIdResponse = (RSIdResponse) userData;

		ownQrCode.setImage(new Image(JavaFxApplication.getControlUrl() + LOCATIONS_PATH + "/" + 1L + "/rsId/qrCode", true));
	}

	private void showPrintSetupThenPrint(Window window)
	{
		var printerJob = PrinterJob.createPrinterJob();
		if (printerJob.showPrintDialog(window))
		{
			print(printerJob, ownQrCode);
		}
	}

	private void print(PrinterJob printerJob, ImageView qrCode)
	{
		status.textProperty().bind(printerJob.jobStatusProperty().asString());

		var loader = new FXMLLoader(getClass().getResource("/view/qrcode/qrprint.fxml"), bundle);

		try
		{
			HBox view = loader.load();

			var controller = (QrPrintController) loader.getController();

			controller.setImage(qrCode.getImage());
			controller.setProfileName(rsIdResponse.name());
			controller.setLocationName(rsIdResponse.location());

			var sizeX = CREDIT_CARD_WIDTH * PRINTER_DPI;
			var sizeY = CREDIT_CARD_HEIGHT * PRINTER_DPI;

			var pageLayout = printerJob.getPrinter().getDefaultPageLayout();

			if (sizeX > pageLayout.getPrintableWidth() || sizeY > pageLayout.getPrintableHeight())
			{
				throw new IllegalStateException("QR code card is too big for the printer paper size");
			}

			var scale = new Scale(sizeX / view.getPrefWidth(), sizeY / view.getPrefHeight());
			view.getTransforms().add(scale);

			// See https://bugs.openjdk.org/browse/JDK-8089053 about the "unexpected PG access" print out.
			// There's nothing that can be done about it and it's harmless.
			if (printerJob.printPage(view))
			{
				printerJob.endJob();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void saveAsPng(Window window)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("qrcode.save-as-png"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("file-requester.png"), "*.png"));
		var selectedFile = fileChooser.showSaveDialog(window);
		if (selectedFile != null && (!selectedFile.exists() || selectedFile.canWrite()))
		{
			var bufferedImage = SwingFXUtils.fromFXImage(ownQrCode.snapshot(null, null), null);
			try
			{
				ImageIO.write(bufferedImage, "PNG", selectedFile);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
