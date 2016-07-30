import { Injectable } from '@angular/core';
import { HmrState } from 'angular2-hmr';

@Injectable()
export class AppState {
    @HmrState() _state: any = {};

    constructor() {}

    /**
     * @returns {any}
     */
    get state (): any {
        return this._state = AppState.clone(this._state);
    }

    /**
     * @param value
     */
    set state (value: any) {
        throw new Error('do not mutate the `.state` directly');
    }

    /**
     * @param prop
     * @returns {any}
     */
    get (prop?: any): any {
        const state = this.state;

        return state.hasOwnProperty(prop) ? state[prop] : state;
    }

    /**
     * @param prop
     * @param value
     * @returns {any}
     */
    set (prop: string, value: any): any {
        return this._state[prop] = value;
    }

    /**
     * @param object
     * @returns {any}
     */
    private static clone (object: any): any {
        return JSON.parse(JSON.stringify(object));
    }
}
