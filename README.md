# AI Customer Support Intelligence System

A RAG-based (Retrieval-Augmented Generation) customer support assistant, rebuilt from an
original Python/Streamlit prototype into a production-shaped **Angular + Spring Boot**
application.

```
Customer Query → Embed (all-MiniLM-L6-v2) → Search Endee Vector DB → Top-K Similar
Tickets → Gemini (category + response) → JSON API → Angular UI
```

---

## 1. Project Overview

Customer support teams answer the same handful of issue types over and over
(billing, delivery, account, technical, ...). This system takes a new customer
query, finds the most semantically similar tickets from a history of resolved
tickets, and gives an LLM that context so it can predict a category and draft a
consistent, professional response — instead of starting from a blank page every
time.

## 2. Features

- Semantic search over historical tickets using real sentence embeddings
  (no random/fake vectors)
- Pluggable vector store: real Endee REST backend, or a local in-memory
  cosine-similarity store for demo/offline use
- Gemini-powered category prediction + response drafting, with a safe mock
  fallback when no API key is configured
- Clean REST API (`/api/support/analyze`, `/api/support/health`)
- Polished, responsive Angular dashboard with loading states, category badges,
  and a semantic-match context panel
- Centralized error handling on both backend and frontend

## 3. Architecture

```
┌────────────────┐      POST /api/support/analyze      ┌─────────────────────┐
│  Angular App    │ ───────────────────────────────────▶│ SupportController    │
│  (localhost:4200)│◀─────────────────────────────────── │ (Spring Boot)        │
└────────────────┘              JSON response            └──────────┬───────────┘
                                                                      │
                                                                      ▼
                                                            ┌──────────────────┐
                                                            │   QueryEngine     │
                                                            └────────┬──────────┘
                                                     ┌────────────────┴───────────────┐
                                                     ▼                                 ▼
                                          ┌───────────────────┐             ┌────────────────────┐
                                          │  EmbeddingService  │             │  EndeeVectorStore   │
                                          │ (DJL, MiniLM-L6-v2)│────────────▶│ (real REST or mock) │
                                          └───────────────────┘  vector     └──────────┬───────────┘
                                                                                        │ Top-K tickets
                                                                                        ▼
                                                                             ┌────────────────────┐
                                                                             │   GeminiService     │
                                                                             │ category + response │
                                                                             └────────────────────┘
```

## 4. Technology Stack

| Layer      | Technology                                                    |
|------------|----------------------------------------------------------------|
| Frontend   | Angular 18 (standalone components), TypeScript, HttpClient     |
| Backend    | Java 21, Spring Boot 3.3, Maven                                 |
| Embeddings | Deep Java Library (DJL) running `all-MiniLM-L6-v2` on PyTorch  |
| Vector DB  | Endee (real REST client + local cosine-similarity fallback)    |
| LLM        | Gemini API (`gemini-2.5-flash`, configurable)                  |

## 5. Project Structure

```
ai-customer-support/
├── backend/                    Spring Boot API (Java 21, Maven)
│   ├── pom.xml
│   └── src/main/java/com/example/support/
│       ├── SupportApplication.java
│       ├── controller/SupportController.java
│       ├── service/            DataLoaderService, EmbeddingService, EndeeVectorStore,
│       │                       QueryEngine, GeminiService
│       ├── model/               Ticket, SimilarTicket, SupportRequest, SupportResponse
│       ├── dto/GeminiResponse.java
│       ├── config/              AppConfig (CORS), DataInitializer (startup indexing)
│       └── exception/GlobalExceptionHandler.java
│
└── frontend/                   Angular 18 app
    └── src/app/
        ├── components/         dashboard, query-form, result, ticket-context
        ├── services/support.service.ts
        └── models/              ticket, similar-ticket, support-response
```

## 6. Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Node.js 18+ and npm
- Angular CLI (`npm install -g @angular/cli`)
- A Gemini API key (optional — app runs with a mock LLM response without it)
- Network access the first time the backend starts, so DJL can download the
  `all-MiniLM-L6-v2` model weights (cached afterwards under `~/.djl.ai`)

## 7. Environment Variables

Copy `.env.example` to `.env` (or export these in your shell):

