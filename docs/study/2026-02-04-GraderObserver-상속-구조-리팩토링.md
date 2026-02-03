# GraderObserver 상속 구조 리팩토링

`LevelGraderObserver`와 `TutorialGraderObserver` 사이의 코드 중복을 `AbstractGraderObserver` 추상 클래스로 추출한 과정을 정리한다.

---

## 문제 상황

두 Observer 구현체가 거의 동일한 코드를 각각 가지고 있었다.

| 중복 코드 | 내용 |
|-----------|------|
| `compileSource()` | Gradle 컴파일 실행 (32줄, 완전 동일) |
| `shutdown()` | 스케줄러 종료 처리 (14줄, 완전 동일) |
| `onFileChanged()` | 파일명 체크 → 디바운스 → 채점 스케줄 (17줄, 구조 동일) |
| 스케줄러 초기화 | `newSingleThreadScheduledExecutor` + 데몬 스레드 (5줄) |
| `isGrading` guard | `compareAndSet(false, true)` + finally `set(false)` (3줄) |

또한 `GameContext.clearCurrentObserver()`에서 Observer 종료 시 instanceof 체크가 필요했다:

```java
// 변경 전: 구현체가 추가될 때마다 분기 추가 필요
if (currentObserver instanceof TutorialGraderObserver tutorialObserver) {
    tutorialObserver.shutdown();
} else if (currentObserver instanceof LevelGraderObserver levelObserver) {
    levelObserver.shutdown();
}
```

---

## 해결: Template Method 패턴 적용

### 클래스 구조

```
GraderObserver (interface)
├── onFileChanged(Path)
└── shutdown()
        ↑ implements
AbstractGraderObserver (abstract)
├── onFileChanged(Path)      ← final, 템플릿 메서드
├── compileSource()           ← 공통 구현
├── loadClass(String)         ← 공통 구현
├── shutdown()                ← 공통 구현
├── onFileChangeDetected()    ← abstract, 서브클래스 위임
└── executeGrading(Path)      ← abstract, 서브클래스 위임
        ↑ extends                    ↑ extends
LevelGraderObserver          TutorialGraderObserver
├── onFileChangeDetected()   ├── onFileChangeDetected()
├── executeGrading()         ├── executeGrading()
├── checkSecretPhaseUnlock() ├── gradeMethod()
└── (Level 고유 로직)         └── runTestCases()
```

### GraderObserver 인터페이스

`shutdown()`을 인터페이스에 추가하여 `GameContext`가 구현체 타입을 알 필요 없이 종료할 수 있게 했다.

```java
public interface GraderObserver {
    void onFileChanged(Path filePath);
    void shutdown();
}
```

### AbstractGraderObserver

공통 인프라와 템플릿 메서드를 담당한다.

```java
public abstract class AbstractGraderObserver implements GraderObserver {

    // --- 공통 인프라 ---
    // expectedFileName: 감시 대상 파일명 (생성자에서 주입)
    // scheduler: 디바운싱용 ScheduledExecutorService
    // isGrading: 중복 채점 방지 AtomicBoolean

    // --- 템플릿 메서드 ---
    @Override
    public final void onFileChanged(Path filePath) {
        // 1. 파일명 확인
        // 2. 이전 대기 채점 취소
        // 3. onFileChangeDetected() 호출 ← 서브클래스 위임
        // 4. 디바운스 후 safeExecuteGrading() 스케줄
    }

    private void safeExecuteGrading(Path filePath) {
        // isGrading guard를 여기서 처리
        // → 서브클래스의 executeGrading()은 guard 없이 순수 채점 로직만 작성
    }

    // --- 서브클래스 확장 포인트 ---
    protected abstract void onFileChangeDetected();
    protected abstract void executeGrading(Path filePath);

    // --- 공통 유틸리티 ---
    protected boolean compileSource() { ... }
    protected Class<?> loadClass(String className) { ... }

    @Override
    public void shutdown() { ... }
}
```

### 핵심 설계 결정

**1. `onFileChanged`를 `final`로 선언한 이유**

디바운싱, 파일명 필터링, isGrading guard는 모든 Observer가 동일하게 동작해야 하는 불변 흐름이다. 서브클래스가 이 흐름을 변경하면 채점 안정성이 깨질 수 있으므로 `final`로 잠갔다.

**2. `safeExecuteGrading` 래퍼를 도입한 이유**

변경 전에는 `isGrading` guard를 각 서브클래스가 직접 관리했다:
- `LevelGraderObserver`: try-finally로 처리
- `TutorialGraderObserver`: 메서드 끝에서 수동 해제 (예외 시 누락 위험)

래퍼로 통합하여 서브클래스는 `executeGrading()` 구현에만 집중하면 된다.

**3. `onFileChangeDetected()`를 abstract으로 분리한 이유**

두 Observer가 사용하는 View 타입이 다르다:
- `LevelGraderObserver` → `LevelGradingView`
- `TutorialGraderObserver` → `GradingView`

공통 View 인터페이스를 만드는 대신, 추상 메서드로 위임하여 기존 View 계층을 변경하지 않았다.

**4. `loadClass(String)`를 공통으로 추출한 이유**

두 Observer 모두 `ByteArrayClassLoader`로 클래스를 로딩하는 동일한 패턴을 사용한다:

```
클래스명 → 파일 경로 변환 → build/classes/java/main에서 바이트 읽기 → ByteArrayClassLoader로 로딩
```

---

## GameContext 변경

instanceof 체크가 제거되어 새로운 Observer 구현체를 추가해도 `clearCurrentObserver()`를 수정할 필요가 없다.

```java
// 변경 후
private void clearCurrentObserver() {
    if (currentObserver != null) {
        fileWatcher.removeObserver(currentObserver);
        currentObserver.shutdown();  // 인터페이스 메서드 호출
        currentObserver = null;
    }
}
```

---

## 변경 결과

| 파일 | 변경 전 | 변경 후 | 비고 |
|------|--------|--------|------|
| `AbstractGraderObserver.java` | - | 113줄 | 신규 생성 |
| `LevelGraderObserver.java` | 251줄 | 139줄 | -112줄 |
| `TutorialGraderObserver.java` | 244줄 | 136줄 | -108줄 |
| `GraderObserver.java` | 12줄 | 18줄 | shutdown() 추가 |
| `GameContext.java` | 98줄 | 92줄 | instanceof 제거 |
| **총합** | 605줄 | 498줄 | **-107줄** |

새로운 Observer 구현체를 추가할 때는 `AbstractGraderObserver`를 상속하고 `onFileChangeDetected()`와 `executeGrading()`만 구현하면 된다.
