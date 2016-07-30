import {
    async,
    describe,
    inject,
    it
} from '@angular/core/testing';
import { TestComponentBuilder } from '@angular/compiler/testing';
import { Component } from '@angular/core';

import { XLarge } from './x-large.directive';

describe('x-large directive', () => {
    @Component({
        template: '',
        directives: [XLarge]
    })
    class TestComponent {
    }

    it('should sent font-size to x-large', async(inject([TestComponentBuilder], (tcb) => {
        return tcb.overrideTemplate(TestComponent, '<div x-large>Content</div>')
            .createAsync(TestComponent).then((fixture: any) => {
                fixture.detectChanges();

                let compiled = fixture.debugElement.nativeElement.children[0];

                expect(compiled.style.fontSize).toBe('x-large');
            });
    })));
});
