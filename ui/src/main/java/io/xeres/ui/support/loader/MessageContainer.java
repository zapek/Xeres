/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.loader;

import io.xeres.ui.controller.common.GxsMessage;

import java.util.List;

interface MessageContainer<M extends GxsMessage>
{
	void clear();

	/// Inserts a new message
	///
	/// @param message a new incoming message
	/// @return true if the message has been inserted, false if it has updated an already existing entry
	boolean insert(M message);

	/// Sets the read status of a message.
	///
	/// @param messageId the message id, it can be null, in that case, every message in the group is concerned
	/// @param read      true if read
	void setMessageReadState(Long messageId, boolean read);

	void addBefore(List<M> messages);

	void addAfter(List<M> messages);

	int getLowerBound();

	int getHigherBound();

	int getTotal();
}
