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
    TransformQuery --> CheckMemory{"Has Chat<br>Memory?"}
    CheckMemory -- No --> ReturnOriginal["Return original query"]
    CheckMemory -- Yes --> FormatMemory["Format chat history"]
    FormatMemory --> CreatePrompt["Create prompt with<br>conversation context"]
    CreatePrompt --> LLMTransform["ChatModel.chat<br>Rewrite query to be<br>self-contained"]
    LLMTransform --> TransformedQuery["Transformed Query"]
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
        Producer[RetrievalAugmentorProducer<br/>@ApplicationScoped]
        Transformer[StandaloneQueryTransformer<br/>@ApplicationScoped]
        EmbedStore[EmbeddingStore<br/>Auto-created by easy-rag]
        EmbedModel[EmbeddingModel<br/>Ollama nomic-embed-text]
    end
    
    subgraph "REST Layer"
        Resource[ChatbotResource<br/>@Path /chat]
    end
    
    subgraph "AI Service"
        Service[BankingChatbot<br/>@RegisterAiService]
    end
    
    subgraph "RAG Components"
        Augmentor[RetrievalAugmentor<br/>@Produces]
        Retriever[ContentRetriever<br/>Created in Producer]
        Aggregator[ContentAggregator<br/>Default]
        Injector[ContentInjector<br/>Default]
    end
    
    subgraph "Models"
        ChatModel[ChatModel<br/>Ollama llama3.2]
    end
    
    subgraph "Storage"
        VectorStore[Vector Store<br/>banking-embeddings.json]
        Documents[Document Store<br/>src/main/resources/documents]
    end
    
    Resource -->|@Inject| Service
    Service -->|Uses| Augmentor
    Producer -->|@Produces| Augmentor
    Producer -->|@Inject| EmbedStore
    Producer -->|@Inject| EmbedModel
    Producer -->|@Inject| Transformer
    Producer -->|Creates| Retriever
    Augmentor -->|Uses| Transformer
    Augmentor -->|Uses| Retriever
    Retriever -->|Uses| EmbedStore
    Retriever -->|Uses| EmbedModel
    EmbedModel -->|Queries| VectorStore
    VectorStore -->|Indexes| Documents
    Transformer -->|Uses| ChatModel
    Service -->|Uses| ChatModel
    
    style Producer fill:#e1f5ff
    style Transformer fill:#fff4e1
    style Augmentor fill:#f3e5f5
    style Service fill:#e8f5e9
```
