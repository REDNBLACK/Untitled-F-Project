import { Injectable } from '@angular/core';
import { Http } from '@angular/http';

@Injectable()
export class Title {
    /**
     * @type {Http}
     */
    public http: Http;

    /**
     * @type {string}
     */
    private value = 'Angular 2';

    /**
     * @param http
     */
    constructor (http: Http) {
        this.http = http;
    }

    getData () {
        console.log('Title#getData(): Get Data');

        return {
            value: 'AngularClass'
        };
    }
}
