/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.chess;

import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/// Renders the bundled RetroChess path/circle artwork as native, scalable JavaFX shapes.
public final class ChessPieceView extends Region
{
	private final Group artwork = new Group();
	private final Scale scale = new Scale();

	public ChessPieceView(char piece)
	{
		var resource = "/view/chess/piece/" + (Character.isUpperCase(piece) ? "w" : "b") + Character.toUpperCase(piece) + ".svg";
		try (var input = Objects.requireNonNull(getClass().getResourceAsStream(resource), resource))
		{
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			append(factory.newDocumentBuilder().parse(input).getDocumentElement(), Map.of());
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Cannot load chess piece " + resource, e);
		}
		artwork.setManaged(false);
		artwork.getTransforms().add(scale);
		getChildren().add(artwork);
		setMouseTransparent(true);
		setMinSize(0, 0);
		setPrefSize(45, 45);
	}

	private void append(Element element, Map<String, String> inherited)
	{
		var style = new HashMap<>(inherited);
		for (var attribute : new String[]{"fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule"})
		{
			if (element.hasAttribute(attribute))
			{
				style.put(attribute, element.getAttribute(attribute));
			}
		}
		Shape shape = switch (element.getTagName())
		{
			case "path" ->
			{
				var path = new SVGPath();
				path.setContent(element.getAttribute("d"));
				path.setFillRule(style.getOrDefault("fill-rule", "nonzero").equals("evenodd") ? FillRule.EVEN_ODD : FillRule.NON_ZERO);
				yield path;
			}
			case "circle" -> new Circle(Double.parseDouble(element.getAttribute("cx")), Double.parseDouble(element.getAttribute("cy")), Double.parseDouble(element.getAttribute("r")));
			case "svg", "g" -> null;
			default -> throw new IllegalArgumentException("Unsupported bundled SVG element: " + element.getTagName());
		};
		if (shape != null)
		{
			shape.setFill(paint(style.getOrDefault("fill", "#000")));
			shape.setStroke(paint(style.getOrDefault("stroke", "none")));
			shape.setStrokeWidth(Double.parseDouble(style.getOrDefault("stroke-width", "1")));
			shape.setStrokeLineCap(StrokeLineCap.valueOf(style.getOrDefault("stroke-linecap", "butt").toUpperCase(Locale.ROOT)));
			shape.setStrokeLineJoin(StrokeLineJoin.valueOf(style.getOrDefault("stroke-linejoin", "miter").toUpperCase(Locale.ROOT)));
			artwork.getChildren().add(shape);
		}
		for (var child = element.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (child instanceof Element nested)
			{
				append(nested, style);
			}
		}
	}

	private Paint paint(String value)
	{
		return value.equals("none") ? null : Color.web(value);
	}

	@Override
	protected void layoutChildren()
	{
		var size = Math.min(getWidth(), getHeight());
		scale.setX(size / 45);
		scale.setY(size / 45);
		artwork.setLayoutX((getWidth() - size) / 2);
		artwork.setLayoutY((getHeight() - size) / 2);
	}
}
