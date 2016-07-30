import { Detail } from './detail.component';
import { Index } from './index.component';

export const routes = {
    path: 'detail',
    component: Detail,
    children: [
        {
            path: '',
            component: Index
        }
    ]
};
