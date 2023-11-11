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
		image = new Image(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/avatar_32.png")));
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

		assertEquals("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAEKUlEQVR4XrVXSWiTURAO4qVoc5B68RSwYhF6cSG0lx6EWtqQXoSGxARK11ACpbX7RktD93SLSfemewPeFA9FwYsXRRRF9OBBRFA8KIhVQexzvkde+DP5kyZdPhj+/P97b755M/NmXgyGNGE2my81NjZ6urq6ggMDA3eHh4d3hoaG7nV3dwdaW1ubqqqqrNnZ2ef5ukPDaDReIJJQMBj8s7KyItbW1sT6+rrY2NiQgnd8X1paEjRnd3Bw8KHdbr/B9aSNwsLCKx0dHXcIP0ACMhAnEmUQZG5uTrS3t/tJTQbXmxKKiorypqen/6ZCrCdYA69QqJ6QOhPXvx/OUHyfHoSYG4Hn+Pj4x5KSknxOkhDkuuDq6mqcwoMKDPH7/btOpzOlvMgYGRl5v7m5GafoMAJ9o6Oj70h/JieMQXl5uWVhYeFAcU8m0BcKhUR9ff1tzhmDzs5OnzpmRy0wgrz7jGhOcN4oKPkeHfXulUAvHenvRHOO8ypk0dH7fJwGzM7O/qP6YubEEsXFxVcx4bgMgCC8fX19Yc4tQQl4Y3Fx8cgTUCs4DVNTU1+JLovzG6qrq+2o58dpAHRTv/h1kcD5DTU1Na7l5eW0DdivXGMMO4fgN7xcWVl5k/MbSktLzdREUs4BEFdUVAir1Sr0agfeI4knHA6H6OnpkUbAy7W1tTbOD2RS3f7AFekJ5qDZFBQUCFonAoGAVK7GMUauFltbW8Lr9co5FotFroMBLpcrj5NLUCHyp1qIoAxtF+T8O9xMJNG7Qm9vL/qBNBJhJg84OLcEhSEfSlPxgiLT6xv4TnGOegaeUPMwRm36JedWOElN44UyAM/5+Xm501SNgoCQLjNy5/itHYs0pi+cOAo0DDQOtTu4DvFMx4DIeYer4zyEd+oJnzivFqdpwhutATMzM2kZgLnYBN0B5JOv7e/vv89JY0ALnUgkGIAQkMvilHDBuDIabg+Hw8Lj8YiJiYmY+OMUuN3uW5wzBmVlZZeJWNYECHlEJLslYayhoUHWBZA2NTXhUipIj2hra4vmAXTRZl4RxSnOGQNaeI2KS9QAaqNSeDy1gmKEcFEtkR6j67ksPsgFpQe7r6urc3O+OMAArQegGAUFJHpGKLdvb2/HiXb3dPwecy5d2Gw2syqvEJxnHEXsihuBYoMd02VGPsfGxqQXsHMYjnWYR8890nudc+kClUo1JmUAkhJkINK6NdLjZdybm5vlE3lAiSaLEblcVkMK4TdSfZZz6YLc/UDtECTIZO1xwjt2qsZVCJTgHaI1ko7ejiHZfVADVMPXikxPoBy1Ad7ACUg0VxlAoXtuSHX3JpMph1z+UylNphw1gv4hy5arDNMKXE9H+G1ubm4O50kI6ogh7T9glOHJyUldQ/ANHsA4DEGSUqbDM3s+n+93S0uLl1QaOQfwH0i6EkrbyhpxAAAAAElFTkSuQmCC",
				pngImage);
	}

	@Test
	void ImageUtils_WriteImageAsJpegData_OK()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(image, 2048);

		assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAoHBwgHBgoICAgLCgoLDhgQDg0NDh0VFhEYIx8lJCIfIiEmKzcvJik0KSEiMEExNDk7Pj4+JS5ESUM8SDc9Pjv/2wBDAQoLCw4NDhwQEBw7KCIoOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozv/wAARCAAgACADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDyjQ9Kk1rV4LCM48xvnbH3VHU/574r2DSvDmlaNGVtLVQx+9I/zMfxP8q4X4YQI+s3U5PzRwbQPqef5V6dQBVutMsb6Ix3VpDMh7OgNeYeMPBp0H/TbRzJZO+NpBLRZ7E9x7/SvV0kSTdsdW2na2DnB9DWX4ls01LQL6yyDIYS6ruwcjlfwyKAPLfB+q/2T4jt5myY5T5MmBk4b/6+K9mDZcj0wRXheiW73WuWUMalmadOgzxnJP4Cvcip35VgCox/+ugBiraWhcgRQGZy7dF3t3J9T0qoq2a2M93N5e5ImilnJGSi56t6d/xq1KWc/wCtSLHVXQH+tcV4+15LfTv7Ht7lHnlbM3lLtCJ1wcHqePwzQB//2Q==",
				jpegImage);
	}

	@Test
	void ImageUtils_WriteImageAsJpegDataWithLimit_OK()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(image, 256);

		assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAFA3PEY8MlBGQUZaVVBfeMiCeG5uePWvuZHI////////////////////////////////////////////////////2wBDAVVaWnhpeOuCguv/////////////////////////////////////////////////////////////////////////wAARCAAgACADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwCpGpdwoq4kaoOBUFqPnJ9qtUANKqwwQDVaaHZ8w+7/ACq1TZRujYd8UAVYW2SA9jxVyqKDLqPer1ABwPQZpvG0k46YJpx+uKguJMLsB5PXFAH/2Q==",
				jpegImage);
	}
}
