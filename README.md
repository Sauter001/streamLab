# Stream Lab

<img src="docs/images/stream-lab-title.png" width="500px" alt="stream lab title logo">

> stream이 어려워요!

**그럼 익숙하게 만들어드리겠습니다!**

Java를 배우는 많은 사람들이 Java의 lambda와 stream이 강력함을 알면서도, 익숙한 `for`문과 `while`만 쓰려합니다. 그럼 강제로 쓰게 하면 되지 않을까요...? 

이 프로젝트는 대학에서 하던 CS:APP의 data lab, bomb lab 과제에서 영향을 받았습니다!

Bomb Lab의 단계별 문제 해금과 비밀 단계, 그리고 특정 연산자만 써야 했던 Data Lab처럼, Stream Lab에서는 **오직 Stream API만 허용**합니다. 별 생각 없이 썼던 루프와 컬렉션 선언 없이 문제를 풀어야 합니다. 이런 극단적인 제약에서 진짜 문제 해결력을 테스트받아 보세요.

## 🔒 4가지 철칙
```java
// ❌ 1. for, while, do-while 금지
for (int i = 0; i < list.size(); i++) { }

// ❌ 2. 재귀 함수 호출 금지  
public int sum(int n) { return n + sum(n-1); }

// ❌ 3. 헬퍼 메소드 추가 선언 금지
private void helper() { }

// ✅ 4. return stream()...; 형태만 허용!
public static List solve(List students) {
    return students.stream()
                   .filter(s -> s.getScore() >= 80)
                   .map(Student::getName)
                   .toList();
}
```

### 그럼 어떻게 구현하라구요?

바로 그게 이 Lab의 핵심입니다. 평소에 당연하게 쓰던 것들이 전부 금지된 상태에서, **Stream API만으로** 모든 문제를 해결해야 합니다.

- 반복? → `forEach()`, `map()`, `flatMap()`
- 조건 분기? → `filter()`, `takeWhile()`, `dropWhile()`
- 집계? → `reduce()`, `collect()`
- 그룹화? → `Collectors.groupingBy()`, `partitioningBy()`

```java
public static List solve(List students) {
    // ❌ 금지:
    // - for, while, do-while 등 루프문
    // - 재귀 함수
    // - 추가 메소드 선언
    // - 컬렉션은 최대한 자제
    
    // ✅ Required:
    // - return문 하나만!
    // - stream으로 변환된 리스트
    
    return students.stream()
        .filter(s -> s.getScore() >= 80)
        .map(Student::getName)
        .toList();
}

// ❌ 하지마세요!
private void doSomething() {
    
}
```

## 🎓 학습 트리

### Tutorial: 기본기 다지기

시작부터 어렵진 않습니다. 

### Level 1-5: 점진적 난이도 상승
각 20점씩, 총 100점. `filter`, `map`, `flatMap`, `groupingBy`... 하나씩 정복해봅시다.

### 💀 ???

만점을 달성하면... 뭐가 나오지 않을까요?


## 🤝 Contributing

이 프로젝트는 재미 및 교육 목적으로 개발되었습니다.  
새로운 레벨 아이디어, 버그 리포트, 문서 개선 등 모든 기여를 환영합니다!