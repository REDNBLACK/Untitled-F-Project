import {
    beforeEachProviders,
    inject,
    it
} from '@angular/core/testing';

import { App } from './app.component';
import { AppState } from './app.service';

describe('App', () => {
    beforeEachProviders(() => [
        AppState,
        App
    ]);

    it('should have a url', inject([App], (app) => {
        expect(app.url).toEqual('https://twitter.com/AngularClass');
    }));
});
