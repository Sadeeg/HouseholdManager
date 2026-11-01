# Testmuster — Codebeispiele je Ebene

Die Beispiele aus `SKILL.md` (Abschnitt „Muster"). Nur laden, wenn ein Test
geschrieben wird — die Ebene steht in der Skill, die Formulare hier.

## Use Case (ohne Spring)

```java
@ExtendWith(MockitoExtension.class)
class HouseholdUseCaseImplTest {
    @Mock private HouseholdRepository householdRepository;
    private HouseholdUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HouseholdUseCaseImpl(householdRepository, Clock.fixed(FIXED_NOW, UTC));
    }
}
```

- `Clock` als feste Uhr → Zeit ist **deterministisch** und der Test
  beschreibt sie explizit.
- Der Port wird gemockt, **nicht** die Entity.

## Controller-Slice

```java
@WebMvcTest(HouseholdController.class)
class HouseholdControllerTest {
    @MockitoBean private HouseholdUseCase householdUseCase;   // der Port, nicht die Impl
}
```

## Adapter gegen echte DB

```java
@DataJpaTest
@Import(HouseholdRepositoryAdapter.class)   // @DataJpaTest lädt keine @Repository-Beans
@ImportTestcontainers
class HouseholdRepositoryAdapterTest { @Container @ServiceConnection static PostgreSQLContainer postgres = ...; }
```

## Frontend-Komponente

```ts
TestBed.configureTestingModule({
  imports: [HouseholdsPage],
  providers: [provideHttpClient(), provideHttpClientTesting(), HouseholdsData],
});
```

- Zoneless: **`fixture.detectChanges()` vor `httpMock.expectOne(...)`** —
  sonst wird der Request nie erwartbar.
- Route-scoped Services (z. B. `HouseholdsData`) gehören in `providers`.
