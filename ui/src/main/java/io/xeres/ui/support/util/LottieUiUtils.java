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

package io.xeres.ui.support.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import io.xeres.common.AppName;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.LottieUtils;
import io.xeres.common.util.OsUtils;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static io.xeres.ui.support.util.DateUtils.DATE_TIME_FILENAME_FORMAT;
import static io.xeres.ui.support.util.UiUtils.getWindow;

public final class LottieUiUtils
{
	private static final Logger log = LoggerFactory.getLogger(LottieUiUtils.class);

	private static final JsonMapper JSON_MAPPER = createJsonMapper();

	private static final ContextMenu contextMenu;
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	static
	{
		var saveAsMenuItem = new MenuItem(bundle.getString("save-animation-as"));
		saveAsMenuItem.setGraphic(new FontIcon(MaterialDesignC.CONTENT_SAVE));
		saveAsMenuItem.setOnAction(LottieUiUtils::saveAs);

		contextMenu = new ContextMenu(saveAsMenuItem);
	}

	private LottieUiUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Animation decodeLottie(String dataUri)
	{
		var data = LottieUtils.readLottieData(dataUri);

		if (LottieUtils.isMimeType(dataUri, LottieUtils.TGS_MIMETYPE))
		{
			return decodeLottie(new ByteArrayInputStream(data));
		}
		else
		{
			var isLottie = LottieUtils.isMimeType(dataUri, LottieUtils.LOTTIE_MIMETYPE);
			var isLot = LottieUtils.isMimeType(dataUri, LottieUtils.JSON_MIMETYPE);

			if (isLottie || isLot)
			{
				Path tempFile = null;
				try
				{
					// XXX: having to create a temporary file is annoying. ask for a more flexible interface?
					tempFile = Files.createTempFile(AppName.NAME + "_sticker", isLottie ? ".lottie" : ".json"); // Extension is important for LottieFileLoader
					Files.write(tempFile, data);
					return LottieFileLoader.load(tempFile.toFile());
				}
				catch (IOException | LottieFileException e)
				{
					log.warn("Could not load lottie file {}", e.getMessage());
				}
				finally
				{
					if (tempFile != null)
					{
						if (!tempFile.toFile().delete())
						{
							log.warn("Couldn't delete temporary lottie file {}", tempFile.toFile().getAbsolutePath());
						}
					}
				}
			}
		}
		return null;
	}

	public static Animation decodeLottie(InputStream in)
	{
		try (var gzipInputStream = new GZIPInputStream(in))
		{
			var bytes = gzipInputStream.readAllBytes();

			return JSON_MAPPER.readValue(bytes, Animation.class);
		}
		catch (IOException | JacksonException e)
		{
			log.debug("Couldn't decode lottie: {}", e.getMessage());
			return null;
		}
	}

	public static void saveLottie(Animation json, OutputStream out) throws IOException, JacksonException
	{
		try (var gzipOutputStream = new GZIPOutputStream(out))
		{
			JSON_MAPPER.writeValue(gzipOutputStream, json);
		}
		out.close();
	}

	public static void addLottieContextMenuActions(Node node)
	{
		node.setOnContextMenuRequested(event -> {
			contextMenu.show(node, event.getScreenX(), event.getScreenY());
			event.consume();
		});
	}

	private static void saveAs(ActionEvent event)
	{
		var selectedMenuItem = (MenuItem) event.getTarget();

		var popup = Objects.requireNonNull(selectedMenuItem.getParentPopup());
		var animation = ((LottiePlayer) popup.getOwnerNode()).getAnimation();

		if (animation != null)
		{
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("file-requester.save-animation-title"));
			ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TGS", "*.tgs"));
			fileChooser.setInitialFileName("Sticker_" + DATE_TIME_FILENAME_FORMAT.format(Instant.now()));

			var selectedFile = fileChooser.showSaveDialog(getWindow(event));
			if (selectedFile != null)
			{
				try
				{
					// XXX: missing gzip compressor!
					saveLottie(animation, new FileOutputStream(selectedFile));
				}
				catch (IOException e)
				{
					Requester.showError(e.getMessage());
				}
			}
		}
	}

	private static JsonMapper createJsonMapper()
	{
		// See ObjectMapperFactory of Lottie
		return JsonMapper.builder()
				.enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
				.changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
				.build();
	}
}
