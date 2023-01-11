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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@FxmlView(value = "/view/qrcode/camera.fxml")
public class CameraWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(CameraWindowController.class);

	@FXML
	private ImageView capturedImage;

	private boolean stopCamera;
	private AddRsIdWindowController parentController;
	private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();

	@Override
	public void initialize() throws IOException
	{
		stopCamera = false;

		var camera = Webcam.getDefault();

		if (camera != null)
		{
			initializeCamera(camera);
		}
		// XXX: no camera found
	}

	@Override
	public void onHidden()
	{
		stopCamera();
	}

	@Override
	public void onShown()
	{
		parentController = (AddRsIdWindowController) capturedImage.getScene().getRoot().getUserData();
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

				log.debug("Supported camera resolutions: {}", Arrays.stream(camera.getViewSizes())
						.map(Dimension::toString)
						.collect(Collectors.joining(",")));

				camera.setViewSize(WebcamResolution.VGA.getSize()); // XXX: we should only set a supported resolution. 640x480 seems to be the maximum on my 720p (which is a lie, but drivers...)
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

							if (result != null)
							{
								rsId = result.getText();
								log.debug("Found qr code: {}", rsId);
								stopCamera();
							}
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
					catch (Exception e)
					{
						log.error("Exception: {}", e.getMessage(), e);
					}
				}
				log.debug("Stopping camera...");
				camera.close();

				if (rsId != null)
				{
					parentController.setRsId(rsId);
					Platform.runLater(() -> UiUtils.closeWindow(capturedImage));
				}
				return null;
			}
		};
		capturedImage.imageProperty().bind(imageProperty);
		new Thread(cameraInitializer).start();
	}

	private void stopCamera()
	{
		stopCamera = true;
	}
}
