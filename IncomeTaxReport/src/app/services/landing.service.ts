import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LandingService {
  // Use same-origin API path. This is ideal for production when Apache/Nginx
  // reverse-proxies /api to the Spring Boot server (avoids CORS + mixed-content).
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  saveEmployee(details: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/employee`, details);
  }

  generateReport(employeeId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/employee/${employeeId}/report`, {});
  }

  serverRoot(): string {
    return this.baseUrl.replace(/\/api$/, '');
  }
}
