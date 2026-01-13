# Easy RAG Flow with RetrievalAugmentorProducer

```mermaid
---
config:
  layout: dagre
---
flowchart TB
    Start(["User sends question"]) --> ChatbotResource["ChatbotResource.chat"]
    ChatbotResource --> BankingChatbot["BankingChatbot.chat<br>@RegisterAiService"] & End(["Return ChatResponse"])
    BankingChatbot --> CDI["CDI Container"] & RAGPipeline["RAG Pipeline"]
    CDI --> Producer["RetrievalAugmentorProducer<br>@Produces RetrievalAugmentor"]
    Producer --> InjectStore["Inject EmbeddingStore<br>auto-created by easy-rag"] & InjectEmbedModel["Inject EmbeddingModel<br>nomic-embed-text"] & InjectTransformer["Inject StandaloneQueryTransformer<br>@ApplicationScoped"]
    InjectStore --> CreateRetriever["Create EmbeddingStoreContentRetriever<br>.embeddingStore<br>.embeddingModel<br>.maxResults(3)"]
    InjectEmbedModel --> CreateRetriever
    CreateRetriever --> BuildAugmentor["DefaultRetrievalAugmentor.builder<br>.queryTransformer<br>.contentRetriever"]
    InjectTransformer --> BuildAugmentor
    BuildAugmentor --> RetrievalAugmentor["RetrievalAugmentor"]
    RAGPipeline --> TransformQuery["1. Query Transformation<br>StandaloneQueryTransformer.transform"]
    TransformQuery --> GetMetadata["Extract chatMemory from<br>Query.metadata().chatMemory()"]
    GetMetadata --> CheckMemory{"Has Chat<br>Memory?"}
    CheckMemory -- No --> LogSkip["Log: Query unchanged<br>(no chat memory)"]
    LogSkip --> ReturnOriginal["Return original query"]
    CheckMemory -- Yes --> FormatMemory["Format chat history<br>User: ... / AI: ..."]
    FormatMemory --> CreatePrompt["Create prompt template<br>with conversation context"]
    CreatePrompt --> LLMTransform["ChatModel.chat<br>Rewrite query to be<br>self-contained"]
    LLMTransform --> LogTransform["Log: Before/After<br>transformation + timing"]
    LogTransform --> TransformedQuery["Transformed Query<br>with preserved metadata"]
    ReturnOriginal --> TransformedQuery
    TransformedQuery --> Retrieve["2. Content Retrieval<br>ContentRetriever.retrieve"]
    Retrieve --> EmbeddingModel["Embedding Model<br>nomic-embed-text"]
    EmbeddingModel --> VectorStore["Vector Store<br>banking-embeddings.json"]
    VectorStore --> Documents["Retrieved Documents<br>max-results: 3"]
    Documents --> Aggregate["3. Content Aggregation<br>DefaultContentAggregator"]
    Aggregate --> Inject["4. Content Injection<br>DefaultContentInjector"]
    Inject --> FinalPrompt["Final Prompt with<br>System Message +<br>Retrieved Context +<br>User Question"]
    FinalPrompt --> ChatModel["5. Generate Response<br>ChatModel.chat<br>llama3.2"]
    ChatModel --> Response["AI Response"]
    Response --> ChatbotResource

    style Producer fill:#e1f5ff
    style BuildAugmentor fill:#f3e5f5
    style TransformQuery fill:#fff4e1
    style Retrieve fill:#e8f5e9
    style ChatModel fill:#fce4ec
```

## Component Architecture

