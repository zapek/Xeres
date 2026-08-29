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

// noinspection JSUnresolvedReference

// This is an example user script for Xeres.
//
// Requirements: ECMA script version 2025 in strict mode.
//
// The script has to be placed in:
//   Windows: %APPDATA%\Xeres\Scripts\user.js
//   Linux: /home/<account>/.local/share/Xeres/Scripts/user.js
//   macOS: /Users/<Account>/Library/Application Support/Xeres/Scripts/user.js

// Feedreader
//import config from "./feedreader_config.json" with { type: "json" };
//load("./scripts/api/feedreader.js");
//startFeedReader(
//		config.feeds,
//		config.options
//);

// Away message
//load("./scripts/api/away_message.js");

// Chat room triggers
//load("./scripts/api/chatroom_triggers.js");

// Interval
//load("./scripts/api/interval.js");

console.log(`User script loaded and ready. ECMA version: ${Graal.versionECMAScript}, Graal version: ${Graal.versionGraalVM}, HotCode: ${Graal.isGraalRuntime()}.`);
