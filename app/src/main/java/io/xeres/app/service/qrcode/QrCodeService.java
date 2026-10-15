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

package io.xeres.app.service.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
public class QrCodeService
{
	private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);

	public BufferedImage generateQrCode(String message)
	{
		if (isEmpty(message))
		{
			log.warn("No QR code to encode because the input is empty");
			return null;
		}

		var qrCodeWriter = new QRCodeWriter();
		BitMatrix matrix;
		try
		{
			matrix = qrCodeWriter.encode(message, BarcodeFormat.QR_CODE, 256, 256);
		}
		catch (WriterException e)
		{
			log.error("Couldn't generate QR Code: {}", e.getMessage(), e);
			return null;
		}
		return MatrixToImageWriter.toBufferedImage(matrix);
	}
}
