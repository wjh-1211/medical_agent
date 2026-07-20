package com.medicalagent.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicalagent.common.JsonSupport;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class KnowledgeDocumentLoader {

    public List<Document> load(Path documentsDirectory) {
        if (!Files.isDirectory(documentsDirectory)) {
            throw new IllegalArgumentException("Knowledge documents directory does not exist: " + documentsDirectory);
        }
        try (Stream<Path> paths = Files.list(documentsDirectory)) {
            List<Document> documents = new ArrayList<>();
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String fileName = path.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".json")) {
                    documents.addAll(loadJson(path));
                } else if (fileName.endsWith(".md")) {
                    documents.addAll(loadMarkdown(path));
                }
            }
            if (documents.isEmpty()) {
                throw new IllegalArgumentException("No supported knowledge documents found in: " + documentsDirectory);
            }
            return documents;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load knowledge documents", exception);
        }
    }

    private List<Document> loadJson(Path path) throws IOException {
        JsonNode root = JsonSupport.JSON_MAPPER.readTree(path.toFile());
        String source = requiredText(root, "source", path);
        String version = requiredText(root, "documentVersion", path);
        List<Document> documents = new ArrayList<>();
        for (JsonNode section : root.path("sections")) {
            String title = requiredText(section, "title", path);
            String content = requiredText(section, "content", path);
            documents.add(Document.from(content, metadata(source, title, version)));
        }
        return documents;
    }

    private List<Document> loadMarkdown(Path path) throws IOException {
        String source = path.getFileName().toString();
        String version = "unversioned";
        String section = source;
        StringBuilder content = new StringBuilder();
        List<Document> documents = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.startsWith("<!-- source:")) {
                source = commentValue(line);
            } else if (line.startsWith("<!-- version:")) {
                version = commentValue(line);
            } else if (line.startsWith("## ")) {
                addMarkdownSection(documents, content, source, section, version);
                section = line.substring(3).trim();
            } else if (!line.startsWith("# ")) {
                content.append(line).append(System.lineSeparator());
            }
        }
        addMarkdownSection(documents, content, source, section, version);
        return documents;
    }

    private void addMarkdownSection(List<Document> documents, StringBuilder content, String source, String section, String version) {
        String text = content.toString().trim();
        if (!text.isEmpty()) {
            documents.add(Document.from(text, metadata(source, section, version)));
        }
        content.setLength(0);
    }

    private Metadata metadata(String source, String section, String version) {
        return new Metadata()
                .put("source", source)
                .put("section", section)
                .put("documentVersion", version);
    }

    private String requiredText(JsonNode node, String field, Path path) {
        String value = node.path(field).asText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing '" + field + "' in knowledge document: " + path);
        }
        return value;
    }

    private String commentValue(String line) {
        return line.substring(line.indexOf(':') + 1).replace("-->", "").trim();
    }
}
