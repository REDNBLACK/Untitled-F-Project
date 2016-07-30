// Angular 2
import { HashLocationStrategy, LocationStrategy } from '@angular/common';
// Angular 2 Http
import { HTTP_PROVIDERS } from '@angular/http';
// Angular 2 Router
import { provideRouter } from '@angular/router';
// Angular 2 forms
import { disableDeprecatedForms, provideForms } from '@angular/forms';

// AngularClass
import { provideWebpack } from '@angularclass/webpack-toolkit';
import { providePrefetchIdleCallbacks } from '@angularclass/request-idle-callback';

import { routes, asyncRoutes, prefetchRouteCallbacks } from '../app/app.routes';
import { APP_RESOLVER_PROVIDERS } from '../app/app.resolver';

export const APPLICATION_PROVIDERS = [
    disableDeprecatedForms(),
    provideForms(),

    ...APP_RESOLVER_PROVIDERS,

    provideRouter(routes),
    provideWebpack(asyncRoutes),
    providePrefetchIdleCallbacks(prefetchRouteCallbacks),

    ...HTTP_PROVIDERS,

    {
        provide: LocationStrategy,
        useClass: HashLocationStrategy
    }
];

export const PROVIDERS = [
    ...APPLICATION_PROVIDERS
];
