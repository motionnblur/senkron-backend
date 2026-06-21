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
import org.springframework.dao.DataIntegrityViolationException;

import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelMemberEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelType;
import com.motionnblur.senkron_backend.support.RepositoryTestFixtures;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ChannelMemberRepositoryTest {

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity alice;
    private UserEntity bob;
    private ChannelEntity general;
    private ChannelEntity team;
    private ChannelMemberEntity aliceInGeneral;
    private ChannelMemberEntity aliceInTeam;
    private ChannelMemberEntity bobInGeneral;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(RepositoryTestFixtures.user("alice@example.com"));
        bob = userRepository.save(RepositoryTestFixtures.user("bob@example.com"));

        general = channelRepository.save(
                RepositoryTestFixtures.channel(alice, ChannelType.PUBLIC, "general"));
        team = channelRepository.save(
                RepositoryTestFixtures.channel(alice, ChannelType.PRIVATE, "team"));

        aliceInGeneral = channelMemberRepository.save(
                RepositoryTestFixtures.membership(alice, general, RepositoryTestFixtures.baseTime()));
        aliceInTeam = channelMemberRepository.save(
                RepositoryTestFixtures.membership(alice, team, RepositoryTestFixtures.timeAfterMinutes(5)));
        bobInGeneral = channelMemberRepository.save(
                RepositoryTestFixtures.membership(bob, general, RepositoryTestFixtures.timeAfterMinutes(10)));
    }

    @Nested
    @DisplayName("findByUser")
    class FindByUser {

        @Test
        void returnsMembershipsForUser() {
            List<ChannelMemberEntity> memberships = channelMemberRepository.findByUser(alice);

            assertThat(memberships)
                    .extracting(membership -> membership.getChannel().getName())
                    .containsExactlyInAnyOrder("general", "team");
        }

        @Test
        void returnsEmptyListWhenUserHasNoMemberships() {
            UserEntity carol = userRepository.save(RepositoryTestFixtures.user("carol@example.com"));

            assertThat(channelMemberRepository.findByUser(carol)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        void returnsMembershipsForUserId() {
            List<ChannelMemberEntity> memberships = channelMemberRepository.findByUserId(bob.getId());

            assertThat(memberships)
                    .extracting(ChannelMemberEntity::getId)
                    .containsExactly(bobInGeneral.getId());
        }
    }

    @Nested
    @DisplayName("findByChannel")
    class FindByChannel {

        @Test
        void returnsMembershipsForChannel() {
            List<ChannelMemberEntity> memberships = channelMemberRepository.findByChannel(general);

            assertThat(memberships)
                    .extracting(membership -> membership.getUser().getEmail())
                    .containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
        }
    }

    @Nested
    @DisplayName("findByChannelId")
    class FindByChannelId {

        @Test
        void returnsMembershipsForChannelId() {
            List<ChannelMemberEntity> memberships = channelMemberRepository.findByChannelId(team.getId());

            assertThat(memberships)
                    .extracting(ChannelMemberEntity::getId)
                    .containsExactly(aliceInTeam.getId());
        }
    }

    @Nested
    @DisplayName("findByUserAndChannel")
    class FindByUserAndChannel {

        @Test
        void returnsMembershipWhenPresent() {
            assertThat(channelMemberRepository.findByUserAndChannel(alice, general))
                    .isPresent()
                    .get()
                    .extracting(ChannelMemberEntity::getId)
                    .isEqualTo(aliceInGeneral.getId());
        }

        @Test
        void returnsEmptyWhenMembershipAbsent() {
            assertThat(channelMemberRepository.findByUserAndChannel(bob, team)).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndChannelId")
    class ExistsByUserIdAndChannelId {

        @Test
        void returnsTrueWhenMembershipExists() {
            assertThat(channelMemberRepository.existsByUserIdAndChannelId(alice.getId(), general.getId()))
                    .isTrue();
        }

        @Test
        void returnsFalseWhenMembershipDoesNotExist() {
            assertThat(channelMemberRepository.existsByUserIdAndChannelId(bob.getId(), team.getId()))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("constraints")
    class Constraints {

        @Test
        void enforcesUniqueUserChannelMembership() {
            ChannelMemberEntity duplicate = RepositoryTestFixtures.membership(alice, general);

            org.junit.jupiter.api.Assertions.assertThrows(
                    DataIntegrityViolationException.class,
                    () -> channelMemberRepository.saveAndFlush(duplicate));
        }
    }

}
