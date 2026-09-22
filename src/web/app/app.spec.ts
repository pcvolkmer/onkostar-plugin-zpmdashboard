import {TestBed} from '@angular/core/testing';
import {App} from './app';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {MockBackendInterceptor} from '../mock-backend.interceptor';
import {provideRouter} from "@angular/router";
import {provideEchartsCore} from "ngx-echarts";

import * as echarts from 'echarts/core';

import { PieChart } from 'echarts/charts';

import {
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components';

import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
  PieChart,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer
]);

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

describe('App', () => {
  beforeEach(async () => {
    globalThis.ResizeObserver =
        ResizeObserverMock as unknown as typeof ResizeObserver;

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        {
          provide: HTTP_INTERCEPTORS,
          useClass: MockBackendInterceptor,
          multi: true
        },
        provideRouter([
          { path: '', component: App }
        ]),
        provideEchartsCore({ echarts })
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('main')?.textContent).toContain('Statistiken');
  });
});
