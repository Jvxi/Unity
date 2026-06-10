package com.jvxi.unity.novel.service.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.rag.RagResult;
import com.jvxi.unity.novel.persistence.entity.Bm25TermEntity;
import com.jvxi.unity.novel.persistence.entity.RagQueryLogEntity;
import com.jvxi.unity.novel.persistence.entity.VectorEmbeddingEntity;
import com.jvxi.unity.novel.persistence.repository.Bm25TermRepository;
import com.jvxi.unity.novel.persistence.repository.RagQueryLogRepository;
import com.jvxi.unity.novel.persistence.repository.VectorEmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final int RRF_K = 60;
    private static final double BM25_K1 = 1.5;
    private static final double BM25_B = 0.75;

    private final VectorEmbeddingRepository vectorEmbeddingRepository;
    private final Bm25TermRepository bm25TermRepository;
    private final RagQueryLogRepository ragQueryLogRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public RagService(
            VectorEmbeddingRepository vectorEmbeddingRepository,
            Bm25TermRepository bm25TermRepository,
            RagQueryLogRepository ragQueryLogRepository,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.vectorEmbeddingRepository = vectorEmbeddingRepository;
        this.bm25TermRepository = bm25TermRepository;
        this.ragQueryLogRepository = ragQueryLogRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    /**
     * 混合检索（默认）
     */
    public List<RagResult> hybridSearch(String bookId, String query, int topK) {
        long startTime = System.currentTimeMillis();

        // 向量检索
        List<RagResult> vectorResults = vectorSearch(bookId, query, topK);

        // BM25检索
        List<RagResult> bm25Results = bm25Search(bookId, query, topK);

        // RRF融合
        List<RagResult> fusedResults = rrfFusion(vectorResults, bm25Results);

        // 截断到topK
        if (fusedResults.size() > topK) {
            fusedResults = fusedResults.subList(0, topK);
        }

        // 记录查询日志
        long latency = System.currentTimeMillis() - startTime;
        logQuery(bookId, null, query, RagQueryLogEntity.QueryType.hybrid, fusedResults, latency);

        return fusedResults;
    }

    /**
     * 向量检索
     */
    public List<RagResult> vectorSearch(String bookId, String query, int topK) {
        long startTime = System.currentTimeMillis();

        // 获取查询向量
        List<Float> queryEmbedding = embeddingService.embed(query);
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            log.warn("向量检索失败：无法获取查询向量");
            return List.of();
        }

        // 获取所有向量
        List<VectorEmbeddingEntity> allVectors = vectorEmbeddingRepository.findByBookId(bookId);

        // 计算相似度并排序
        List<RagResult> results = allVectors.stream()
                .map(entity -> {
                    List<Float> docEmbedding = bytesToFloats(entity.getEmbedding());
                    double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                    return new RagResult(
                            entity.getChunkId(),
                            entity.getChunkText(),
                            similarity,
                            0.0,
                            entity.getChapterNumber(),
                            "vector"
                    );
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());

        long latency = System.currentTimeMillis() - startTime;
        logQuery(bookId, null, query, RagQueryLogEntity.QueryType.vector, results, latency);

        return results;
    }

    /**
     * BM25检索
     */
    public List<RagResult> bm25Search(String bookId, String query, int topK) {
        long startTime = System.currentTimeMillis();

        // 分词
        List<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        // 获取所有chunk的文档频率
        long totalChunks = bm25TermRepository.countDistinctChunksByBookId(bookId);
        if (totalChunks == 0) {
            return List.of();
        }

        // 计算每个chunk的BM25分数
        Map<String, Double> chunkScores = new HashMap<>();
        Map<String, String> chunkTexts = new HashMap<>();

        for (String term : terms) {
            List<Bm25TermEntity> termEntities = bm25TermRepository.findByBookIdAndTerm(bookId, term);
            int df = termEntities.size();

            for (Bm25TermEntity entity : termEntities) {
                String chunkId = entity.getChunkId();
                int tf = entity.getTermFrequency();

                // BM25评分公式
                double idf = Math.log((totalChunks - df + 0.5) / (df + 0.5) + 1.0);
                double tfNorm = (tf * (BM25_K1 + 1)) / (tf + BM25_K1);
                double score = idf * tfNorm;

                chunkScores.merge(chunkId, score, Double::sum);
            }
        }

        // 获取chunk文本并排序
        List<RagResult> results = chunkScores.entrySet().stream()
                .map(entry -> {
                    String chunkId = entry.getKey();
                    // 获取chunk文本
                    VectorEmbeddingEntity vectorEntity = vectorEmbeddingRepository.findByBookIdAndChunkId(bookId, chunkId).orElse(null);
                    String chunkText = vectorEntity != null ? vectorEntity.getChunkText() : "";
                    Integer chapterNumber = vectorEntity != null ? vectorEntity.getChapterNumber() : null;

                    return new RagResult(chunkId, chunkText, entry.getValue(), 0.0, chapterNumber, "bm25");
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());

        long latency = System.currentTimeMillis() - startTime;
        logQuery(bookId, null, query, RagQueryLogEntity.QueryType.bm25, results, latency);

        return results;
    }

    /**
     * RRF融合算法
     */
    private List<RagResult> rrfFusion(List<RagResult> vectorResults, List<RagResult> bm25Results) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, RagResult> resultMap = new HashMap<>();

        // 向量结果计分
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            RagResult r = vectorResults.get(rank);
            scoreMap.merge(r.chunkId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            resultMap.putIfAbsent(r.chunkId(), r);
        }

        // BM25结果计分
        for (int rank = 0; rank < bm25Results.size(); rank++) {
            RagResult r = bm25Results.get(rank);
            scoreMap.merge(r.chunkId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            resultMap.putIfAbsent(r.chunkId(), r);
        }

        // 按分数排序
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    RagResult r = resultMap.get(entry.getKey());
                    return new RagResult(
                            r.chunkId(),
                            r.chunkText(),
                            r.score(),
                            entry.getValue(),
                            r.chapterNumber(),
                            r.source()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 添加向量
     */
    @Transactional
    public void addEmbedding(String bookId, String chunkId, String text, List<Float> embedding) {
        // 删除已有的
        vectorEmbeddingRepository.findByBookIdAndChunkId(bookId, chunkId)
                .ifPresent(entity -> vectorEmbeddingRepository.delete(entity));

        // 创建新的
        VectorEmbeddingEntity entity = new VectorEmbeddingEntity();
        entity.setBookId(bookId);
        entity.setChunkId(chunkId);
        entity.setChunkText(text);
        entity.setEmbedding(floatsToBytes(embedding));

        vectorEmbeddingRepository.save(entity);
    }

    /**
     * 索引章节
     */
    @Transactional
    public void indexChapter(String bookId, int chapterNumber, String chapterText) {
        // 1. 分块
        List<String> chunks = splitIntoChunks(chapterText, 500);

        // 2. 为每个chunk生成向量并存储
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = String.format("ch%04d_p%d", chapterNumber, i);

            // 生成向量
            List<Float> embedding = embeddingService.embed(chunk);
            if (embedding != null && !embedding.isEmpty()) {
                addEmbedding(bookId, chunkId, chunk, embedding);
            }

            // BM25索引
            indexBm25Terms(bookId, chunkId, chunk);
        }

        log.info("章节索引完成: bookId={}, chapter={}, chunks={}", bookId, chapterNumber, chunks.size());
    }

    /**
     * RAG统计
     */
    public Map<String, Object> getStats(String bookId) {
        long vectorCount = vectorEmbeddingRepository.countByBookId(bookId);
        long chunkCount = bm25TermRepository.countDistinctChunksByBookId(bookId);
        long queryCount = ragQueryLogRepository.findByBookId(bookId).size();

        return Map.of(
                "vector_count", vectorCount,
                "chunk_count", chunkCount,
                "query_count", queryCount
        );
    }

    // ============ 私有辅助方法 ============

    private void indexBm25Terms(String bookId, String chunkId, String text) {
        // 删除已有的
        bm25TermRepository.findByBookIdAndChunkId(bookId, chunkId)
                .forEach(entity -> bm25TermRepository.delete(entity));

        // 分词并统计词频
        List<String> terms = tokenize(text);
        Map<String, Long> termFreq = terms.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        // 存储
        for (Map.Entry<String, Long> entry : termFreq.entrySet()) {
            Bm25TermEntity entity = new Bm25TermEntity();
            entity.setBookId(bookId);
            entity.setChunkId(chunkId);
            entity.setTerm(entry.getKey());
            entity.setTermFrequency(entry.getValue().intValue());
            bm25TermRepository.save(entity);
        }
    }

    private List<String> tokenize(String text) {
        // 简单的中文分词：按字符和英文单词分割
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else {
                if (current.length() > 0) {
                    tokens.add(current.toString().toLowerCase());
                    current = new StringBuilder();
                }
                // 中文字符单独作为一个token
                if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                    tokens.add(Character.toString(c));
                }
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString().toLowerCase());
        }

        // 过滤停用词和短token
        return tokens.stream()
                .filter(t -> t.length() >= 2 || Character.toString(t.charAt(0)).matches("[\\u4e00-\\u9fa5]"))
                .collect(Collectors.toList());
    }

    private List<String> splitIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private byte[] floatsToBytes(List<Float> floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.size() * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (Float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    private List<Float> bytesToFloats(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        List<Float> floats = new ArrayList<>();
        while (buffer.hasRemaining()) {
            floats.add(buffer.getFloat());
        }
        return floats;
    }

    private void logQuery(String bookId, Integer chapterNumber, String queryText,
                          RagQueryLogEntity.QueryType queryType, List<RagResult> results, long latencyMs) {
        try {
            RagQueryLogEntity logEntity = new RagQueryLogEntity();
            logEntity.setBookId(bookId);
            logEntity.setChapterNumber(chapterNumber);
            logEntity.setQueryText(queryText);
            logEntity.setQueryType(queryType);
            logEntity.setResultsJson(objectMapper.writeValueAsString(results));
            logEntity.setLatencyMs((int) latencyMs);
            ragQueryLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("记录RAG查询日志失败", e);
        }
    }
}

