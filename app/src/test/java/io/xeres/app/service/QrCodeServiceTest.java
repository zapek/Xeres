package io.xeres.app.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class QrCodeServiceTest
{
	@InjectMocks
	private QrCodeService qrCodeService;

	@Test
	void QrCodeService_GenerateQrCode_OK() throws NotFoundException
	{
		var message = "hello world";

		var image = qrCodeService.generateQrCode(message);

		var source = new BufferedImageLuminanceSource(image);
		var bitmap = new BinaryBitmap(new HybridBinarizer(source));

		var result = new MultiFormatReader().decode(bitmap);

		assertEquals(message, result.getText());
	}
}
