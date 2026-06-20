package com.motionnblur.senkron_backend.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.motionnblur.senkron_backend.channel.ChannelEntity;
import com.motionnblur.senkron_backend.channel.ChannelRepository;
import com.motionnblur.senkron_backend.channel.ChannelType;
import com.motionnblur.senkron_backend.support.RepositoryTestFixtures;
import com.motionnblur.senkron_backend.user.UserEntity;
import com.motionnblur.senkron_backend.user.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity alice;
    private UserEntity bob;
    private ChannelEntity general;
    private ChannelEntity team;
    private MessageEntity oldestInGeneral;
    private MessageEntity middleInGeneral;
    private MessageEntity newestInGeneral;
    private MessageEntity teamMessage;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(RepositoryTestFixtures.user("alice@example.com"));
        bob = userRepository.save(RepositoryTestFixtures.user("bob@example.com"));

        general = channelRepository.save(
                RepositoryTestFixtures.channel(alice, ChannelType.PUBLIC, "general"));
        team = channelRepository.save(
                RepositoryTestFixtures.channel(alice, ChannelType.PRIVATE, "team"));

        oldestInGeneral = messageRepository.save(
                RepositoryTestFixtures.message(alice, general, "first", RepositoryTestFixtures.baseTime()));
        middleInGeneral = messageRepository.save(
                RepositoryTestFixtures.message(bob, general, "second", RepositoryTestFixtures.timeAfterMinutes(5)));
        newestInGeneral = messageRepository.save(
                RepositoryTestFixtures.message(alice, general, "third", RepositoryTestFixtures.timeAfterMinutes(10)));
        teamMessage = messageRepository.save(
                RepositoryTestFixtures.message(alice, team, "team-only", RepositoryTestFixtures.timeAfterMinutes(15)));
    }

    @Nested
    @DisplayName("findByChannelOrderByCreatedAtDesc")
    class FindByChannelOrderByCreatedAtDesc {

        @Test
        void returnsMessagesForChannelOrderedNewestFirst() {
            List<MessageEntity> messages = messageRepository.findByChannelOrderByCreatedAtDesc(general);

            assertThat(messages)
                    .extracting(MessageEntity::getContent)
                    .containsExactly("third", "second", "first");
        }

        @Test
        void excludesMessagesFromOtherChannels() {
            List<MessageEntity> messages = messageRepository.findByChannelOrderByCreatedAtDesc(team);

            assertThat(messages)
                    .extracting(MessageEntity::getContent)
                    .containsExactly("team-only");
        }

        @Test
        void returnsEmptyListWhenChannelHasNoMessages() {
            ChannelEntity empty = channelRepository.save(
                    RepositoryTestFixtures.channel(alice, ChannelType.PUBLIC, "empty"));

            assertThat(messageRepository.findByChannelOrderByCreatedAtDesc(empty)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByChannelIdOrderByCreatedAtDesc")
    class FindByChannelIdOrderByCreatedAtDesc {

        @Test
        void returnsPagedMessagesOrderedNewestFirst() {
            Page<MessageEntity> page = messageRepository.findByChannelIdOrderByCreatedAtDesc(
                    general.getId(),
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

            assertThat(page.getContent())
                    .extracting(MessageEntity::getContent)
                    .containsExactly("third", "second");
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        void returnsSecondPage() {
            Page<MessageEntity> page = messageRepository.findByChannelIdOrderByCreatedAtDesc(
                    general.getId(),
                    PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

            assertThat(page.getContent())
                    .extracting(MessageEntity::getContent)
                    .containsExactly("first");
            assertThat(page.isLast()).isTrue();
        }

        @Test
        void returnsEmptyPageForUnknownChannelId() {
            Page<MessageEntity> page = messageRepository.findByChannelIdOrderByCreatedAtDesc(
                    9_999L,
                    PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findByUser")
    class FindByUser {

        @Test
        void returnsAllMessagesAuthoredByUserAcrossChannels() {
            List<MessageEntity> messages = messageRepository.findByUser(alice);

            assertThat(messages)
                    .extracting(MessageEntity::getContent)
                    .containsExactlyInAnyOrder("first", "third", "team-only");
        }

        @Test
        void returnsOnlyMessagesForRequestedUser() {
            List<MessageEntity> messages = messageRepository.findByUser(bob);

            assertThat(messages)
                    .extracting(MessageEntity::getContent)
                    .containsExactly("second");
        }

        @Test
        void returnsEmptyListWhenUserHasNoMessages() {
            UserEntity carol = userRepository.save(RepositoryTestFixtures.user("carol@example.com"));

            assertThat(messageRepository.findByUser(carol)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaRepository operations")
    class JpaRepositoryOperations {

        @Test
        void persistsMessageWithUserAndChannelRelationships() {
            MessageEntity saved = messageRepository.findById(newestInGeneral.getId()).orElseThrow();

            assertThat(saved.getUser().getEmail()).isEqualTo("alice@example.com");
            assertThat(saved.getChannel().getName()).isEqualTo("general");
        }

        @Test
        void allowsMessageWithoutChannel() {
            MessageEntity directMessage = messageRepository.save(
                    RepositoryTestFixtures.message(
                            bob,
                            null,
                            "direct",
                            RepositoryTestFixtures.timeAfterMinutes(20)));

            assertThat(messageRepository.findById(directMessage.getId()))
                    .isPresent()
                    .get()
                    .satisfies(message -> {
                        assertThat(message.getChannel()).isNull();
                        assertThat(message.getContent()).isEqualTo("direct");
                    });
        }
    }

}
