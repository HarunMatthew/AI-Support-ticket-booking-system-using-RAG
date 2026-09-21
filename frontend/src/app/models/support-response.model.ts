import { SimilarTicket } from './similar-ticket.model';

export interface SupportResponse {
  category: string;
  response: string;
  searchTime: number;
  similarTickets: SimilarTicket[];
}

export interface SupportRequest {
  query: string;
  topK: number;
}
