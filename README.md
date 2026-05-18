<div align="center">

# 🧠 DocMind
### AI-Powered Document Q&A Platform

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)

**Upload any PDF → Ask questions → Get AI-powered answers**

*100% free · Groq LLaMA 3 · Ollama local embeddings · PGVector*

</div>

---

## 💡 What does this project do?

```
  📄 You               🧠 DocMind                    💬 You
  ────                 ───────────                   ────

  Upload PDF    ──►    Reads & understands   ──►     Ask any question
                       your PDF using AI             Get accurate answers
                                                     with page citations
```

> Think of it as **ChatGPT for your own documents** — your PDF never leaves your machine for embeddings!

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  🌐  React Frontend   (localhost:3000)                       │
│      Login · Register · PDF Upload · Chat UI                 │
└───────────────────────────┬─────────────────────────────────┘
                            │  HTTP + JWT Bearer Token
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  ⚙️  Spring Boot Backend   (localhost:8080)                  │
│                                                              │
│  🔐 Spring Security ── JwtAuthFilter ── BCrypt               │
│                                                              │
│  📥 AuthController    📄 DocumentController  💬 ChatController│
│       │                      │                      │        │
│  🔑 AuthService    📦 IngestionService    🤖 ChatService      │
│                                                              │
│  ✨ ──────────── Spring AI Layer ────────────────────────    │
│     PagePdfDocumentReader · TokenTextSplitter · VectorStore  │
│     ChatClient · MessageChatMemoryAdvisor                    │
└──────────┬───────────────────┬──────────────────┬───────────┘
           │                   │                  │
           ▼                   ▼                  ▼
    🐘 PostgreSQL         🦙 Ollama           ⚡ Groq
    users table           nomic-embed-text    llama-3.3-70b
    vector_store          768-dim · FREE      free tier · fast
```

---

## 📤 How PDF Upload Works

```
  📄 Your PDF
      │
      ▼
  ┌──────────────────────────────────────┐
  │  1️⃣  Read PDF                         │
  │     Pages extracted one by one        │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │  2️⃣  Split into chunks               │
  │     ~500 words each with overlap      │
  │     Small chunks = accurate answers   │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │  3️⃣  Convert to numbers (Ollama) 🦙  │
  │     "Once upon a time..."             │
  │      → [0.12, -0.87, 0.43 ... × 768] │
  │     Numbers capture MEANING of text   │
  └──────────────────┬───────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────┐
  │  4️⃣  Save to PGVector 🐘             │
  │     text + numbers stored per chunk   │
  │     Ready for lightning-fast search   │
  └──────────────────────────────────────┘
```

---

## 💬 How Asking a Question Works (RAG)

```
  ❓ "What is the story about?"
        │
        ▼
  ┌─────────────────────────────────────┐
  │  Convert question → numbers  🦙     │
  │  Same Ollama model, locally, FREE   │
  └────────────────┬────────────────────┘
                   │
                   ▼
  ┌─────────────────────────────────────┐
  │  Find closest chunks  🔍            │
  │  Compare question vs all chunks     │
  │  Pick TOP 4 most similar            │
  │  (cosine similarity in PGVector)    │
  └────────────────┬────────────────────┘
                   │
                   ▼
  ┌─────────────────────────────────────┐
  │  Build RAG prompt  📝               │
  │  "Answer ONLY using this context:   │
  │   {chunk1} {chunk2} {chunk3}        │
  │   Question: What is this about?"    │
  └────────────────┬────────────────────┘
                   │
                   ▼
  ┌─────────────────────────────────────┐
  │  Groq AI answers ⚡                  │
  │  llama-3.3-70b reads the 4 chunks   │
  │  NOT your whole PDF — fast!         │
  └────────────────┬────────────────────┘
                   │
                   ▼
  ✅ "The story is about a rabbit and tortoise..."
     📄 Source: story.pdf — Page 1
```

> 💡 **Why not just send the whole PDF?**
> AI has a token limit. RAG solves this by sending only the 4 most relevant pieces — making answers faster and more accurate.

---

## 🔐 How JWT Auth Works

```
  REGISTER / LOGIN                    EVERY PROTECTED REQUEST
  ────────────────                    ───────────────────────

  email + password                    request + "Bearer eyJ..."
        │                                       │
        ▼                                       ▼
  verify password                     JwtAuthFilter checks token
  (BCrypt)                                      │
        │                              ┌────────┴────────┐
        ▼                              │                 │
  generate JWT token               ✅ Valid          ❌ Invalid
        │                           allow              401
        ▼                           request         Unauthorized
  { "token": "eyJhbGci..." }
