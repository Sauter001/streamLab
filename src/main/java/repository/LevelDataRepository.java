package repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import constants.LevelConstants;
import ui.view.level.LevelView.ProblemSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LevelDataRepository {
    private static final String DATA_PATH = "data/test-data/level%d/level%d.json";
    private static final String SECRET_DATA_PATH = "data/test-data/secret/secret.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ProblemSummary> loadProblemSummaries(int level) {
        Path path = level == LevelConstants.SECRET_LEVEL
                ? Paths.get(SECRET_DATA_PATH)
                : Paths.get(String.format(DATA_PATH, level, level));

        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            JsonNode problems = root.get("problems");

            List<ProblemSummary> summaries = new ArrayList<>();
            for (JsonNode problem : problems) {
                summaries.add(new ProblemSummary(
                        problem.get("id").asText(),
                        problem.get("name").asText(),
                        problem.get("description").asText(),
                        false // TODO: 프로필에서 solved 여부 체크
                ));
            }
            return summaries;
        } catch (IOException e) {
            return List.of();
        }
    }
}
