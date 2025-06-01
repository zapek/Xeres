/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.id.AddRsIdWindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/qrcode/camera.fxml")
public class CameraWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(CameraWindowController.class);

	@FXML
	private ImageView capturedImage;

	@FXML
	private Label error;

	private boolean stopCamera;
	private AddRsIdWindowController parentController;
	private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
	private final ResourceBundle bundle;

	public CameraWindowController(ResourceBundle bundle)
	{
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		stopCamera = false;

		var camera = Webcam.getDefault();

		if (camera != null)
		{
			initializeCamera(camera);
		}
		else
		{
			error.setText(bundle.getString("qr-code.camera.error"));
			error.setVisible(true);
		}
	}

	@Override
	public void onHidden()
	{
		stopCamera();
	}

	@Override
	public void onShown()
	{
		parentController = (AddRsIdWindowController) UiUtils.getUserData(capturedImage);
	}

	private void initializeCamera(Webcam camera)
	{
		var cameraInitializer = new Task<Void>()
		{
			@Override
			protected Void call()
			{
				String rsId = null;
				var multiFormatReader = new MultiFormatReader();
				multiFormatReader.setHints(Map.of(
						DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)));

				// Built-in driver only supports 640x480 as maximum, but
				// this is usually enough for QR code scanning.
				camera.setViewSize(WebcamResolution.VGA.getSize());
				camera.open();
				while (!stopCamera)
				{
					try
					{
						var grabbedImage = camera.getImage();
						if (grabbedImage != null)
						{
							Platform.runLater(() -> imageProperty.set(SwingFXUtils.toFXImage(grabbedImage, null)));

							var source = new BufferedImageLuminanceSource(grabbedImage);
							var bitmap = new BinaryBitmap(new HybridBinarizer(source));

							grabbedImage.flush();

							var result = multiFormatReader.decodeWithState(bitmap);

							rsId = result.getText();
							log.debug("Found qr code: {}", rsId);
							stopCamera();
						}
						else
						{
							log.warn("Empty image!?");
						}
					}
					catch (NotFoundException e)
					{
						// No QR code was found on the image
					}
				}
				camera.close();
				imageProperty.set(null);

				if (rsId != null)
				{
					parentController.setRsId(rsId);
					Platform.runLater(() -> UiUtils.closeWindow(capturedImage));
				}
				return null;
			}
		};
		capturedImage.imageProperty().bind(imageProperty);
		Thread.ofVirtual().name("Camera Handler").start(cameraInitializer);
	}

	private void stopCamera()
	{
		stopCamera = true;
	}
}
