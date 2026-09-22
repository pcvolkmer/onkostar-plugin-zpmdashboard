import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {MockBackendInterceptor} from '../mock-backend.interceptor';
import {environment} from '../environment';
import {provideEchartsCore} from "ngx-echarts";
import * as echarts from 'echarts/core';
import { PieChart } from 'echarts/charts';
import {
    TitleComponent,
    TooltipComponent,
    LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
    TitleComponent,
    PieChart,
    TooltipComponent,
    LegendComponent,
    CanvasRenderer
]);

const providers: any[] = [
  provideBrowserGlobalErrorListeners(),
  provideRouter(routes),
  provideEchartsCore({ echarts })
];

if (environment.useMockApi) {
  console.log("Using MockBackendInterceptor");
  providers.push(
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MockBackendInterceptor,
      multi: true
    }
  );
} else {
  console.log("Production");
  providers.push(provideHttpClient());
}

export const appConfig: ApplicationConfig = {
  providers: providers
};