```

---

## 📁 Project Structure

```
src/main/java/com/docmind/
│
├── 🚀 DocmindApplication.java
│
├── ⚙️  config/
│   └── AppConfig.java              ← wires Groq for chat, Ollama for embeddings
│
├── 🌐 controller/
│   ├── AuthController.java         ← /api/auth/register, /login
│   ├── DocumentController.java     ← /api/documents/upload, /ask
│   └── ChatController.java         ← /api/chat/start, /ask, /history
│
├── 🧠 service/
│   ├── AuthService.java            ← register + login + JWT
│   ├── DocumentIngestionService.java ← PDF → chunk → embed → save
│   ├── DocumentQAService.java      ← question → search → Groq → answer
│   └── ChatService.java            ← RAG + multi-turn memory
│
├── 🔐 security/
│   ├── JwtService.java             ← create and verify JWT tokens
│   ├── JwtAuthFilter.java          ← checks token on every request
│   ├── SecurityConfig.java         ← public vs protected endpoints
│   └── UserDetailsServiceImpl.java ← loads user from database
│
├── 🗄️  entity/
│   └── User.java                   ← users table in PostgreSQL
│
├── 📦 repository/
│   └── UserRepository.java         ← findByEmail, existsByEmail
│
├── 📋 dto/
│   └── AuthDtos.java               ← request/response shapes
│
└── 💾 model/
    └── ChatSession.java            ← conversation history in memory
```

---

## 🔌 API Reference

### 🔓 Public Endpoints (no token needed)

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| `POST` | `/api/auth/register` | `{name, email, password}` | `{token, name, email}` |
| `POST` | `/api/auth/login` | `{email, password}` | `{token, name, email}` |
| `GET` | `/api/documents/health` | — | `{status: "UP"}` |

### 🔒 Protected Endpoints (JWT required)

> Add header: `Authorization: Bearer <your-token>`

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| `POST` | `/api/documents/upload` | form-data: `file` | `{chunksStored, status}` |
| `POST` | `/api/documents/ask` | `{question}` | `{answer, sources}` |
| `POST` | `/api/chat/start` | — | `{sessionId}` |
| `POST` | `/api/chat/ask` | `{sessionId, question}` | `{answer, sources, turnNumber}` |
| `GET` | `/api/chat/history/{id}` | — | `{history: [...]}` |

---

## 🚀 Setup & Run

### Prerequisites
- ☕ Java 21+
- 🐋 Docker + Docker Compose
- 🦙 [Ollama](https://ollama.com) installed
- ⚡ Groq API key → [console.groq.com](https://console.groq.com) (free)

### Step 1 — Pull embedding model
```bash
ollama pull nomic-embed-text
```

### Step 2 — Start the database
```bash
docker compose up -d
```

### Step 3 — Set Groq API key
```bash
# Mac/Linux
export GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxx

# Windows PowerShell
$env:GROQ_API_KEY="gsk_xxxxxxxxxxxxxxxxxxxx"
```

### Step 4 — Run backend
```bash
mvn spring-boot:run
```

### Step 5 — Run frontend
```bash
cd docmind-frontend
npm install && npm start
# Opens at http://localhost:3000
```

---

## 🧪 Postman Test Flow

```
1️⃣  POST /api/auth/register     → create your account
2️⃣  POST /api/auth/login        → copy the token from response
3️⃣  POST /api/documents/upload  → upload a PDF  (add token to header)
4️⃣  POST /api/chat/start        → copy the sessionId
5️⃣  POST /api/chat/ask          → ask a question
6️⃣  POST /api/chat/ask          → ask a follow-up (it remembers! 🧠)
7️⃣  GET  /api/chat/history/{id} → see the full conversation
```

## 📊 Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| 🌐 Frontend | React 18 + plain CSS | Chat UI, file upload |
| ⚙️ Backend | Spring Boot 3.3.4 | REST API |
| 🤖 AI Framework | Spring AI 1.0.0-M3 | RAG, embeddings, memory |
| ⚡ LLM | Groq llama-3.3-70b | Answer generation (free) |
| 🦙 Embeddings | Ollama nomic-embed-text | Local vectorization (free) |
| 🔍 Vector DB | PGVector (PostgreSQL) | Similarity search |
| 🔐 Auth | Spring Security + JJWT | JWT stateless auth |
| 🐘 Database | PostgreSQL 16 | Users + vectors |
| 🐋 DevOps | Docker Compose | Local environment |
| ☕ Language | Java 21 | — |

---

## 🎯 Phases

| Phase | Feature | Status |
|-------|---------|--------|
| Phase 1 | Core RAG — upload PDF, ask questions | ✅ Done |
| Phase 2 | Chat Memory — follow-ups, source citations | ✅ Done |
| Phase 3 | Auth + React UI — JWT, login, chat frontend | ✅ Done |
| Phase 4 | Deploy — Docker full stack, Railway/Render | ⬜ Upcoming |

---

## 📝 Resume Talking Points

- Implemented **RAG architecture** from scratch — PDF ingestion, token chunking, vector embedding, cosine similarity retrieval, prompt augmentation
- Integrated **two AI providers** (Groq cloud LLM + Ollama local embeddings) in one Spring Boot app; resolved Spring autoconfiguration ambiguity via manual `@Bean` wiring
- Used **PGVector** with HNSW indexing and cosine distance for fast semantic search over document embeddings
- Built **stateless JWT authentication** with Spring Security — BCrypt hashing, per-request token validation via `OncePerRequestFilter`
- Implemented **multi-turn conversational memory** per session using Spring AI's `MessageWindowChatMemory`
- Applied **prompt engineering** to ground LLM answers strictly in document context — preventing hallucination

---

<div align="center">

Made with ❤️ using Spring AI · Groq · Ollama · PGVector

</div>
