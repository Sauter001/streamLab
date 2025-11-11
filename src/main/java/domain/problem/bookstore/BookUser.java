package domain.problem.bookstore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
class BookUser {
    String username;
    int age;
    List<Book> readBooks;
}