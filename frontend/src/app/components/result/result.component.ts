import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SupportResponse } from '../../models/support-response.model';

@Component({
  selector: 'app-result',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './result.component.html',
  styleUrls: ['./result.component.css']
})
export class ResultComponent {
  @Input() result: SupportResponse | null = null;

  categoryClass(category: string): string {
    const key = (category || '').toLowerCase().replace(/[^a-z]/g, '-');
    return `badge-${key || 'default'}`;
  }
}
