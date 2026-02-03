package domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import constants.LevelConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    private String userName;
    private boolean tutorialSkipped;
    private Map<String, Boolean> levelDialogueShown;
    private Set<Integer> passedLevels;
    private int currentLevel;
    private boolean secretUnlocked;

    public static Profile createNew(String userName) {
        return Profile.builder()
                .userName(userName)
                .tutorialSkipped(false)
                .levelDialogueShown(new HashMap<>())
                .passedLevels(new HashSet<>())
                .currentLevel(LevelConstants.INITIAL_LEVEL)
                .secretUnlocked(false)
                .build();
    }

    public void skipTutorial() {
        this.tutorialSkipped = true;
    }

    public void markLevelDialogueShown(String levelId) {
        this.levelDialogueShown.put(levelId, true);
    }

    public boolean isLevelDialogueShown(String levelId) {
        return levelDialogueShown.getOrDefault(levelId, false);
    }

    public void passLevel(int level) {
        this.passedLevels.add(level);
        if (level >= currentLevel) {
            this.currentLevel = level + 1;
        }
    }

    public boolean hasPassedLevel(int level) {
        return passedLevels.contains(level);
    }

    public void unlockSecret() {
        this.secretUnlocked = true;
    }
}
