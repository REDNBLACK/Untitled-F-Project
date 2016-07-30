import { Component, ViewEncapsulation } from '@angular/core';

import { AppState } from './app.service';

@Component({
    selector: 'app',
    encapsulation: ViewEncapsulation.None,
    styleUrls: [
        './app.style.css'
    ],
    template: `
    <nav>
      <span>
        <a [routerLink]=" ['./'] ">
          Index
        </a>
      </span>
      |
      <span>
        <a [routerLink]=" ['./home'] ">
          Home
        </a>
      </span>
      |
      <span>
        <a [routerLink]=" ['./detail'] ">
          Detail
        </a>
      </span>
      |
      <span>
        <a [routerLink]=" ['./about'] ">
          About
        </a>
      </span>
    </nav>

    <main>
      <router-outlet></router-outlet>
    </main>

    <pre class="app-state">this.appState.state = {{ appState.state | json }}</pre>
  `
})
export class App {
    /**
     * @type {AppState}
     */
    public appState: AppState;

    /**
     * @param appState
     */
    constructor (appState: AppState) {
        this.appState = appState;
    }

    ngOnInit () {
        console.log('Initial App State', this.appState.state);
    }
}
