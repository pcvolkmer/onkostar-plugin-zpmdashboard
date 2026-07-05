import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpResponse
} from '@angular/common/http';
import {delay, Observable, of} from 'rxjs';

@Injectable()
export class MockBackendInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    if (req.url.startsWith('/onkostar/zpm-dashboard/statistics') && req.method === 'GET') {

      const mockData = {
        anmeldungen: 42,
        empfehlungen: 39,
        consents: 41,
        primaerfaelle: [{
            year: 2024,
            count: 21,
        }, {
            year: 2025,
            count: 18,
        }, {
            year: 2026,
            count: 7,
        }],
        cases: [
          {
            pid: "12345678",
            guid: "12345678-1234-1234-1234-123456789012",
          }
        ]
      };

      return of(new HttpResponse({
        status: 200,
        body: mockData
      })).pipe(delay(100));
    }

    if (req.url.startsWith('/onkostar/zpm-dashboard/cases/') && req.method === 'GET') {

      let guid = req.url.split('/').reverse()[0] ?? "00000000-0000-0000-0000-000000000000";
      if (guid === "undefined") {
          guid = "12345678-1234-1234-1234-123456789012";
      }

      const mockData = {
              pid:  guid.split('-')[0],
              tid: 1,
              guid: guid,
              icd: `C${guid[0]}0.${guid[7]}`,
              anmeldedatum: `2025-0${guid[1]}-0${guid[4]}`,
              empfehlungsdatum: parseInt(guid[1]) % 2 == 0 ? `2025-0${guid[3]}-0${guid[5]}` : null,
              latestDokuDatum: `2025-0${guid[6]}-0${guid[7]}`,
              consentdatum: parseInt(guid[1]) % 2 == 0 ? `2025-0${guid[2]}-0${guid[3]}` : null,
      };

      return of(new HttpResponse({
        status: 200,
        body: mockData
      })).pipe(delay(1000 * Math.random()));
    }

    if (req.url.startsWith('/onkostar/zpm-dashboard/cases') && req.method === 'GET') {

      const mockData = [
          {
              pid: "12345678",
              guid: "12345678-1234-1234-1234-123456789012",
          },
          {
              pid: "23456789",
              guid: "23456789-1234-1234-1234-123456789012",
          },
          {
              pid: "34567890",
              guid: "34567890-1234-1234-1234-123456789012",
          },

      ];

      return of(new HttpResponse({
          status: 200,
          body: mockData
      })).pipe(delay(200));
    }

    return next.handle(req);
  }
}