| Variable          | Required | Description                                             |
|-------------------|----------|----------------------------------------------------------|
| `GEMINI_API_KEY`  | No       | Enables real Gemini responses. Without it, a mock category/response is returned. |
| `ENDEE_API_TOKEN` | No       | Only used when `endee.mock-mode=false`.                  |
| `ENDEE_BASE_URL`  | No       | Base URL of your Endee deployment (default `http://localhost:9000`). |

## 8. Backend Setup

```bash
cd backend
export GEMINI_API_KEY=your_key_here   # optional
mvn spring-boot:run
```

On startup the app loads `sample_tickets.json`, generates real embeddings for
all 8 tickets, and inserts them into the vector store (Endee or the local
fallback, per `endee.mock-mode`). The backend listens on `http://localhost:8080`.

## 9. Frontend Setup

```bash
cd frontend
npm install
ng serve
```

The UI is served at `http://localhost:4200` and talks to the backend at
`http://localhost:8080`.

## 10. How to Run (both together)

```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2
cd frontend && npm install && ng serve
```

Then open `http://localhost:4200`.

## 11. API Documentation

### `POST /api/support/analyze`

**Request**
```json
{
  "query": "My product arrived damaged",
  "topK": 3
}
```

**Response**
```json
{
  "category": "Delivery",
  "response": "We are sorry to hear...",
  "searchTime": 0.142,
  "similarTickets": [
    {
      "id": "TKT-006",
      "score": 0.91,
      "query": "The product arrived damaged...",
      "category": "Delivery",
      "response": "Hi, I am so sorry..."
    }
  ]
}
```

### `GET /api/support/health`
```json
{ "status": "UP" }
```

## 12. Example cURL / Postman requests

```bash
curl -X POST http://localhost:8080/api/support/analyze \
  -H "Content-Type: application/json" \
  -d '{"query": "My product arrived damaged", "topK": 3}'

curl http://localhost:8080/api/support/health
```

## 13. RAG Workflow

1. **Embed** — the customer's query text is converted into a 384-dimensional
   vector by `EmbeddingService`, using the real `all-MiniLM-L6-v2` model
   (loaded once at startup via DJL/PyTorch, not random or hard-coded values).
2. **Retrieve** — `EndeeVectorStore.search()` compares that vector against the
   embeddings of all historical tickets (cosine similarity) and returns the
   Top-K most similar ones, with their category and past resolution.
3. **Augment** — `GeminiService` builds a prompt that includes those Top-K
   tickets as context, alongside the new query.
4. **Generate** — Gemini returns a predicted category and a drafted
   professional response as JSON, which is parsed and returned to Angular.

This is the standard RAG pattern: retrieval grounds the LLM's generation in
real, relevant examples instead of letting it answer from parametric memory
alone — which is what keeps responses consistent with how your team has
actually resolved similar issues before.

## 14. Endee Configuration

`Endee` has no publicly documented official Java SDK, so `EndeeVectorStore`
talks to it over a plain REST contract via Spring's `RestClient`
(`POST /collections/{name}/insert`, `POST /collections/{name}/search`).
If your real Endee deployment's contract differs, only `insertViaRestApi()`
and `searchViaRestApi()` in `EndeeVectorStore.java` need to change.

```properties
endee.mock-mode=true          # true = local in-memory cosine-similarity store
endee.base-url=http://localhost:9000
endee.api.token=${ENDEE_API_TOKEN}
endee.collection=support_tickets
```

`mock-mode=true` is the default so the project runs immediately with zero
external services configured — mirroring the original Python app's own
built-in `MockCollection` fallback.

## 15. Gemini Configuration

```properties
gemini.api.key=${GEMINI_API_KEY}
gemini.model=gemini-2.5-flash
```

The original Python code used `gemini-1.5-pro-latest`; that model line is
older, so the Java backend defaults to the current `gemini-2.5-flash` model
instead (fully configurable). Temperature is fixed at `0.2` for consistent,
low-variance category/response output, and the service strips ```` ```json ````
code fences before parsing Gemini's reply.

## 16. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Backend fails to start with a DJL/model error | No network access on first run — DJL needs to download `all-MiniLM-L6-v2` once. Retry with network access, or pre-populate `~/.djl.ai`. |
| Angular shows "Could not reach the backend" | Confirm `mvn spring-boot:run` is running on port 8080 and CORS `frontend.url` matches `http://localhost:4200`. |
| Gemini responses are always the mock message | `GEMINI_API_KEY` isn't set, or is still the placeholder value. |
| Vector search returns nothing | Check backend logs for "System Ready! N tickets indexed." — if 0, `sample_tickets.json` failed to load. |

