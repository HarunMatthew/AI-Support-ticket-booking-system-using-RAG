import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SimilarTicket } from '../../models/similar-ticket.model';

@Component({
  selector: 'app-ticket-context',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ticket-context.component.html',
  styleUrls: ['./ticket-context.component.css']
})
export class TicketContextComponent {
  @Input() tickets: SimilarTicket[] = [];

  scorePercent(score: number): number {
    return Math.round(score * 100);
  }
}
