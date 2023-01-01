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

package io.xeres.ui.support.util;

import io.xeres.testutils.TestUtils;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUtilsTest
{
	private static Image image;

	@BeforeAll
	static void setup()
	{
		image = new Image(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/avatar_16.png")));
	}

	@Test
	void ImageUtils_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ImageUtils.class);
	}

	@Test
	void ImageUtils_WriteImageAsPngData_OK()
	{
		var pngImage = ImageUtils.writeImageAsPngData(image, 2048);

		assertEquals("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABxUlEQVR4XmNgwAQcKioqsZGRkWvy8/OflpSUfE5JSTkJ5C/U1dUNRVeMAvj5+Y3T0tLezJs37/+SJUswcG9v7/+oqKh9QKUS6HpBgDEhIeEKuiZsOCcn5zE7O7s6im5eXl6bKVOmYCjGhePj4/ejGODk5NSArggfbmho+AvUJgw3ABhQB9EV4cMLFiz4r6SkFAU3IDc39xa6IkI4Ojp6LdyAzMxMrAYA/fo/Li4OzF68ePH/hQsXgsWqq6v/Z2VlnYMb4ODg0IyuGYSDgoLAGMTu7u4Gs/X09P4D08V/YJQjDODi4jKZMGECWOG0adMwDIJhU1NTOLusrOw73AAQiIiI2AWVwNAIw87Ozv+nTp0KZjc2Nv5HMYCFhcUDlAqLior+L1q0CK4J5DI/Pz+ws+3s7EBOB4sDA3E7igFAoAdS3Nra+r+iogJuACjw+vr6/jc3N4Nd19bWBkoHn0VFRQ1QdHNwcDjNmjXrf21tLTiU6+vrwQYAE9l/S0tLOPb09PwfExOzCkUzCPj6+vbBnAyiQbYWFhZihAMwU/3R0NDwRdfPnJ6efgmkAOQFmOKZM2f+B6aR/+Xl5WAMjPvH6urq/jBNALtG678cuzTOAAAAAElFTkSuQmCC",
				pngImage);
	}

	@Test
	void ImageUtils_WriteImageAsJpegData_OK()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(image, 2048);

		assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAoHBwgHBgoICAgLCgoLDhgQDg0NDh0VFhEYIx8lJCIfIiEmKzcvJik0KSEiMEExNDk7Pj4+JS5ESUM8SDc9Pjv/2wBDAQoLCw4NDhwQEBw7KCIoOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozv/wAARCAAQABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDE+HWiWZ0c6lNBHLPLIwVnXOxRxgfjmtTxRoGm6vo0ssccIuEQtBKhA3MATtz3zisT4a3N3/Z91FtLW6TqVGeclTuH6Ka1da1CGwR7qS1hFvbxyLChCDLHGMDcSDkf3QeTQB//2Q==",
				jpegImage);
	}

	@Test
	void ImageUtils_WriteImageAsJpegDataWithLimit_OK()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(image, 256);

		assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAFA3PEY8MlBGQUZaVVBfeMiCeG5uePWvuZHI////////////////////////////////////////////////////2wBDAVVaWnhpeOuCguv/////////////////////////////////////////////////////////////////////////wAARCAAQABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwBlsg2biMk06WNXQkYz2NMtidpHbNOdgvJAwOlAH//Z",
				jpegImage);
	}
}
