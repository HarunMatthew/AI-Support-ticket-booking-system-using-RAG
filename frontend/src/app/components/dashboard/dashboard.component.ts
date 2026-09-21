import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QueryFormComponent } from '../query-form/query-form.component';
import { ResultComponent } from '../result/result.component';
import { TicketContextComponent } from '../ticket-context/ticket-context.component';
import { SupportService } from '../../services/support.service';
import { SupportResponse } from '../../models/support-response.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, QueryFormComponent, ResultComponent, TicketContextComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  loading = false;
  loadingStage = '';
  errorMessage = '';
  result: SupportResponse | null = null;

  constructor(private supportService: SupportService) {}

  onSubmitQuery(payload: { query: string; topK: number }): void {
    this.loading = true;
    this.errorMessage = '';
    this.result = null;
    this.loadingStage = 'Searching Endee...';

    // Give the "Searching Endee..." stage a brief moment to render before
    // the "Generating AI response..." stage, providing the two-stage
    // unified loading experience described in the UI spec.
    const stageTimer = setTimeout(() => {
      this.loadingStage = 'Generating AI response...';
    }, 500);

    this.supportService.analyzeQuery(payload.query, payload.topK).subscribe({
      next: (res) => {
        clearTimeout(stageTimer);
        this.result = res;
        this.loading = false;
        this.loadingStage = '';
      },
      error: (err) => {
        clearTimeout(stageTimer);
        this.loading = false;
        this.loadingStage = '';
        if (err.status === 0) {
          this.errorMessage =
            'Could not reach the backend. Make sure the Spring Boot server is running on http://localhost:8080.';
        } else if (err.error?.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Something went wrong while analyzing your query. Please try again.';
        }
      }
    });
  }
}
