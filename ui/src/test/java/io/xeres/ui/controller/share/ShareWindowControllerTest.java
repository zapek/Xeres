/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.share;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.client.ShareClient;
import io.xeres.ui.model.share.Share;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static reactor.core.publisher.Mono.when;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
class ShareWindowControllerTest
{
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private ShareClient shareClient;

	@Spy
	private ResourceBundle resourceBundle = I18nUtils.getBundle();

	@InjectMocks
	private ShareWindowController controller;

	@Test
	void testFxmlLoading() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/file/share.fxml"), resourceBundle);

		loader.setControllerFactory(applicationContext -> controller);

		var share = new Share();
		share.setName("test");

		when(shareClient.findAll()).thenReturn(Flux.just(List.of(share)));

		Parent root = loader.load();

		assertNotNull(root);
	}
}