import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupportRequest, SupportResponse } from '../models/support-response.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SupportService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  analyzeQuery(query: string, topK: number): Observable<SupportResponse> {
    const body: SupportRequest = { query, topK };
    return this.http.post<SupportResponse>(`${this.baseUrl}/analyze`, body);
  }

  checkHealth(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.baseUrl}/health`);
  }
}
