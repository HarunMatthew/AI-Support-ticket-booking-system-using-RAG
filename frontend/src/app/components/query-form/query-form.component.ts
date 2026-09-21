import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-query-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './query-form.component.html',
  styleUrls: ['./query-form.component.css']
})
export class QueryFormComponent {
  @Input() loading = false;
  @Input() loadingStage = '';
  @Output() submitQuery = new EventEmitter<{ query: string; topK: number }>();

  query = '';
  topK = 3;
  topKOptions = [1, 2, 3, 4, 5];

  onSubmit(): void {
    if (!this.query.trim() || this.loading) {
      return;
    }
    this.submitQuery.emit({ query: this.query.trim(), topK: this.topK });
  }
}