```mermaid
graph TB
    subgraph "CDI Container"
        Producer["RetrievalAugmentorProducer<br/>@ApplicationScoped"]
        Transformer["StandaloneQueryTransformer<br/>@ApplicationScoped<br/>Injects ChatModel"]
        EmbedStore["EmbeddingStore<br/>Auto-created by easy-rag"]
        EmbedModel["EmbeddingModel<br/>Ollama nomic-embed-text"]
    end
    
    subgraph "REST Layer"
        Resource["ChatbotResource<br/>@Path /chat"]
    end
    
    subgraph "AI Service"
        Service["BankingChatbot<br/>@RegisterAiService<br/>@ApplicationScoped<br/>@MemoryId for session management"]
    end
    
    subgraph "RAG Components"
        Augmentor["RetrievalAugmentor<br/>@Produces by RetrievalAugmentorProducer"]
        Retriever["EmbeddingStoreContentRetriever<br/>Created in Producer<br/>maxResults: 3"]
        Aggregator["ContentAggregator<br/>Default"]
        Injector["ContentInjector<br/>Default"]
    end
    
    subgraph "Query Metadata"
        QueryMeta["Query.metadata()<br/>Contains:<br/>- chatMemoryId<br/>- chatMemory (List<ChatMessage>)<br/>- chatMessage"]
    end
    
    subgraph "Models"
        ChatModel["ChatModel<br/>Ollama llama3.2"]
    end
    
    subgraph "Storage"
        VectorStore["Vector Store<br/>banking-embeddings.json"]
        Documents["Document Store<br/>src/main/resources/documents"]
    end
    
    Resource -->|"@Inject"| Service
    Service -->|Uses| Augmentor
    Service -->|Manages| QueryMeta
    Producer -->|"@Produces"| Augmentor
    Producer -->|"@Inject"| EmbedStore
    Producer -->|"@Inject"| EmbedModel
    Producer -->|"@Inject"| Transformer
    Producer -->|Creates| Retriever
    Augmentor -->|Uses| Transformer
    Augmentor -->|Uses| Retriever
    Transformer -->|Reads| QueryMeta
    Transformer -->|Uses| ChatModel
    Retriever -->|Uses| EmbedStore
    Retriever -->|Uses| EmbedModel
    EmbedModel -->|Queries| VectorStore
    VectorStore -->|Indexes| Documents
    Service -->|Uses| ChatModel
    
    style Producer fill:#e1f5ff
    style Transformer fill:#fff4e1
    style Augmentor fill:#f3e5f5
    style Service fill:#e8f5e9
```

## Key Implementation Details

### Query Transformation Flow

1. **Query Metadata Access**: The `StandaloneQueryTransformer` reads chat memory directly from `Query.metadata().chatMemory()`. This is populated by the LangChain4j framework when using `@RegisterAiService` with `@MemoryId`.

2. **Chat Memory Check**: 
   - If `chatMemory` is `null` or empty, the transformer returns the original query unchanged (first message in a conversation)
   - If chat memory exists, it formats the conversation history and uses an LLM to rewrite the query to be self-contained

3. **Logging**: The transformer logs:
   - Original query text (before transformation)
   - Transformed query text (after transformation)
   - Transformation duration in milliseconds
   - Query unchanged message when no chat memory is present

4. **Metadata Preservation**: The transformed query preserves the original query's metadata, ensuring chat memory ID and other context is maintained.

### Component Dependencies

- **RetrievalAugmentorProducer**: Creates the `RetrievalAugmentor` by combining:
  - `EmbeddingStoreContentRetriever` (built from `EmbeddingStore` and `EmbeddingModel`)
  - `StandaloneQueryTransformer` (injected as a CDI bean)

- **StandaloneQueryTransformer**: 
  - Injects `ChatModel` for query rewriting
  - Uses `PromptTemplate` to format the transformation prompt
  - Formats chat history as "User: ..." and "AI: ..." messages

### Chat Memory Behavior

- Chat memory is managed by the `@RegisterAiService` framework
- The `@MemoryId` parameter in `BankingChatbot.chat()` identifies the session
- Query metadata includes `chatMemoryId` and `chatMemory` (list of previous messages)
- On the first message, `chatMemory` will be empty, so no transformation occurs
- On subsequent messages, `chatMemory` contains previous exchanges, enabling query rewriting
