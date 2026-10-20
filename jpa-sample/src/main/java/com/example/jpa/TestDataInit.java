package com.example.jpa;


import com.example.jpa.entity.Comment;
import com.example.jpa.entity.Order;
import com.example.jpa.entity.OrderItem;
import com.example.jpa.entity.Post;
import com.example.jpa.entity.Product;
import com.example.jpa.entity.User;
import com.example.jpa.repository.CommentRepository;
import com.example.jpa.repository.OrderRepository;
import com.example.jpa.repository.PostRepository;
import com.example.jpa.repository.ProductRepository;
import com.example.jpa.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDataInit implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    @Override
    public void run(String... args) {
        // 10개 상품 미리 생성
        List<Product> products = IntStream.range(1, 11)
            .mapToObj(i -> Product.builder()
                .name("상품" + i)
                .price(1000 * i)
                .stock(100)
                .build())
            .map(productRepository::save)
            .toList();

        // 10명 유저 + 각 유저마다 1개의 주문 생성
        for (int i = 1; i <= 10; i++) {
            User user = userRepository.save(
                User.builder()
                    .name("유저" + i)
                    .email("user" + i + "@test.com")
                    .build()
            );

            // 주문 아이템 2개 (임의 상품 2개 사용)
            Product product1 = products.get((i - 1) % products.size());
            Product product2 = products.get((i) % products.size());

            OrderItem item1 = OrderItem.builder()
                .product(product1)
                .quantity(1)
                .totalPrice(product1.getPrice())
                .build();

            OrderItem item2 = OrderItem.builder()
                .product(product2)
                .quantity(2)
                .totalPrice(product2.getPrice() * 2)
                .build();

            Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .orderItems(List.of(item1, item2))
                .build();

            item1.setOrder(order);
            item2.setOrder(order);

            orderRepository.save(order);
        }

        // === Post 데이터 추가 ===
        String[] categories = {"JAVA", "SPRING", "JPA", "QUERYDSL"};

        IntStream.range(1, 21).forEach(i -> {
            String title = "게시글 제목 " + i;
            String author = "작성자" + (i % 5 + 1);
            String category = categories[i % categories.length];
            LocalDateTime createdAt = LocalDateTime.now().minusDays(i);

            Post post = Post.builder()
                .title(title)
                .author(author)
                .category(category)
                .build();

            Post savedPost = postRepository.save(post);

            // ✅ 댓글 2~3개 추가
            IntStream.range(1, (i % 2) + 2).forEach(j -> {
                Comment comment = Comment.builder()
                    .writer("익명" + j)
                    .content("댓글 내용 " + j + " (for post " + i + ")")
                    .post(savedPost)
                    .build();

                commentRepository.save(comment);
            });
        });

    }
}
