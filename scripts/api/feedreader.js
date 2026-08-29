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

// A simple RSS/Atom feed reader for Xeres.
//
// Usage: load this file (e.g. from user.js) and call startFeedReader(feeds, options).
//
//   startFeedReader({
//     "https://example.com/rss": "News",
//     "https://other.example.org/feed": 2,
//     "https://third.example.org/rss": "0x0e5b3c9a1f7d4a6b"
//   }, {
//     refreshInterval: 3600
//   });

const publishedEntries = new Set();

// Resolves a board identifier to a numeric board ID. The identifier can be:
//   - a number: used directly as the board ID
//   - a string starting with "0x": treated as a GxsId, looked up in subscribed boards
//   - any other string: treated as a board name, looked up in subscribed boards
//
// @param identifier the board identifier from the feeds map
// @return the numeric board ID, or null if not found
function resolveBoardId(identifier)
{
	if (typeof identifier === 'number')
	{
		return identifier;
	}

	const boards = xeres.getAllSubscribedBoards();

	if (typeof identifier === 'string' && identifier.startsWith('0x'))
	{
		const gxsId = identifier.substring(2);
		const board = boards.find(b => b.gxsId === gxsId);
		if (board)
		{
			console.log(`[FeedReader] Resolved GxsId ${gxsId} to board '${board.name}' (id=${board.id})`);
			return board.id;
		}
		console.error(`[FeedReader] No subscribed board found for GxsId ${gxsId}`);
		return null;
	}

	const name = String(identifier).toLowerCase();
	const board = boards.find(b => b.name.toLowerCase() === name);
	if (board)
	{
		console.log(`[FeedReader] Resolved board name '${name}' to id=${board.id}`);
		return board.id;
	}
	console.error(`[FeedReader] No subscribed board found with name '${name}'`);
	return null;
}

// Loads existing messages from the given board and populates publishedEntries
// with their links so that already-published feed entries are not re-posted.
function loadPublishedEntries(boardId)
{
	try
	{
		const messages = xeres.findAllMessages(boardId, 0, 100);
		for (const msg of messages)
		{
			if (msg.link)
			{
				publishedEntries.add(msg.link);
			}
		}
		console.log(`[FeedReader] Loaded ${publishedEntries.size} existing entry link(s) from board ${boardId}`);
	}
	catch (error)
	{
		console.error(`[FeedReader] Failed to load existing messages from board ${boardId}: ${error}`);
	}
}

// Starts the feed reader. Does an initial refresh and then refreshes every
// options.refreshInterval seconds.
//
// @param feeds            a map of feed URL to a board identifier: a number
//                         (internal board ID), a GxsId string prefixed with
//                         "0x", or the board's name
// @param options          optional settings:
//   refreshInterval     delay between refreshes, in seconds (default 3600)
//   maxItemsPerRun        maximum number of new entries per feed, per refresh
//                         (default 10, so that a feed doesn't flood the board)
function startFeedReader(feeds, options)
{
	options = options || {};

	const refreshInterval = options.refreshInterval || 3600;
	const maxItemsPerRun = options.maxItemsPerRun || 10;

	const resolved = {};
	for (const [url, identifier] of Object.entries(feeds))
	{
		const boardId = resolveBoardId(identifier);
		if (boardId === null)
		{
			console.error(`[FeedReader] Skipping feed ${url}: could not resolve board '${identifier}'`);
			continue;
		}
		resolved[url] = boardId;
		loadPublishedEntries(boardId);
	}

	console.log(`[FeedReader] Starting with ${Object.keys(resolved).length} feed(s), refreshing every ${refreshInterval} seconds`);

	refreshAllFeeds(resolved, maxItemsPerRun);

	setInterval(() =>
	{
		refreshAllFeeds(resolved, maxItemsPerRun);
	}, refreshInterval * 1000);
}

function refreshAllFeeds(feeds, maxItemsPerRun)
{
	Promise.all(Object.entries(feeds).map(([url, boardId]) =>
			refreshFeed(url, boardId, maxItemsPerRun)
	)).catch(error =>
	{
		console.error(`[FeedReader] Error while refreshing feeds: ${error}`);
	});
}

