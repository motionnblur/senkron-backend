package com.motionnblur.senkron_backend.channel.repository;

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

import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelType;
import com.motionnblur.senkron_backend.support.RepositoryTestFixtures;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ChannelRepositoryTest {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity creator;
    private UserEntity otherUser;
    private ChannelEntity publicChannel;
    private ChannelEntity privateChannel;
    private ChannelEntity dmChannel;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(RepositoryTestFixtures.user("creator@example.com"));
        otherUser = userRepository.save(RepositoryTestFixtures.user("other@example.com"));

        publicChannel = channelRepository.save(
                RepositoryTestFixtures.channel(creator, ChannelType.PUBLIC, "general"));
        privateChannel = channelRepository.save(
                RepositoryTestFixtures.channel(creator, ChannelType.PRIVATE, "team"));
        dmChannel = channelRepository.save(
                RepositoryTestFixtures.channel(otherUser, ChannelType.DM, "dm"));
    }

    @Nested
    @DisplayName("findByType")
    class FindByType {

        @Test
        void returnsChannelsMatchingType() {
            List<ChannelEntity> publicChannels = channelRepository.findByType(ChannelType.PUBLIC);

            assertThat(publicChannels)
                    .extracting(ChannelEntity::getName)
                    .containsExactly("general");
        }

        @Test
        void returnsEmptyListWhenNoChannelsMatchType() {
            channelRepository.delete(publicChannel);
            channelRepository.delete(privateChannel);
            channelRepository.delete(dmChannel);

            assertThat(channelRepository.findByType(ChannelType.PUBLIC)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCreatedBy")
    class FindByCreatedBy {

        @Test
        void returnsChannelsCreatedByUser() {
            List<ChannelEntity> channels = channelRepository.findByCreatedBy(creator);

            assertThat(channels)
                    .extracting(ChannelEntity::getName)
                    .containsExactlyInAnyOrder("general", "team");
        }

        @Test
        void returnsEmptyListWhenUserCreatedNoChannels() {
            UserEntity loneUser = userRepository.save(RepositoryTestFixtures.user("lone@example.com"));

            assertThat(channelRepository.findByCreatedBy(loneUser)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCreatedById")
    class FindByCreatedById {

        @Test
        void returnsChannelsCreatedByUserId() {
            List<ChannelEntity> channels = channelRepository.findByCreatedById(creator.getId());

            assertThat(channels)
                    .extracting(ChannelEntity::getName)
                    .containsExactlyInAnyOrder("general", "team");
        }

        @Test
        void returnsSingleChannelForDifferentCreator() {
            List<ChannelEntity> channels = channelRepository.findByCreatedById(otherUser.getId());

            assertThat(channels)
                    .extracting(ChannelEntity::getName)
                    .containsExactly("dm");
        }

        @Test
        void returnsEmptyListForUnknownCreatorId() {
            assertThat(channelRepository.findByCreatedById(9_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaRepository operations")
    class JpaRepositoryOperations {

        @Test
        void persistsChannelWithCreatorRelationship() {
            ChannelEntity saved = channelRepository.findById(publicChannel.getId()).orElseThrow();

            assertThat(saved.getCreatedBy().getEmail()).isEqualTo("creator@example.com");
            assertThat(saved.getType()).isEqualTo(ChannelType.PUBLIC);
        }

        @Test
        void updatesChannelFields() {
            publicChannel.setDescription("Updated description");
            channelRepository.save(publicChannel);

            assertThat(channelRepository.findById(publicChannel.getId()))
                    .isPresent()
                    .get()
                    .extracting(ChannelEntity::getDescription)
                    .isEqualTo("Updated description");
        }
    }

}
