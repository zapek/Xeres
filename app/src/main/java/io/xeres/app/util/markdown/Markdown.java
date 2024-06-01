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

package io.xeres.app.util.markdown;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Markdown
{
	private final List<String> body = new ArrayList<>();
	private String render;

	public enum HeaderSize
	{
		H1, H2, H3, H4, H5, H6
	}

	public void addParagraph(String text)
	{
		if (isBlank(text))
		{
			return;
		}
		invalidate();

		addEmptyLineBeforeIfNeeded();
		body.add(text.trim());
		addEmptyLine();
	}

	public void addText(String text)
	{
		if (isBlank(text))
		{
			return;
		}
		invalidate();

		if (body.isEmpty())
		{
			body.add(text.trim());
		}
		else
		{
			var lastLine = body.removeLast();
			body.add(lastLine + (lastLine.isBlank() ? "" : " ") + text.trim());
		}
	}

	public void breakLine()
	{
		if (lastLineIsEmpty())
		{
			if (body.size() >= 2 && !body.get(body.size() - 2).isBlank())
			{
				addEmptyLine();
			}
		}
		else
		{
			addEmptyLine();
		}
	}

	public void addHeader(String text, HeaderSize size)
	{
		if (isBlank(text))
		{
			return;
		}
		invalidate();

		addEmptyLineBeforeIfNeeded();
		var header = switch (size)
		{
			case H1 -> "#";
			case H2 -> "##";
			case H3 -> "###";
			case H4 -> "####";
			case H5 -> "#####";
			case H6 -> "######";
		};
		body.add(header + " " + text.trim());
		addEmptyLine();
	}

	public void addList(String text)
	{
		if (isBlank(text))
		{
			return;
		}
		invalidate();

		addEmptyLine();
		body.add(text);
		addEmptyLine();
	}

	public void addListItem(String text)
	{
		if (isBlank(text))
		{
			return;
		}
		invalidate();

		body.add("    - " + text.trim());
	}

	public void addUrl(String text, String url)
	{
		if (isBlank(url))
		{
			return;
		}
		invalidate();

		if (!isBlank(text) && !text.equals(url))
		{
			body.add("[" + text + "](" + url + ")");
		}
		else
		{
			body.add(url);
		}
	}

	public void addImage(String title, String data)
	{
		if (isBlank(title) && isBlank(data))
		{
			return;
		}

		if (isNotBlank(data) && !data.startsWith("data:"))
		{
			return;
		}
		invalidate();

		body.add("![" + title + "](" + data + ")");
	}

	private void addEmptyLine()
	{
		invalidate();

		body.add("");
	}

	private void addEmptyLineBeforeIfNeeded()
	{
		if (!lastLineIsEmpty())
		{
			addEmptyLine();
		}
	}

	private boolean lastLineIsEmpty()
	{
		return getLastLine().isEmpty();
	}

	private String getLastLine()
	{
		if (body.isEmpty())
		{
			return "";
		}
		return body.getLast();
	}

	private void invalidate()
	{
		render = null;
	}

	private void removeTrailingEndOfLine()
	{
		if (!body.isEmpty() && body.getLast().isBlank())
		{
			body.removeLast();
		}
	}

	@Override
	public String toString()
	{
		if (render == null)
		{
			var sb = new StringBuilder();

			removeTrailingEndOfLine();

			for (var line : body)
			{
				sb.append(line);
				sb.append("\n");
			}
			render = sb.toString();
		}
		return render;
	}
}