async function refreshFeed(url, boardId, maxItemsPerRun)
{
	const xml = await fetchFeed(url);
	if (xml === null)
	{
		return;
	}

	const entries = parseFeed(xml);
	if (entries.length === 0)
	{
		console.warn(`[FeedReader] No entries found in ${url}`);
		return;
	}

	let published = 0;

	for (let i = entries.length - 1; i >= 0; i--)
	{
		const entry = entries[i];

		if (published >= maxItemsPerRun)
		{
			break;
		}

		// The entry link is what's stored in the board message, so it is what
		// gets recovered on startup. Fall back to the guid when there's no link.
		const entryKey = entry.link || entry.guid;

		if (publishedEntries.has(entryKey))
		{
			continue; // Already published
		}

		const image = await fetchImage(entry.imageUrl);
		const content = toMarkdown(entry.content);

		try
		{
			xeres.writeBoardMessage(boardId, entry.title, content, entry.link, image);
			publishedEntries.add(entryKey);
			published++;
			console.log(`[FeedReader] Published '${entry.title}' to board ${boardId}`);
		}
		catch (error)
		{
			console.error(`[FeedReader] Failed to publish '${entry.title}' to board ${boardId}: ${error}`);
		}
	}

	console.log(`[FeedReader] ${url}: published ${published} new entry(ies)`);
}

async function fetchFeed(url)
{
	try
	{
		const response = await fetch(url, {headers: {'User-Agent': 'Xeres-FeedReader/0.1'}});
		if (!response.ok)
		{
			console.error(`[FeedReader] ${url}: HTTP error ${response.status}`);
			return null;
		}
		return await response.text();
	}
	catch (error)
	{
		console.error(`[FeedReader] ${url}: ${error}`);
		return null;
	}
}

function parseFeed(xml)
{
	if (/<rss[\s>]/i.test(xml))
	{
		return parseRss(xml);
	}
	else if (/<feed[\s>]/i.test(xml))
	{
		return parseAtom(xml);
	}
	console.warn('[FeedReader] Unsupported feed format');
	return [];
}

function parseRss(xml)
{
	const entries = [];
	const itemRegex = /<item[\s>][\s\S]*?<\/item>/gi;
	let match;

	while ((match = itemRegex.exec(xml)) !== null)
	{
		const item = match[0];
		const title = decodeEntities(extractTag(item, 'title')) || '(untitled)';
		const link = extractTag(item, 'link') || '';
		const guid = decodeEntities(extractTag(item, 'guid')) || link || title;
		const content = extractTag(item, 'description') ||
				extractTag(item, 'content:encoded') || '';

		const enclosureUrl = extractAttribute(item, 'enclosure', 'url');
		const mediaUrl = extractAttribute(item, 'media:content', 'url') ||
				extractAttribute(item, 'media:thumbnail', 'url');
		const imageUrl = enclosureUrl || mediaUrl || extractFirstImage(content);

		entries.push({guid, title, link, content, imageUrl});
	}
	return entries;
}

function parseAtom(xml)
{
	const entries = [];
	const entryRegex = /<entry[\s>][\s\S]*?<\/entry>/gi;
	let match;

	while ((match = entryRegex.exec(xml)) !== null)
	{
		const entry = match[0];
		const title = decodeEntities(extractTag(entry, 'title')) || '(untitled)';
		const link = extractHref(entry) || '';
		const id = decodeEntities(extractTag(entry, 'id')) || link || title;

		// Prefer the <content> tag, fall back to <summary>.
		const inner = /<content[\s>][\s\S]*?<\/content>/i.exec(entry) ||
				/<summary[\s>][\s\S]*?<\/summary>/i.exec(entry);
		const content = inner ? inner[0] : '';

		const imageUrl = extractAttribute(entry, 'media:content', 'url') ||
				extractAttribute(entry, 'media:thumbnail', 'url') ||
				extractAttribute(entry, 'enclosure', 'url') ||
				extractFirstImage(content);

		entries.push({guid: id, title, link, content, imageUrl});
	}
	return entries;
}

async function fetchImage(imageUrl)
{
	if (!imageUrl)
	{
		return new ArrayBuffer(0);
	}

	try
	{
		const response = await fetch(imageUrl, {headers: {'User-Agent': 'Xeres-FeedReader/0.1'}});
		if (!response.ok)
		{
			console.warn(`[FeedReader] Couldn't fetch image ${imageUrl}: HTTP error ${response.status}`);
			return new ArrayBuffer(0);
		}
		return await response.arrayBuffer();
	}
	catch (error)
	{
		console.warn(`[FeedReader] Couldn't fetch image ${imageUrl}: ${error}`);
		return new ArrayBuffer(0);
	}
}

