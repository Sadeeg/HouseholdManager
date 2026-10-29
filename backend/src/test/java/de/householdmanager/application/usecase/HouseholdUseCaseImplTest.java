package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.InvalidHouseholdNameException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test des Use Cases gegen den ausgehenden Port — ohne Spring, ohne
 * Datenbank. Der Port wird gemockt (in Produktion wäre ein In-Memory-Fake
 * die bessere Wahl).
 */
@ExtendWith(MockitoExtension.class)
class HouseholdUseCaseImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:15:30Z");

    @Mock
    private HouseholdRepository householdRepository;

    private HouseholdUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HouseholdUseCaseImpl(householdRepository, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void listHouseholds_returnsAllHouseholds() {
        Household household = Household.builder()
                .id(1L)
                .name("Haushalt Müller")
                .createdAt(FIXED_NOW)
                .build();
        when(householdRepository.findAll()).thenReturn(List.of(household));

        List<Household> result = useCase.listHouseholds();

        assertThat(result).containsExactly(household);
    }

    @Test
    void createHousehold_trimsNameAndSetsTimestamp() {
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
            Household saved = invocation.getArgument(0);
            return saved.withId(7L);
        });

        Household result = useCase.createHousehold("  Haushalt Müller  ");

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getName()).isEqualTo("Haushalt Müller");
        assertThat(result.getCreatedAt()).isEqualTo(FIXED_NOW);
        verify(householdRepository).save(any(Household.class));
    }

    @Test
    void createHousehold_rejectsBlankName() {
        assertThatThrownBy(() -> useCase.createHousehold("   "))
                .isInstanceOf(InvalidHouseholdNameException.class)
                .hasMessageContaining("darf nicht leer sein");
    }

    @Test
    void createHousehold_rejectsNameLongerThanColumnLimit() {
        String tooLong = "x".repeat(Household.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> useCase.createHousehold(tooLong))
                .isInstanceOf(InvalidHouseholdNameException.class)
                .hasMessageContaining("höchstens " + Household.MAX_NAME_LENGTH + " Zeichen");
    }

    @Test
    void createHousehold_measuresLengthAfterTrimming() {
        // Maximale Länge plus Leerzeichen: getrimmt gültig, ungetrimmt länger.
        String padded = "  " + "x".repeat(Household.MAX_NAME_LENGTH) + "  ";
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
            Household saved = invocation.getArgument(0);
            return saved.withId(1L);
        });

        Household result = useCase.createHousehold(padded);

        assertThat(result.getName()).hasSize(Household.MAX_NAME_LENGTH);
    }
}
