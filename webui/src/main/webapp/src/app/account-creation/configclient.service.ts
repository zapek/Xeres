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

import {Injectable} from "@angular/core";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {Observable, throwError} from "rxjs";
import {catchError} from "rxjs/operators";

@Injectable({
	providedIn: "root"
})
export class ConfigClient {
	private url = "http://localhost:1066/api/v1/config";

	constructor(private http: HttpClient) {
	}

	public createProfile(name: string): Observable<string> {
		return this.http.post<string>(this.url + "/profile", {name: name}).pipe(catchError(this.handleError));
	}

	// XXX: this is boring... any global way to do that?
	private handleError(err: HttpErrorResponse) {
		let errorMessage = "";
		if (err.error instanceof ErrorEvent) {
			errorMessage = `An error occured: ${err.error.message}`;
		} else {
			errorMessage = `Server returned code: ${err.status}, error message is: ${err.message}`;
		}
		console.error(errorMessage);
		return throwError(errorMessage);
	}
}