function toMarkdown(html)
{
	if (!html || !html.trim())
	{
		return '';
	}

	let text = html;
	text = text.replace(/<!\[CDATA\[/gi, '');
	text = text.replace(/]]>/g, '');
	text = text.replace(/<br\s*\/?>/gi, '\n');
	text = text.replace(/<\/p>/gi, '\n\n');
	text = text.replace(/<img[^>]*src="([^"]+)"[^>]*>/gi, (_, src) => `![](${src})`);
	text = text.replace(/<a[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/gi, (_, href, label) => `[${stripTags(label)}](${href})`);
	text = text.replace(/<li[^>]*>/gi, '\n- ');
	text = text.replace(/<\/li>/gi, '');
	text = text.replace(/<h([1-6])[^>]*>/gi, (_, level) => `${'#'.repeat(level)} `);
	text = text.replace(/<b[^>]*>([\s\S]*?)<\/b>/gi, (_, inner) => `**${inner}**`);
	text = text.replace(/<strong[^>]*>([\s\S]*?)<\/strong>/gi, (_, inner) => `**${inner}**`);
	text = text.replace(/<i[^>]*>([\s\S]*?)<\/i>/gi, (_, inner) => `*${inner}*`);
	text = text.replace(/<em[^>]*>([\s\S]*?)<\/em>/gi, (_, inner) => `*${inner}*`);
	text = text.replace(/<code[^>]*>([\s\S]*?)<\/code>/gi, (_, inner) => `\`${inner}\``);
	text = text.replace(/<[^>]+>/g, '');
	text = text.replace(/&nbsp;/gi, ' ');
	text = text.replace(/&amp;/gi, '&');
	text = text.replace(/&lt;/gi, '<');
	text = text.replace(/&gt;/gi, '>');
	text = text.replace(/&quot;/gi, '"');
	text = text.replace(/&#39;/gi, "'");
	text = text.replace(/\n{3,}/g, '\n\n');
	return text.trim();
}

// Extracts the text content of the first occurrence of <tag>...</tag>,
// stripping any CDATA wrapper.
function extractTag(xml, tag)
{
	const regex = new RegExp(`<${tag}[^>]*>([\\s\\S]*?)<\\/${tag}>`, 'i');
	const match = regex.exec(xml);
	if (!match)
	{
		return '';
	}
	let content = match[1].trim();
	content = content.replace(/^\s*<!\[CDATA\[/, '').replace(/]]>\s*$/, '');
	return content;
}

// Extracts the value of an attribute of the first occurrence of <tag ...>.
function extractAttribute(xml, tag, attribute)
{
	const regex = new RegExp(`<${tag}[^>]*\\s${attribute}\\s*=\\s*["']([^"']+)["']`, 'i');
	const match = regex.exec(xml);
	return match ? match[1] : '';
}

// Returns the src of the first <img> tag in an HTML fragment, or '' if there's
// no image. Used as a fallback for feeds that embed their image in the entry
// content instead of an <enclosure> or media: namespace tag.
function extractFirstImage(html)
{
	const match = /<img[^>]*\ssrc\s*=\s*["']([^"']+)["']/i.exec(html || '');
	return match ? match[1] : '';
}

// Extracts the href of the first <link> element that has a rel="alternate"
// attribute (or any <link> if none has one), which is the Atom entry URL.
function extractHref(xml)
{
	const links = [...xml.matchAll(/<link[^>]*>/gi)];
	for (const link of links)
	{
		if (/rel\s*=\s*["']alternate["']/i.test(link[0]))
		{
			const match = /\shref\s*=\s*["']([^"']+)["']/i.exec(link[0]);
			if (match)
			{
				return match[1];
			}
		}
	}
	const match = /\shref\s*=\s*["']([^"']+)["']/i.exec(links[0] ? links[0][0] : '');
	return match ? match[1] : '';
}

// Removes any remaining HTML tags from a string.
function stripTags(text)
{
	return text.replace(/<[^>]+>/g, '').trim();
}

// Decodes the most common HTML entities in a plain text field.
function decodeEntities(text)
{
	return text
			.replace(/&nbsp;/gi, ' ')
			.replace(/&amp;/gi, '&')
			.replace(/&lt;/gi, '<')
			.replace(/&gt;/gi, '>')
			.replace(/&quot;/gi, '"')
			.replace(/&#39;/gi, "'")
			.replace(/&#x27;/gi, "'")
			.replace(/&#0*39;/gi, "'");
}
