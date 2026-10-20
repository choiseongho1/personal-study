package com.example.jpa;

import com.example.jpa.entity.item.Book;
import com.example.jpa.entity.item.Item;
import com.example.jpa.entity.item.Movie;
import com.example.jpa.repository.ItemRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback(false)
class InheritanceTest {

    @Autowired
    EntityManager em;
    @Autowired
    ItemRepository itemRepository;

    @Test
    void singleTable_전략_테스트() {
        Book book = new Book();
        book.setName("JPA 책");
        book.setPrice(20000);
        book.setAuthor("홍길동");
        book.setIsbn("123-456");

        em.persist(book);

        Movie movie = new Movie();
        movie.setName("인셉션");
        movie.setPrice(15000);
        movie.setDirector("놀란");
        movie.setActor("디카프리오");

        em.persist(movie);

        em.flush();
        em.clear();

        System.out.println("▶ 모든 Item 조회");
        List<Item> result = em.createQuery("select i from Item i", Item.class).getResultList();
        for (Item item : result) {
            System.out.println("📦 " + item.getClass().getSimpleName() + ": " + item.getName());
        }
    }
}
