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

package io.xeres.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class UnHtmlServiceTest
{
	@InjectMocks
	private UnHtmlService unHtmlService;

	@Test
	void UnchangedMessage()
	{
		var result = unHtmlService.cleanupMessage("foo");

		assertEquals("foo", result);
	}

	@Test
	void QuotedMessage()
	{
		var result = unHtmlService.cleanupMessage("""
				<body><style type="text/css" RSOptimized="v2">.S0{font-size:11pt;}.S0{font-style:normal;}.S0{font-family:'MSSansSerif';}.S0{font-weight:400;}</style><span><span class="S0">> this is a quote<br/><br/>and that's the reply</span></span></body>
				""");

		assertEquals("""
				\\> this is a quote \s
				 \s
				and that's the reply
				""", result);
	}

	@Test
	void MultiLevelQuotes()
	{
		var result = unHtmlService.cleanupMessage("""
				>>> third
				
				>> second
				
				> first
				
				not a quote
				""");

		assertEquals("""
				>>> third
				
				>> second
				
				> first
				
				not a quote
				""", result);
	}

	@Test
	void Pre()
	{
		var result = unHtmlService.cleanupMessage("""
				<body><style type="text/css" RSOptimized="v2">.S1{margin-bottom:16px;}.S2{font-size:10pt;}.S1{margin-left:0px;}.S2{font-family:'SansSerif';}.S2{font-style:normal;}.S1{-qt-block-indent:0;}.S0{background-color:transparent;}.S2{font-weight:400;}.S0{color:#1f2328;}.S1{text-indent:0px;}.S1{background-color:#f6f8fa;}.S1{margin-top:0px;}.S1{line-height:145%;}.S0{font-family:'ui-monospace','SFMono-Regular','SFMono','Menlo','Consolas','LiberationMono','monospace';}.S1{margin-right:0px;}</style><span><span class="S2"><pre class="S1"><span class="S0">flatpak install --user https://dl.flathub.org/build-repo/189725/cc.retroshare.retroshare-gui.flatpakref</span></pre></span></span></body>""");


		assertEquals("```\nflatpak install --user https://dl.flathub.org/build-repo/189725/cc.retroshare.retroshare-gui.flatpakref\n```\n", result);
	}

	@Test
	void Code()
	{
		var result = unHtmlService.cleanupMessage("""
				<body><style type="text/css" RSOptimized="v2">.S1{margin-bottom:16px;}.S2{font-size:10pt;}.S1{margin-left:0px;}.S2{font-family:'SansSerif';}.S2{font-style:normal;}.S1{-qt-block-indent:0;}.S0{background-color:transparent;}.S2{font-weight:400;}.S0{color:#1f2328;}.S1{text-indent:0px;}.S1{background-color:#f6f8fa;}.S1{margin-top:0px;}.S1{line-height:145%;}.S0{font-family:'ui-monospace','SFMono-Regular','SFMono','Menlo','Consolas','LiberationMono','monospace';}.S1{margin-right:0px;}</style><span><span class="S2"><code class="S1"><span class="S0">flatpak install --user https://dl.flathub.org/build-repo/189725/cc.retroshare.retroshare-gui.flatpakref</span></code></span></span></body>""");


		assertEquals("`flatpak install --user https://dl.flathub.org/build-repo/189725/cc.retroshare.retroshare-gui.flatpakref`\n", result);
	}

	@Test
	void CodeWithLanguage()
	{
		var result = unHtmlService.cleanupMessage("""
				<body>
					<pre>
						<code class="language-java">System.out.println("hello world");</code>
					</pre>
				</body>
				"""
		);

		assertEquals("""
				```java
				System.out.println("hello world");
				```
				""", result);
	}
}