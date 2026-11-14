package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;

/**
 * Groupe d'étudiants d'une filière (ex. "Groupe 1 - GL").
 * UML v02 : CL_2 Structure pédagogique — Filiere "1" -- "0..*" Groupe.
 * Un nom de groupe est unique à l'intérieur d'une même filière.
 *
 * Remarque : le lien avec l'année universitaire (AcademicYear) et l'affectation
 * des étudiants (Enrollment) sont prévus dans la conception mais pas encore
 * codés ; ils seront ajoutés avec l'entité Enrollment.
 */
@Entity
@Table(name = "groupes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_groupe_filiere_name",
                columnNames = {"filiere_id", "name"}))
public class Groupe extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;            // ex. Groupe 1 - GL

    @Column
    private Integer capacity;       // effectif maximal, facultatif

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filiere_id", nullable = false)
    private Filiere filiere;

    public Groupe() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Filiere getFiliere() {
        return filiere;
    }

    public void setFiliere(Filiere filiere) {
        this.filiere = filiere;
    }
}
