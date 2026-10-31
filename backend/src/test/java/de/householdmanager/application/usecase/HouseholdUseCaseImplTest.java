package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.HouseholdMemberRepository;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdMember;
import de.householdmanager.domain.model.InvalidHouseholdNameException;
import de.householdmanager.domain.model.InvalidWasteCalendarUrlException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.WasteCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private static final String ALICE = "alice";

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private HouseholdMemberRepository householdMemberRepository;

    @Mock
    private MembershipUseCase membership;

    private HouseholdUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HouseholdUseCaseImpl(
                householdRepository, householdMemberRepository, membership,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void listHouseholds_returnsOnlyHouseholdsWhereThePersonIsMember() {
        Household household = household(1L, "Haushalt Müller");
        when(householdMemberRepository.findByUser(ALICE))
                .thenReturn(List.of(new HouseholdMember(1L, 1L, ALICE, FIXED_NOW)));
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));

        List<Household> result = useCase.listHouseholds(ALICE);

        assertThat(result).containsExactly(household);
    }

    @Test
    void listHouseholds_includesHouseholdsThePersonCreated() {
        Household owned = household(2L, "Mein Haushalt");
        when(householdMemberRepository.findByUser(ALICE)).thenReturn(List.of());
        when(householdRepository.findByCreatedBy(ALICE)).thenReturn(List.of(owned));

        List<Household> result = useCase.listHouseholds(ALICE);

        assertThat(result).containsExactly(owned);
    }

    @Test
    void createHousehold_trimsNameSetsTimestampAndCreator() {
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
            Household saved = invocation.getArgument(0);
            return saved.withId(7L);
        });

        Household result = useCase.createHousehold(ALICE, "  Haushalt Müller  ");

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getName()).isEqualTo("Haushalt Müller");
        assertThat(result.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(result.getCreatedBy()).isEqualTo(ALICE);
        verify(householdRepository).save(any(Household.class));
    }

    @Test
    void createHousehold_makesTheCreatorMemberOfTheHousehold() {
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation ->
                ((Household) invocation.getArgument(0)).withId(7L));

        useCase.createHousehold(ALICE, "Haushalt Müller");

        verify(householdMemberRepository).save(new HouseholdMember(null, 7L, ALICE, FIXED_NOW));
    }

    @Test
    void createHousehold_requiresACreator() {
        assertThatThrownBy(() -> useCase.createHousehold("  ", "Haushalt Müller"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createHousehold_rejectsBlankName() {
        assertThatThrownBy(() -> useCase.createHousehold(ALICE, "   "))
                .isInstanceOf(InvalidHouseholdNameException.class)
                .hasMessageContaining("darf nicht leer sein");
    }

    @Test
    void createHousehold_rejectsNameLongerThanColumnLimit() {
        String tooLong = "x".repeat(Household.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> useCase.createHousehold(ALICE, tooLong))
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

        Household result = useCase.createHousehold(ALICE, padded);

        assertThat(result.getName()).hasSize(Household.MAX_NAME_LENGTH);
    }

    // --- Müllkalender: Quelle setzen und entfernen (AC1, AC9, AC22) ---------

    /** AC1, R2: jedes Mitglied darf die Quelle pflegen — nicht nur die Leitung. */
    @Test
    void setWasteSource_forMemberStoresTheUrl() {
        Household household = household(1L, "Haushalt Müller");
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Household result = useCase.setWasteSource(ALICE, 1L, "  https://entsorger.example/ics  ");

        assertThat(result.getWasteCalendarUrl()).isEqualTo("https://entsorger.example/ics");
    }

    @Test
    void setWasteSource_forNonMemberThrowsMembershipRequired() {
        doThrow(new MembershipRequiredException("Die Person ist nicht Mitglied dieses Haushalts."))
                .when(membership).requireMembership(1L, ALICE);

        assertThatThrownBy(() -> useCase.setWasteSource(ALICE, 1L, "https://entsorger.example/ics"))
                .isInstanceOf(MembershipRequiredException.class);
    }

    /** AC22, R18: die Länge wird geprüft und die Grenze benannt. */
    @Test
    void setWasteSource_rejectsUrlLongerThanColumnLimit() {
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household(1L, "Haushalt Müller")));
        String tooLong = "https://entsorger.example/ics" + "x".repeat(WasteCalendar.MAX_URL_LENGTH);

        assertThatThrownBy(() -> useCase.setWasteSource(ALICE, 1L, tooLong))
                .isInstanceOf(InvalidWasteCalendarUrlException.class)
                .hasMessageContaining("höchstens " + WasteCalendar.MAX_URL_LENGTH + " Zeichen");
    }

    @Test
    void setWasteSource_requiresHttpOrHttps() {
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household(1L, "Haushalt Müller")));

        assertThatThrownBy(() -> useCase.setWasteSource(ALICE, 1L, "ftp://entsorger.example/ics"))
                .isInstanceOf(InvalidWasteCalendarUrlException.class)
                .hasMessageContaining("http");
    }

    /**
     * webcal ist die Konvention für einen ICS-Link — dieselbe Adresse, nur als Abonnement
     * bezeichnet. Der Abruf kann sie nicht lesen, deshalb wird sie auf https gesetzt und
     * in dieser Form gespeichert.
     */
    @Test
    void setWasteSource_normalizesWebcalToTheRetrievableUrl() {
        Household household = household(1L, "Haushalt Müller");
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Household result = useCase.setWasteSource(ALICE, 1L,
                "webcal://www.abfalltermine-forchheim.de/Forchheim/Landkreis/Forchheim%20-%20Kersbach/ics"
                        + "?RESTMUELL=true&BIO=true&YELLOW_SACK=true&PAPER=true");

        assertThat(result.getWasteCalendarUrl()).isEqualTo(
                "https://www.abfalltermine-forchheim.de/Forchheim/Landkreis/Forchheim%20-%20Kersbach/ics"
                        + "?RESTMUELL=true&BIO=true&YELLOW_SACK=true&PAPER=true");
    }

    /** Annahme 5: das Entfernen der Quelle lässt die gespeicherten Termine unberührt — hier wird nur das Feld geleert. */
    @Test
    void removeWasteSource_clearsTheFieldOnly() {
        Household household = household(1L, "Haushalt Müller").withWasteCalendarUrl("https://entsorger.example/ics");
        when(householdRepository.findById(1L)).thenReturn(Optional.of(household));
        ArgumentCaptor<Household> saved = ArgumentCaptor.forClass(Household.class);

        useCase.removeWasteSource(ALICE, 1L);

        verify(householdRepository).save(saved.capture());
        assertThat(saved.getValue().getWasteCalendarUrl()).isNull();
    }

    /** R4: der Tageslauf braucht die Haushalte mit Quelle. */
    @Test
    void listHouseholdsWithSource_returnsHouseholdsHavingASource() {
        Household withSource = household(2L, "Haus B").withWasteCalendarUrl("https://entsorger.example/ics");
        when(householdRepository.findWithSource()).thenReturn(List.of(withSource));

        assertThat(useCase.listHouseholdsWithSource()).containsExactly(withSource);
    }

    private static Household household(Long id, String name) {
        return Household.builder()
                .id(id)
                .name(name)
                .createdAt(FIXED_NOW)
                .createdBy("other")
                .build();
    }
}
