package ma.ump.deptinfo.backend.dtos;

/**
 * DTO renvoyé par GET /api/stats
 * Contient les chiffres réels lus depuis la base de données MySQL.
 */
public class StatsDTO {

    private long totalEtudiants;
    private long totalEnseignants;
    private long totalFilieres;
    private long totalModules;

    public StatsDTO(long totalEtudiants, long totalEnseignants,
                    long totalFilieres, long totalModules) {
        this.totalEtudiants   = totalEtudiants;
        this.totalEnseignants = totalEnseignants;
        this.totalFilieres    = totalFilieres;
        this.totalModules     = totalModules;
    }

    public long getTotalEtudiants()   { return totalEtudiants; }
    public long getTotalEnseignants() { return totalEnseignants; }
    public long getTotalFilieres()    { return totalFilieres; }
    public long getTotalModules()     { return totalModules; }
}
