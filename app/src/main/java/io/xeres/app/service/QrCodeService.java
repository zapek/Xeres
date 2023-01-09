package io.xeres.app.service;

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
