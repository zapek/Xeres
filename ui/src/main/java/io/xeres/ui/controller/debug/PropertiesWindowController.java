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

package io.xeres.ui.controller.debug;

import io.xeres.ui.controller.WindowController;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@FxmlView(value = "/view/debug/properties.fxml")
public class PropertiesWindowController implements WindowController
{
	@FXML
	private TextArea propertiesArea;

	@Override
	public void initialize() throws IOException
	{
		var sb = new StringBuilder();
		getSortedProperties().forEach((k, v) -> sb.append(k).append(": ").append(beautifyOutput(v)).append("\n"));
		propertiesArea.setText(sb.toString());
	}

	private LinkedHashMap<String, String> getSortedProperties()
	{
		var properties = System.getProperties();

		return properties.entrySet().stream()
				.collect(Collectors.toMap(k -> (String) k.getKey(), e -> (String) e.getValue()))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	/**
	 * Shows the line separators and put entries separated with ; on a different line.
	 *
	 * @param in the input string
	 * @return the beautified string
	 */
	private String beautifyOutput(String in)
	{
		in = in.replace("\n", "\\n");
		in = in.replace("\r", "\\r");
		in = in.replace(";", ";\n    ");
		return in;
	}
}
