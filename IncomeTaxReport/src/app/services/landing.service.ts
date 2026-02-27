import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LandingService {
  // Default to backend server for local development. Change to '/api' if using an Angular proxy.
  private baseUrl = 'http://localhost:8080/api';

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