---

## 17. Why This Architecture (for interviews / review)

**Controller / Service / Model separation**
- `SupportController` only handles HTTP concerns (routing, request/response
  shapes) — it has no business logic.
- `service/` classes each own one responsibility (loading data, embedding,
  vector search, retrieval orchestration, LLM calls) — classic separation of
  concerns, and each is independently testable/mockable.
- `model/` and `dto/` are plain data carriers; DTOs shape what crosses the
  API boundary, distinct from internal domain objects.
- `config/AppConfig` and `exception/GlobalExceptionHandler` centralize
  cross-cutting concerns (CORS, error formatting) instead of scattering them
  through business code.

**RAG concepts**
- *Embedding*: a dense numeric vector that captures a sentence's meaning, so
  semantically similar text ends up numerically close.
- *Vector database*: a store optimized for finding the nearest vectors to a
  query vector (here, via cosine similarity).
- *Semantic similarity*: closeness in vector space as a proxy for closeness
  in meaning, which is far more robust than keyword matching.
- *Top-K retrieval*: returning only the K most relevant historical examples,
  keeping the LLM's prompt focused and cheap.
- *Context*: the retrieved examples, injected into the LLM prompt so it can
  ground its answer in real precedent.
- *LLM generation*: the final step where Gemini uses the query + context to
  produce a category and a response.

**Spring Boot**
- *Dependency Injection / Constructor Injection*: Spring wires up
  `QueryEngine`, `EmbeddingService`, etc. by passing dependencies through
  constructors — this makes classes explicit about what they need and easy
  to unit test with mocks.
- `@RestController` / `@PostMapping`: mark a class as returning JSON directly
  and map HTTP verbs+paths to methods.
- `@Service`: marks a class as business-logic-holding, discoverable by
  component scanning.
- `@Configuration`: marks a class that declares beans (like the CORS filter).

**Angular**
- *Component*: encapsulates a template + logic + styles for one UI piece
  (e.g. `QueryFormComponent`).
- *Service*: framework-agnostic class (`SupportService`) that talks to the
  backend, injected into components via Angular's DI.
- *HttpClient*: Angular's wrapper for making HTTP calls and getting back
  typed `Observable`s.
- *TypeScript interface*: compile-time shape checking for API payloads
  (`SupportResponse`, `SimilarTicket`, ...), catching mismatches early.
- *Data binding*: `[(ngModel)]`, `[input]`, `(output)` keep the template and
  component class in sync without manual DOM manipulation.
- *Event handling*: `(ngSubmit)`, `(click)` route user actions back into
  component methods.

---

## 18. Interview Questions & Answers

**1. What does RAG stand for and why use it here instead of just prompting Gemini directly?**
Retrieval-Augmented Generation. Without retrieval, Gemini would have to guess
a category and response from the query alone. By retrieving similar past
tickets first, the response stays consistent with how the team has actually
resolved similar issues, and the model has concrete grounding instead of
purely parametric guessing.

**2. Why `all-MiniLM-L6-v2` specifically?**
It's a small, fast sentence-embedding model (384 dimensions) that gives a
strong quality/speed tradeoff for short support-ticket-length text, and was
the model specified in the original Python prototype.

**3. How are embeddings generated in the Java backend, and why not just use `new float[384]`?**
Via DJL running the real `all-MiniLM-L6-v2` PyTorch model (`EmbeddingService`).
A zero/random vector would carry no semantic information, so every similarity
search would be meaningless — the requirement was to preserve real RAG
behavior, not fake it.

**4. What happens if Endee is unreachable?**
`EndeeVectorStore` either runs in `mock-mode` (local in-memory cosine
similarity) by default, or, if a real call fails while `mock-mode=false`,
catches the exception and falls back to the same local store for that
request — so the app degrades gracefully instead of crashing.

**5. How is cosine similarity computed and why use it?**
`dot(A,B) / (‖A‖·‖B‖)`. It measures the angle between two vectors regardless
of magnitude, which is standard for comparing sentence embeddings where
direction (meaning), not length, is what matters.

