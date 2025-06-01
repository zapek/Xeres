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

package io.xeres.ui.controller.debug;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
class PropertiesWindowControllerTest
{
	@Test
	void textFxmlLoading() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/debug/properties.fxml"));
		Parent root = loader.load();
		PropertiesWindowController controller = loader.getController();

		assertNotNull(controller);
		assertNotNull(root);
	}
}