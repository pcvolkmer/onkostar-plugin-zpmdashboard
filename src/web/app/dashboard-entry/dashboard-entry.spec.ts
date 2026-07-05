import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardEntry } from './dashboard-entry';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {MockBackendInterceptor} from "../../mock-backend.interceptor";

describe('DashboardEntry', () => {
  let component: DashboardEntry;
  let fixture: ComponentFixture<DashboardEntry>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardEntry],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        {
          provide: HTTP_INTERCEPTORS,
          useClass: MockBackendInterceptor,
          multi: true
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardEntry);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render title', async () => {
    component
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled?.textContent).toContain('PID: 12345678');
  });
});
