# 🤖 AI Customer Support Intelligence System

A RAG-based (Retrieval-Augmented Generation) customer support assistant built using Angular, Spring Boot, Vector Search, and Gemini.

## 🚀 Project Overview

Customer support teams often receive repeated queries related to billing, delivery, account issues, and technical problems.

This application uses Retrieval-Augmented Generation (RAG) to:

- Understand the customer's query
- Generate a semantic embedding
- Search similar historical support tickets
- Retrieve the most relevant tickets
- Provide the retrieved context to Gemini
- Predict the support category
- Generate a professional response

## 🏗️ Architecture

Customer Query
      ↓
Angular Frontend
      ↓
Spring Boot REST API
      ↓
Embedding Service
      ↓
all-MiniLM-L6-v2
      ↓
Vector Search
      ↓
Top-K Similar Tickets
      ↓
Gemini LLM
      ↓
Category + Response
      ↓
Angular UI

## ✨ Features

- 🔎 Semantic search over historical support tickets
- 🧠 RAG-based response generation
- 🤖 Gemini-powered category prediction
- 💬 Automatic professional response generation
- 🗄️ Endee vector database support
- ⚡ Local cosine-similarity fallback
- 🌐 REST APIs
- 🎨 Responsive Angular dashboard
- 🛡️ Centralized exception handling
- 🔄 Configurable Top-K retrieval
- 📊 Similar-ticket context display

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 18, TypeScript |
| Backend | Java 21, Spring Boot 3.3 |
| Build Tool | Maven |
| Embeddings | DJL + all-MiniLM-L6-v2 |
| Vector Database | Endee |
| LLM | Gemini 2.5 Flash |
| API | REST |
| Data Format | JSON |

## 📁 Project Structure

```text
ai-customer-support/
│
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/support/
│       ├── SupportApplication.java
│       ├── controller/
│       ├── service/
│       ├── model/
│       ├── dto/
│       ├── config/
│       └── exception/
│
└── frontend/
    └── src/app/
        ├── components/
        ├── services/
        └── models/
