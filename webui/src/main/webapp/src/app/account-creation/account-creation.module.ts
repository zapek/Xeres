/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import {NgModule} from '@angular/core';
import {SharedModule} from "../shared/shared.module";
import {RouterModule} from "@angular/router";
import {AccountCreationComponent} from './account-creation.component';


@NgModule({
	declarations: [AccountCreationComponent],
	imports: [
		RouterModule.forChild([
			{path: "create", component: AccountCreationComponent}
		]),
		SharedModule
	]
})
export class AccountCreationModule {
}
