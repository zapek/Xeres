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

package io.xeres.chess.app.api.controller.chess;

import io.xeres.app.service.IdentityService;
import io.xeres.chess.app.xrs.service.chess.ChessContactsStore;
import io.xeres.chess.app.xrs.service.chess.ChessHistoryStore;
import io.xeres.chess.app.xrs.service.chess.ChessRatingService;
import io.xeres.chess.app.xrs.service.chess.ChessRsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static io.xeres.chess.ChessPaths.CHESS_PATH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChessController.class)
@org.springframework.test.context.ContextConfiguration(classes = ChessController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChessControllerTest
{
	private final org.springframework.test.web.servlet.MockMvc mvc;

	@org.springframework.beans.factory.annotation.Autowired
	ChessControllerTest(org.springframework.test.web.servlet.MockMvc mvc)
	{
		this.mvc = mvc;
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getJson(String uri)
	{
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(uri)
				.accept(org.springframework.http.MediaType.APPLICATION_JSON);
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJson(String uri, String body)
	{
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(uri)
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.accept(org.springframework.http.MediaType.APPLICATION_JSON).content(body);
	}

	private static final String BASE_URL = CHESS_PATH;
	private static final String PEER_A = "11".repeat(16);

	@MockitoBean
	private ChessRsService chessRsService;

	@MockitoBean
	private ChessHistoryStore historyStore;

	@MockitoBean
	private ChessRatingService ratingService;

	@MockitoBean
	private ChessContactsStore contactsStore;

	@MockitoBean
	private IdentityService identityService;

	@Test
	void GetContacts_Success() throws Exception
	{
		when(chessRsService.contacts()).thenReturn(List.of(new io.xeres.chess.common.dto.chess.ChessContactDTO(PEER_A, "Peer A", "available", "2026-03-01T12:00:00Z")));

		mvc.perform(getJson(BASE_URL + "/contacts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].gxsId").value(PEER_A))
				.andExpect(jsonPath("$.[0].status").value("available"));

		verify(chessRsService).contacts();
	}

	@Test
	void activeGamesAndWatchLifecycle() throws Exception
	{
		var id = io.xeres.common.id.GxsId.fromString(PEER_A);
		var match = new io.xeres.chess.common.dto.chess.ChessActiveGameDTO(PEER_A, "game-id", "Alice", "22".repeat(16), "Bob", false);
		var watch = new io.xeres.chess.common.dto.chess.ChessWatchDTO(PEER_A, "game-id", "", "", "", true, 0, List.of(), -1, -1, "WAITING", "");
		when(chessRsService.activeGames()).thenReturn(List.of(match));
		when(chessRsService.watch(id, "game-id")).thenReturn(watch);
		when(chessRsService.watchedGame(id)).thenReturn(watch);
		mvc.perform(getJson(BASE_URL + "/active")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].local").value(false)).andExpect(jsonPath("$[0].opponentName").value("Bob"));
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BASE_URL + "/" + PEER_A + "/watch").param("gameId", "game-id"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WAITING"));
		mvc.perform(getJson(BASE_URL + "/" + PEER_A + "/watch")).andExpect(status().isOk());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(BASE_URL + "/" + PEER_A + "/watch"))
				.andExpect(status().isNoContent());
		verify(chessRsService).leaveWatch(id);
	}

	@Test
	void IsBusy_Success() throws Exception
	{
		when(chessRsService.isBusy()).thenReturn(true);

		mvc.perform(getJson(BASE_URL + "/busy"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true));

		verify(chessRsService).isBusy();
	}

	@Test
	void SetBusy_Success() throws Exception
	{
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BASE_URL + "/busy").param("busy", "true"))
				.andExpect(status().isNoContent());

		verify(chessRsService).setBusy(true);
	}

	@Test
	void AddContact_Success() throws Exception
	{
		mvc.perform(postJson(BASE_URL + "/contacts/" + PEER_A, ""))
				.andExpect(status().isNoContent());

		verify(contactsStore).add(PEER_A);
	}

	@Test
	void RemoveContact_Success() throws Exception
	{
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(BASE_URL + "/contacts/" + PEER_A))
				.andExpect(status().isNoContent());

		verify(contactsStore).remove(PEER_A);
	}
}
