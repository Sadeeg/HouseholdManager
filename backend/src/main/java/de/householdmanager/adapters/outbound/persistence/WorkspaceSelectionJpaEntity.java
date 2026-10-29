package de.householdmanager.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell der Workspace-Auswahl: pro Person genau eine Zeile —
 * der zuletzt gewählte Haushalt (ADR 0004).
 */
@Entity
@Table(name = "workspace_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceSelectionJpaEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "household_id", nullable = false)
    private Long householdId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
