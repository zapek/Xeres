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
	private void start(Stage stage) throws InterruptedException
	{
		editorView = new EditorView();
		editorView.setId("editorView");
		stage.setScene(new Scene(new VBox(editorView), 640, 480));
		stage.requestFocus(); // workaround, see https://github.com/TestFX/TestFX/issues/749
		stage.show();
	}

	@Test
	@Order(1)
		// this method must be first, possibly related to the above workaround
	void EditorView_Content_Empty()
	{
		assertEquals("", editorView.getText());
	}

	@Test
	void EditorView_Content_Type(FxRobot robot)
	{
		robot.write("hello, world");
		assertEquals("hello, world", editorView.getText());
	}

	@Test
	void EditorView_Content_Bold(FxRobot robot)
	{
		robot.clickOn("#bold");
		assertEquals("****", editorView.getText());
	}

	@Test
	void EditorView_Content_Bold_Selected(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#bold");
		assertEquals("**hello**", editorView.getText());
	}

	@Test
	void EditorView_Content_Italic(FxRobot robot)
	{
		robot.clickOn("#italic");
		assertEquals("**", editorView.getText());
	}

	@Test
	void EditorView_Content_Italic_Selected(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#italic");
		assertEquals("*hello*", editorView.getText());
	}

	@Test
	void EditorView_Content_Code(FxRobot robot)
	{
		robot.clickOn("#code");
		assertEquals("``", editorView.getText());
	}

	@Test
	void EditorView_Content_Code_Selected(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#code");
		assertEquals("\thello", editorView.getText());
	}

	@Test
	void EditorView_Content_Quote(FxRobot robot)
	{
		robot.clickOn("#quote");
		assertEquals("> ", editorView.getText());
	}

	@Test
	void EditorView_Content_Quote_Selected(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#quote");
		assertEquals("> hello", editorView.getText());
	}

	@Test
	void EditorView_Content_Quote_Selected_Multiples(FxRobot robot)
	{
		robot.write("hello\nworld\nhere");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#quote");
		assertEquals("> hello\n> world\n> here", editorView.getText());
	}

	@Test
	void EditorView_Content_List(FxRobot robot)
	{
		robot.clickOn("#list");
		assertEquals("- ", editorView.getText());
	}

	@Test
	void EditorView_Content_List_Selected(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#list");
		assertEquals("\n- hello", editorView.getText());
	}

	@Test
	void EditorView_Content_Heading(FxRobot robot)
	{
		robot.clickOn("#heading");
		assertEquals("## ", editorView.getText());
	}

	@Test
	void EditorView_Content_Heading_Selected(FxRobot robot)
	{
		robot.write("hello");
		robot.press(KeyCode.CONTROL, KeyCode.A);
		robot.release(KeyCode.CONTROL, KeyCode.A);
		robot.clickOn("#heading");
		assertEquals("\n## hello", editorView.getText());
	}
}
