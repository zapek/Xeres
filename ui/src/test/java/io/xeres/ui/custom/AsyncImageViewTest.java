/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom;

import io.xeres.ui.client.GeneralClient;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith({ApplicationExtension.class, SpringExtension.class})
class AsyncImageViewTest
{
	private AsyncImageView asyncImageView;

	@Start
	private void start(Stage stage)
	{
		asyncImageView = new AsyncImageView();
		stage.setScene(new Scene(new VBox(asyncImageView), 256, 256));
		stage.show();
	}

	@Mock
	private GeneralClient generalClient;

	@Test
	void LoadUrl_Success() throws IOException
	{
		var url = "/foo/bar.jpg";
		var data = Objects.requireNonNull(AsyncImageViewTest.class.getResourceAsStream("/image/icon.png")).readAllBytes();

		when(generalClient.getImage(url)).thenReturn(Mono.just(data));

		asyncImageView.loadUrl(url, path -> generalClient.getImage(path)
				.block());

		await().atMost(Duration.ofSeconds(1)).until(() -> asyncImageView.getImage() != null);
	}

	@Test
	void LoadUrl_Null_ThrowsException()
	{
		assertThrows(IllegalArgumentException.class, () -> asyncImageView.loadUrl("", path -> null));
	}
}