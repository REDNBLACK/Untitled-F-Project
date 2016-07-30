import { ActivatedRoute } from '@angular/router';
import {
    beforeEachProviders,
    describe,
    inject,
    it
} from '@angular/core/testing';

import { About } from './about.component';

describe('About', () => {
    beforeEachProviders(() => [
        {
            provide: ActivatedRoute,
            useValue: {
                data: {
                    subscribe: (fn) => fn({yourData: 'yolo'})
                }
            }
        },
        About
    ]);

    it('should log ngOnInit', inject([About], (about) => {
        spyOn(console, 'log');
        expect(console.log).not.toHaveBeenCalled();

        about.ngOnInit();
        expect(console.log).toHaveBeenCalled();
    }));
});
