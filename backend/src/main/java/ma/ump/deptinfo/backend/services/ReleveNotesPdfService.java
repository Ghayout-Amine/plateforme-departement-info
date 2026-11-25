package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ReleveNotesResponseDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates an official A4 PDF transcript using standard PDF 1.4 vector/text streams
 * with zero external dependencies (WinAnsiEncoding for French accents).
 */
@Service
public class ReleveNotesPdfService {

    private static final Charset WIN_ANSI = Charset.forName("Windows-1252");

    public byte[] generatePdf(ReleveNotesResponseDTO releve) {
        StringBuilder stream = new StringBuilder();

        // Top Navy Banner (y = 755 to 815)
        stream.append("0.08 0.18 0.36 rg 40 755 515 55 re f\n");
        drawText(stream, "/F2", 12, 1.0, 1.0, 1.0, 60, 790, "UNIVERSITÉ MOHAMMED PREMIER - OUJDA");
        drawText(stream, "/F1", 10, 0.85, 0.90, 0.98, 60, 772, "Faculté des Sciences - Département d'Informatique (PGDI 2026)");

        // Document Title
        drawText(stream, "/F2", 18, 0.08, 0.35, 0.85, 165, 718, "RELEVÉ DE NOTES OFFICIEL");
        stream.append("0.08 0.35 0.85 RG 1.5 w 165 712 m 430 712 l S\n");

        // Student Info Box (y = 640 to 695)
        stream.append("0.97 0.98 0.99 rg 40 640 515 55 re f\n");
        stream.append("0.78 0.83 0.90 RG 0.8 w 40 640 515 55 re S\n");

        drawText(stream, "/F2", 10.5, 0.1, 0.15, 0.25, 55, 675, "Étudiant : " + safe(releve.studentName()));
        drawText(stream, "/F1", 10.5, 0.2, 0.25, 0.35, 55, 655, "N° Étudiant : " + safe(releve.studentNumber()));

        String dateEdition = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        drawText(stream, "/F2", 10.5, 0.1, 0.15, 0.25, 370, 675, "Semestre : " + safe(releve.semester()));
        drawText(stream, "/F1", 10.5, 0.2, 0.25, 0.35, 370, 655, "Date d'édition : " + dateEdition);

        // Table Header (y = 595 to 620)
        int y = 595;
        stream.append("0.12 0.23 0.54 rg 40 ").append(y).append(" 515 26 re f\n");
        drawText(stream, "/F2", 10.5, 1, 1, 1, 50, y + 8, "Code");
        drawText(stream, "/F2", 10.5, 1, 1, 1, 125, y + 8, "Intitulé du Module");
        drawText(stream, "/F2", 10.5, 1, 1, 1, 355, y + 8, "Moyenne / 20");
        drawText(stream, "/F2", 10.5, 1, 1, 1, 440, y + 8, "Statut");

        // Table Rows
        List<ReleveNotesResponseDTO.ModuleResultDTO> modules = releve.modules() != null ? releve.modules() : List.of();
        for (int i = 0; i < modules.size(); i++) {
            y -= 26;
            if (y < 140) break; // safety guard for A4 bounds
            ReleveNotesResponseDTO.ModuleResultDTO mod = modules.get(i);
            if (i % 2 == 1) {
                stream.append("0.97 0.98 0.99 rg 40 ").append(y).append(" 515 26 re f\n");
            }
            stream.append("0.85 0.88 0.93 RG 0.6 w 40 ").append(y).append(" 515 26 re S\n");

            drawText(stream, "/F2", 10, 0.06, 0.09, 0.16, 50, y + 8, safe(mod.code()));
            drawText(stream, "/F1", 10, 0.1, 0.15, 0.25, 125, y + 8, truncate(safe(mod.name()), 36));
            String scoreStr = mod.score() != null ? String.format(Locale.FRANCE, "%.2f", mod.score()) : "—";
            drawText(stream, "/F2", 10, 0.06, 0.09, 0.16, 370, y + 8, scoreStr);

            String statusText = formatStatus(mod.status());
            if ("VALIDE".equals(mod.status())) {
                drawText(stream, "/F2", 9.5, 0.08, 0.50, 0.24, 440, y + 8, statusText);
            } else if ("VALIDE_PAR_COMPENSATION".equals(mod.status())) {
                drawText(stream, "/F2", 9.5, 0.12, 0.25, 0.69, 440, y + 8, "Validé (Comp.)");
            } else {
                drawText(stream, "/F2", 9.5, 0.72, 0.11, 0.11, 440, y + 8, statusText);
            }
        }

        // Summary Box
        int sumY = Math.max(120, y - 75);
        stream.append("0.95 0.96 0.98 rg 40 ").append(sumY).append(" 515 55 re f\n");
        stream.append("0.60 0.70 0.85 RG 1.0 w 40 ").append(sumY).append(" 515 55 re S\n");

        drawText(stream, "/F1", 9, 0.4, 0.45, 0.55, 60, sumY + 36, "MOYENNE GÉNÉRALE");
        String avgStr = releve.semesterAverage() != null
                ? String.format(Locale.FRANCE, "%.2f / 20", releve.semesterAverage())
                : "—";
        drawText(stream, "/F2", 13, 0.06, 0.09, 0.16, 60, sumY + 15, avgStr);

        drawText(stream, "/F1", 9, 0.4, 0.45, 0.55, 240, sumY + 36, "DÉCISION DU JURY");
        boolean admis = "ADMIS".equals(releve.decision());
        drawText(stream, "/F2", 13, admis ? 0.08 : 0.72, admis ? 0.50 : 0.11, admis ? 0.24 : 0.11,
                240, sumY + 15, safe(releve.decision()));

        drawText(stream, "/F1", 9, 0.4, 0.45, 0.55, 400, sumY + 36, "MENTION");
        String mention = releve.mention() != null && !releve.mention().isBlank() ? releve.mention() : "—";
        drawText(stream, "/F2", 13, 0.06, 0.09, 0.16, 400, sumY + 15, mention);

        // Footer
        stream.append("0.85 0.88 0.92 RG 0.5 w 40 70 m 555 70 l S\n");
        drawText(stream, "/F1", 8.5, 0.45, 0.5, 0.6, 120, 52,
                "Document officiel généré par la Plateforme de Gestion du Département Informatique (PGDI 2026)");

        return assemblePdf(stream.toString().getBytes(WIN_ANSI));
    }

