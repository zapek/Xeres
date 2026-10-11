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

package io.xeres.app.xrs.service.mail;

public enum MailFlags
{
	/// Unused
	OUTGOING,
	/// Unused
	PENDING,
	/// Unused
	DRAFT,
	/// Unused
	UNUSED,
	/// Unused
	NEW,
	/// Unused
	TRASH,
	/// Unused
	UNREAD_BY_USER,
	/// Unused
	REPLIED,
	/// Unused
	FORWARDED,
	/// Unused
	STAR,
	/// Set for a partial mail. Partial mails are needed for mails bigger than
	/// 15000 bytes.
	PARTIAL,
	/// Set for a user request (friend invite).
	USER_REQUEST,
	/// Set for a friend recommendation.
	FRIEND_RECOMMENDATION,
	RETURN_RECEIPT,
	ENCRYPTED,
	/// Set for mails that are sent to identities.
	DISTANT,
	/// Unused
	SIGNATURE_CHECKS,
	/// Unused
	SIGNED,
	/// Unused
	LOAD_EMBEDDED_IMAGES,
	/// Unused
	DECRYPTED,
	/// Unused
	ROUTED,
	/// Unused
	PUBLISH_KEY,
	/// Unused
	SPAM
}
