import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

console.log('`About` component loaded asynchronously');

@Component({
    selector: 'about',
    template: `
        <h1>About</h1>
        <div>
            For hot module reloading run
            <pre>npm run start:hmr</pre>
        </div>
        <div>
            <h3>
                patrick@AngularClass.com
            </h3>
        </div>
        <pre>this.localState = {{ localState | json }}</pre>
    `
})
export class About {
    /**
     * @type {ActivatedRoute}
     */
    public route: ActivatedRoute;

    public localState;

    /**
     * @param route
     */
    constructor (route: ActivatedRoute) {
        this.route = route;
    }

    ngOnInit () {
        this.route
            .data
            .subscribe((data:any) => {
                this.localState = data.yourData;
            });

        console.log('hello `About` component');
    }
}
