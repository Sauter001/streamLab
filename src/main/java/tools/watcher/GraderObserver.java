package tools.watcher;

import java.nio.file.Path;

public interface GraderObserver {
    /**
     * 파일이 변경되었을 때 호출됩니다.
     *
     * @param filePath 변경된 파일의 경로
     */
    void onFileChanged(Path filePath);

    /**
     * Observer의 리소스를 정리합니다.
     * 스케줄러 종료, 대기 중인 채점 취소 등을 수행합니다.
     */
    void shutdown();
}
