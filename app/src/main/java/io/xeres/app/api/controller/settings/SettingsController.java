/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.service.SettingsService;
import io.xeres.common.dto.settings.SettingsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.xeres.app.database.model.settings.SettingsMapper.toDTO;
import static io.xeres.common.rest.PathConfig.SETTINGS_PATH;

@Tag(name = "Settings", description = "Persisted settings", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/settings", description = "Settings documentation"))
@RestController
@RequestMapping(value = SETTINGS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class SettingsController
{
	private final SettingsService settingsService;
	private final ObjectMapper objectMapper;

	public SettingsController(SettingsService settingsService, ObjectMapper objectMapper)
	{
		this.settingsService = settingsService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/")
	@Operation(summary = "Get the current settings.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public SettingsDTO getSettings()
	{
		return toDTO(settingsService.getSettings());
	}

	@PatchMapping(path = "/", consumes = "application/json-patch+json")
	public ResponseEntity<SettingsDTO> updateSettings(@RequestBody JsonPatch jsonPatch)
	{
		try
		{
			var newSettings = applyPatchToSettings(jsonPatch, settingsService.getSettings());
			settingsService.updateSettings(newSettings);
			return ResponseEntity.ok(toDTO(newSettings));
		}
		catch (JsonPatchException | JsonProcessingException e)
		{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private Settings applyPatchToSettings(JsonPatch patch, Settings targetSettings) throws JsonPatchException, JsonProcessingException
	{
		var patched = patch.apply(objectMapper.convertValue(targetSettings, JsonNode.class));
		return objectMapper.treeToValue(patched, Settings.class);
	}
}
