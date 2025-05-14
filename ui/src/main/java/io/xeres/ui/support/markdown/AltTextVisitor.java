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

package io.xeres.ui.support.markdown;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;

class AltTextVisitor extends AbstractVisitor
{
	private final StringBuilder sb = new StringBuilder();

	String getAltText()
	{
		return sb.toString();
	}

	@Override
	public void visit(Text text)
	{
		sb.append(text.getLiteral());
	}

	@Override
	public void visit(SoftLineBreak softLineBreak)
	{
		sb.append('\n');
	}

	@Override
	public void visit(HardLineBreak hardLineBreak)
	{
		sb.append('\n');
	}
}
