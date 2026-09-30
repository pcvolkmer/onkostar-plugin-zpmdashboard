import { Routes } from '@angular/router';
import {AllCasesComponent} from "./dashboard/all-cases/cases";
import {PrimeCasesComponent} from "./dashboard/prime-cases/cases";
import {MvCasesComponent} from "./dashboard/mv-cases/cases";

export const routes: Routes = [
    {
        path: '',
        component: PrimeCasesComponent
    },
    {
        path: 'prime',
        component: PrimeCasesComponent
    },
    {
        path: 'all',
        component: AllCasesComponent
    },
    {
        path: 'mv',
        component: MvCasesComponent
    }
];