    private void drawText(StringBuilder sb, String font, double size,
                          double r, double g, double b, double x, double y, String text) {
        sb.append(String.format(Locale.US, "BT %s %.1f Tf %.2f %.2f %.2f rg %.1f %.1f Td (%s) Tj ET\n",
                font, size, r, g, b, x, y, escapePdfText(text)));
    }

    private String escapePdfText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String formatStatus(String status) {
        if ("VALIDE".equals(status)) return "Validé";
        if ("VALIDE_PAR_COMPENSATION".equals(status)) return "Validé par comp.";
        if ("NON_VALIDE".equals(status)) return "Non validé";
        return safe(status);
    }

    private byte[] assemblePdf(byte[] contentBytes) {
        try {
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            pdf.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".getBytes(StandardCharsets.ISO_8859_1));

            // Obj 1: Catalog
            offsets.add(pdf.size());
            pdf.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Obj 2: Pages
            offsets.add(pdf.size());
            pdf.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Obj 3: Page
            offsets.add(pdf.size());
            pdf.write(("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> "
                    + "/Contents 6 0 R >>\nendobj\n").getBytes(StandardCharsets.US_ASCII));

            // Obj 4: Helvetica
            offsets.add(pdf.size());
            pdf.write(("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica "
                    + "/Encoding /WinAnsiEncoding >>\nendobj\n").getBytes(StandardCharsets.US_ASCII));

            // Obj 5: Helvetica-Bold
            offsets.add(pdf.size());
            pdf.write(("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold "
                    + "/Encoding /WinAnsiEncoding >>\nendobj\n").getBytes(StandardCharsets.US_ASCII));

            // Obj 6: Content stream
            offsets.add(pdf.size());
            pdf.write(("6 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            pdf.write(contentBytes);
            pdf.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // xref
            int xrefOffset = pdf.size();
            pdf.write(("xref\n0 " + (offsets.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
            pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
            for (int off : offsets) {
                pdf.write(String.format(Locale.US, "%010d 00000 n \n", off).getBytes(StandardCharsets.US_ASCII));
            }

            // trailer
            pdf.write(("trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                    + xrefOffset + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));

            return pdf.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de construction PDF", e);
        }
    }
}
