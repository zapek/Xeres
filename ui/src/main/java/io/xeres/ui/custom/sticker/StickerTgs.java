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

package io.xeres.ui.custom.sticker;

import io.xeres.common.util.LottieUtils;
import io.xeres.ui.support.util.LottieUiUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

class StickerTgs extends StickerLottieAbstract
{
	public StickerTgs(Path filePath)
	{
		super(filePath);
		try
		{
			animation = LottieUiUtils.decodeLottie(new FileInputStream(filePath.toFile()));
		}
		catch (FileNotFoundException e)
		{
			log.debug("Couldn't open TGS, file not found: {}", e.getMessage());
		}
	}

	@Override
	public String generateBase64Data(byte[] data)
	{
		return LottieUtils.writeLottieData(LottieUtils.TGS_MIMETYPE, data);
	}
}