**6. Why is Top-K configurable (1–5) instead of fixed?**
It lets the caller trade off context breadth vs. prompt size/cost — more
matches give the LLM more precedent but increase prompt length and can dilute
relevance if K is too high.

**7. Walk through what happens end-to-end for one query.**
Angular POSTs `{query, topK}` → `SupportController` calls `QueryEngine` →
`EmbeddingService` embeds the query → `EndeeVectorStore.search()` returns the
K nearest historical tickets → `GeminiService` builds a prompt with that
context and calls Gemini → the parsed `{category, response}` plus the
matched tickets and timing are returned as JSON → Angular renders it.

**8. Why constructor injection instead of field injection (`@Autowired` on fields)?**
It makes dependencies explicit and required, enables making fields `final`,
and makes unit testing trivial (just pass mocks into the constructor) without
needing a Spring context.

**9. What's the purpose of `GlobalExceptionHandler`?**
Centralizes error formatting into a consistent JSON shape and HTTP status
codes across the whole API, and ensures internal exception details/stack
traces are logged server-side but never leaked to the client.

**10. How does the backend avoid re-inserting duplicate tickets on every restart?**
Tickets carry stable IDs (`TKT-001`...`TKT-008`); a real vector DB insert is
expected to behave as an upsert keyed by ID. The in-memory mock store is
rebuilt fresh each restart anyway, so there's nothing to duplicate there.

**11. Why use `ApplicationRunner` instead of `@PostConstruct` for startup indexing?**
`ApplicationRunner` runs only after the entire Spring context (including the
embedding model bean) is fully initialized, whereas `@PostConstruct` on a
single bean could run before its dependencies are completely ready.

**12. How does the app handle a missing `GEMINI_API_KEY`?**
`GeminiService` checks for a blank/placeholder key up front and returns a
clearly-labeled mock response (`"General Inquiry"` / a mock message) instead
of attempting a call that would fail.

**13. How is the Gemini API called, and why not use an SDK?**
Via a direct REST call (`generateContent`) using Spring's `RestClient`. This
keeps the dependency footprint minimal and works with any current Gemini
model name via configuration, rather than depending on wrapper-library
version compatibility.

**14. Why is `temperature=0.2` used for the Gemini call?**
Lower temperature reduces randomness, which matters here because the output
must be valid, consistently-structured JSON and a fairly deterministic
category classification — not creative variation.

**15. How does the app handle Gemini returning markdown-fenced JSON?**
`GeminiService` strips ` ```json ` / ` ``` ` fences before calling
`ObjectMapper.readValue`, since LLMs frequently wrap JSON in code fences even
when told not to.

**16. What's the difference between the `Ticket` model and the `SimilarTicket` model?**
`Ticket` is the raw historical record (id/query/category/response). A
`SimilarTicket` additionally carries a similarity `score` — it's what search
results look like, not what's stored.

**17. Why validate `SupportRequest` with `@NotBlank`/`@Min`/`@Max` instead of checking manually in the controller?**
Bean Validation keeps validation rules declarative and next to the field
they apply to, and Spring automatically returns a 400 with a clear message
via `MethodArgumentNotValidException`, handled centrally.

**18. How does CORS work here, and why is it needed?**
Browsers block cross-origin XHR/fetch by default. `AppConfig` explicitly
allows the Angular dev server's origin (`http://localhost:4200`) to call
`/api/**` on the Spring Boot server (`http://localhost:8080`), which run on
different ports and are therefore different origins.

**19. In Angular, why is `SupportService` a separate injectable class instead of calling `HttpClient` directly from the component?**
Separation of concerns: the component only handles presentation/state,
while the service owns "how do I talk to the backend." This also makes the
service reusable across components and easy to mock in component tests.

**20. How would you extend this system to support streaming responses or multi-turn conversations?**
On the backend, switch the Gemini call to its streaming endpoint and expose
a Server-Sent Events or WebSocket endpoint instead of a single JSON response;
on the frontend, consume that stream incrementally in `SupportService` and
append tokens to the UI as they arrive. For multi-turn support, the
`SupportRequest` would need a conversation/session ID and prior turns would
need to be included in the Gemini prompt (or in Gemini's `contents` array as
alternating user/model turns).
#   A I - S u p p o r t - t i c k e t - b o o k i n g - s y s t e m - u s i n g - R A G  
 