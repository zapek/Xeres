/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import com.github.fge.jsonpatch.JsonPatch;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.SettingsService;
import io.xeres.common.dto.settings.SettingsDTO;
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

	public SettingsController(SettingsService settingsService)
	{
		this.settingsService = settingsService;
	}

	@GetMapping
	@Operation(summary = "Get the current settings.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public SettingsDTO getSettings()
	{
		return settingsService.getSettings();
	}

	@PatchMapping(consumes = "application/json-patch+json")
	public ResponseEntity<SettingsDTO> updateSettings(@RequestBody JsonPatch jsonPatch)
	{
		var newSettings = settingsService.applyPatchToSettings(jsonPatch);
		return ResponseEntity.ok(toDTO(newSettings));
	}
}
