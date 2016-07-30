import { Component } from '@angular/core';

import { AppState } from '../app.service';
import { Title } from './title';
import { XLarge } from './x-large';

@Component({
    selector: 'home',
    providers: [
        Title
    ],
    directives: [
        XLarge
    ],
    pipes: [],
    styleUrls: ['./home.style.css'],
    templateUrl: './home.template.html'
})
export class Home {
    /**
     * @type {AppState}
     */
    public appState: AppState;

    /**
     * @type {Title}
     */
    public title: Title;

    /**
     * @type {{value: string}}
     */
    private localState = {
        value: ''
    };

    /**
     * @param appState
     * @param title
     */
    constructor(appState: AppState, title: Title) {
        this.appState = appState;
        this.title = title;
    }

    ngOnInit() {
        console.log('hello `Home` component');
    }

    /**
     * @param value
     */
    submitState (value) {
        console.log('submitState', value);

        this.appState.set('value', value);
        this.localState.value = '';
    }
}
