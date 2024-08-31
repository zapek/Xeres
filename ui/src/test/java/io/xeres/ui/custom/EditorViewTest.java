/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EditorViewTest
{
	private EditorView editorView;

	@Start
	private void start(Stage stage)
	{
		editorView = new EditorView();
		editorView.setId("editorView");
		stage.setScene(new Scene(new VBox(editorView), 640, 480));
		stage.show();
	}

	@Test
	@Order(1)
		// this method must be first, possibly related to the above workaround
	void Content_Empty()
	{
		assertEquals("", editorView.getText());
	}

	@Test
	void Content_Type_Echoed(FxRobot robot)
	{
		robot.write("hello, world");
		assertEquals("hello, world", editorView.getText());
	}

	@Test
	void Content_Bold_Transformed(FxRobot robot)
	{
		robot.clickOn("#bold");
		assertEquals("****", editorView.getText());
	}

	@Test
	void Content_Bold_Selected_Transformed(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#bold");
		assertEquals("**hello**", editorView.getText());
	}

	@Test
	void Content_Italic_Transformed(FxRobot robot)
	{
		robot.clickOn("#italic");
		assertEquals("__", editorView.getText());
	}

	@Test
	void Content_Italic_Selected_Transformed(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#italic");
		assertEquals("_hello_", editorView.getText());
	}

	@Test
	void Content_Code_Transformed(FxRobot robot)
	{
		robot.clickOn("#code");
		assertEquals("``", editorView.getText());
	}

	@Test
	void Content_Code_Selected_Transformed(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#code");
		assertEquals("\thello", editorView.getText());
	}

	@Test
	void Content_Quote_Transformed(FxRobot robot)
	{
		robot.clickOn("#quote");
		assertEquals("> ", editorView.getText());
	}

	@Test
	void Content_Quote_Selected_Transformed(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#quote");
		assertEquals("> hello", editorView.getText());
	}

	@Test
	void Content_Quote_Selected_Multiples_Transformed(FxRobot robot)
	{
		robot.write("hello\nworld\nhere");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#quote");
		assertEquals("> hello\n> world\n> here", editorView.getText());
	}

	@Test
	void Content_List_Transformed(FxRobot robot)
	{
		robot.clickOn("#list");
		assertEquals("- ", editorView.getText());
	}

	@Test
	void Content_List_Selected_Transformed(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#list");
		assertEquals("\n- hello", editorView.getText());
	}

	@Test
	void Content_Heading_Transformed(FxRobot robot)
	{
		robot.clickOn("#heading");
		robot.clickOn("#header2");
		assertEquals("## ", editorView.getText());
	}

	@Test
	void Content_Heading_Selected_Transformed(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#heading");
		robot.clickOn("#header2");
		assertEquals("\n## hello", editorView.getText());
	}
}
